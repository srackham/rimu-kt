package org.rimumarkup

object Lists {

    data class Definition(
            val match: Regex,
            val listOpenTag: String,
            val listCloseTag: String,
            val itemOpenTag: String,
            val itemCloseTag: String,
            val termOpenTag: String? = null, // Definition lists only.
            val termCloseTag: String? = null  // Definition lists only.
    )

    // Information about a matched list item element.
    data class ItemInfo(
            val id: String, // List ID.
            val groupValues: List<String>,
            val def: Definition
    ) {
        companion object {
            // Static identity items.
            val NO_MATCH = ItemInfo("NO_MATCH", listOf<String>(), defs[0])
        }
    }

    val defs = arrayOf(
            // Prefix match property with backslash to allow escaping.

            // Unordered lists.
            // $1 is list ID $2 is item text.
            Definition(
                    match = Regex("""^\\?\s*(-|\+|\*{1,4})\s+(.*)$"""),
                    listOpenTag = "<ul>",
                    listCloseTag = "</ul>",
                    itemOpenTag = "<li>",
                    itemCloseTag = "</li>"
            ),
            // Ordered lists.
            // $1 is list ID $2 is item text.
            Definition(
                    match = Regex("""^\\?\s*(?:\d*)(\.{1,4})\s+(.*)$"""),
                    listOpenTag = "<ol>",
                    listCloseTag = "</ol>",
                    itemOpenTag = "<li>",
                    itemCloseTag = "</li>"
            ),
            // Definition lists.
            // $1 is term, $2 is list ID, $3 is definition.
            Definition(
                    match = Regex("""^\\?\s*(.*[^:])(:{2,4})(|\s+.*)$"""),
                    listOpenTag = "<dl>",
                    listCloseTag = "</dl>",
                    itemOpenTag = "<dd>",
                    itemCloseTag = "</dd>",
                    termOpenTag = "<dt>",
                    termCloseTag = "</dt>"
            )
    )

    var ids = mutableListOf<String>()       // Stack of open list IDs.

    fun render(reader: Io.Reader, writer: Io.Writer): Boolean {
        if (reader.eof()) Options.panic("premature eof")
        val startItem: ItemInfo = matchItem(reader)
        if (startItem === ItemInfo.NO_MATCH) {
            return false
        }
        ids.clear()
        renderList(startItem, reader, writer)
        // ids should now be empty.
        assert(ids.isEmpty())
        if (ids.size != 0) Options.panic("list stack failure")
        return true
    }

    fun renderList(startItem: ItemInfo, reader: Io.Reader, writer: Io.Writer): ItemInfo {
        ids.add(startItem.id)
        writer.write(BlockAttributes.injectHtmlAttributes(startItem.def.listOpenTag))
        var currentItem = startItem
        var nextItem: ItemInfo
        while (true) {
            nextItem = renderListItem(currentItem, reader, writer)
            if (nextItem === ItemInfo.NO_MATCH || nextItem.id != currentItem.id) {
                // End of list or next item belongs to ancestor.
                writer.write(currentItem.def.listCloseTag)
                ids.removeAt(ids.size - 1)   // pop.
                return nextItem
            }
            currentItem = nextItem
        }
    }

    // Render the current list item, return the next list item or null if there are no more items.
    fun renderListItem(item: ItemInfo, reader: Io.Reader, writer: Io.Writer): ItemInfo {
        val def = item.def
        val groupValues = item.groupValues
        var text: String
        if (groupValues.size == 4) { // 3 match groups => definition list.
            writer.write(BlockAttributes.injectHtmlAttributes(def.termOpenTag as String))
            text = Utils.replaceInline(groupValues[1], ExpansionOptions(macros = true, spans = true))
            writer.write(text)
            writer.write(def.termCloseTag as String)
            writer.write(def.itemOpenTag)
        } else {
            writer.write(BlockAttributes.injectHtmlAttributes(def.itemOpenTag))
        }
        // Process of item text from first line.
        val item_lines = Io.Writer()
        text = groupValues.last()
        item_lines.write(text + "\n")
        // Process remainder of list item i.e. item text, optional attached block, optional child list.
        reader.next()
        val attached_lines = Io.Writer()
        var blank_lines: Int
        var attached_done = false
        var next_item: ItemInfo
        while (true) {
            blank_lines = consumeBlockAttributes(reader, attached_lines)
            if (blank_lines >= 2 || blank_lines == -1) {
                // EOF or two or more blank lines terminates list.
                next_item = ItemInfo.NO_MATCH
                break
            }
            next_item = matchItem(reader)
            if (next_item !== ItemInfo.NO_MATCH) {
                if (ids.contains(next_item.id)) {
                    // Next item belongs to current list or a parent list.
                } else {
                    // Render child list.
                    next_item = renderList(next_item, reader, attached_lines)
                }
                break
            }
            if (attached_done)
                break // Multiple attached blocks are not permitted.
            if (blank_lines == 0) {
                val savedIds = ids
                ids = mutableListOf<String>()
                if (DelimitedBlocks.render(reader, attached_lines, listOf("comment", "code", "division", "html", "quote"))) {
                    attached_done = true
                } else {
                    // Item body line.
                    item_lines.write(reader.cursor + "\n")
                    reader.next()
                }
                ids = savedIds
            } else if (blank_lines == 1) {
                if (DelimitedBlocks.render(reader, attached_lines, listOf("indented", "quote-paragraph"))) {
                    attached_done = true
                } else {
                    break
                }
            }
        }
        // Write item text.
        text = item_lines.toString().trim()
        text = Utils.replaceInline(text, ExpansionOptions(macros = true, spans = true))
        writer.write(text)
        // Write attachment and child list.
        writer.buffer.addAll(attached_lines.buffer)
        // Close list item.
        writer.write(def.itemCloseTag)
        return next_item
    }

    // Consume blank lines and Block Attributes.
    // Return number of blank lines read or -1 if EOF.
    fun consumeBlockAttributes(reader: Io.Reader, writer: Io.Writer): Int {
        var blanks = 0
        while (true) {
            if (reader.eof())
                return -1
            if (LineBlocks.render(reader, writer, listOf("attributes")))
                continue
            if (reader.cursor != "")
                return blanks
            blanks++
            reader.next()
        }
    }

    // Check if the line at the reader cursor matches a list related element.
    // Unescape escaped list items in reader.
    // If it does not match a list related element return ItemInfo.NO_MATCH.
    fun matchItem(reader: Io.Reader): ItemInfo {
        // Check if the line matches a List definition.
        if (reader.eof()) return ItemInfo.NO_MATCH
        // Check if the line matches a list item.
        for (def in defs) {
            val match = def.match.find(reader.cursor)
            if (match != null) {
                if (match.groupValues[0].startsWith('\\')) {
                    reader.cursor = reader.cursor.substring(1)   // Drop backslash.
                    return ItemInfo.NO_MATCH
                }
                return ItemInfo(groupValues = match.groupValues,
                        def = def,
                        id = match.groupValues[match.groupValues.size - 2] // The second to last match group is the list ID.
                )
            }
        }
        return ItemInfo.NO_MATCH
    }

}
