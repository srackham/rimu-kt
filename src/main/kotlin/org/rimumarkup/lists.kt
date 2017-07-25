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
//TODO: The nulls are only necessary to return a empty ItemState => attched block from matchItem()!!! Makes the code a mess of null negation!!
    data class ItemState(
            val groupValues: List<String>,
            val def: Definition,
            val id: String  // List ID.
    ) {
        companion object {
            // Special items.
            val NO_MATCH = ItemState(listOf<String>(), defs[0], "")
            val BLOCK_MATCH = ItemState(listOf<String>(), defs[0], "")
        }
    }

    val defs = arrayOf(
            // Prefix match with backslash to allow escaping.

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
        if (reader.eof()) throw RimucException("Premature EOF")
        val startItem: ItemState = matchItem(reader)
        if (startItem === ItemState.NO_MATCH) {
            return false
        }
        ids.clear()
        renderList(startItem, reader, writer)
        // ids should now be empty.
        assert(ids.isEmpty())
        return true
    }

    fun renderList(startItem: ItemState, reader: Io.Reader, writer: Io.Writer): ItemState {
        ids.add(startItem.id)
        writer.write(Utils.injectHtmlAttributes(startItem.def.listOpenTag))
        var currentItem = startItem
        var nextItem: ItemState
        while (true) {
            nextItem = renderListItem(currentItem, reader, writer)
            if (nextItem === ItemState.NO_MATCH || nextItem.id != currentItem.id) {
                // End of list or next item belongs to ancestor.
                writer.write(currentItem.def.listCloseTag)
                ids.removeAt(ids.size - 1)   // pop.
                return nextItem
            }
            currentItem = nextItem
        }
    }

    fun renderListItem(startItem: ItemState, reader: Io.Reader, writer: Io.Writer): ItemState {
        val def = startItem.def
        val groupValues = startItem.groupValues
        var text: String
        if (groupValues.size == 4) { // 3 match groups => definition list.
            writer.write(def.termOpenTag as String)
            text = Utils.replaceInline(groupValues[1], ExpansionOptions(macros = true, spans = true))
            writer.write(text)
            writer.write(def.termCloseTag as String)
        }
        writer.write(def.itemOpenTag)
        // Process of item text.
        val lines = Io.Writer()
        lines.write(groupValues[groupValues.size - 1] + "\n") // Item text from first line.
        reader.next()
        var nextItem: ItemState
        nextItem = readToNext(reader, lines)
        text = lines.toString().trim()
        text = Utils.replaceInline(text, ExpansionOptions(macros = true, spans = true))
        writer.write(text)
        while (true) {
            if (nextItem === ItemState.NO_MATCH) {
                // EOF or non-list related item.
                writer.write(def.itemCloseTag)
                return ItemState.NO_MATCH
            } else if (nextItem === ItemState.BLOCK_MATCH) {
                // Delimited block.
                val savedIds = ids
                ids = mutableListOf<String>()
                DelimitedBlocks.render(reader, writer)
                ids = savedIds
                reader.skipBlankLines()
                if (reader.eof()) {
                    writer.write(def.itemCloseTag)
                    return ItemState.NO_MATCH
                } else {
                    nextItem = matchItem(reader)
                }
            } else {
                // List item.
                if (ids.indexOf(nextItem.id) != -1) {
                    // Item belongs to current list or an ancestor list.
                    writer.write(def.itemCloseTag)
                    return nextItem
                } else {
                    // Render new child list.
                    nextItem = renderList(nextItem, reader, writer)
                    writer.write(def.itemCloseTag)
                    return nextItem
                }
            }
        }
        // Unreachable code, should never arrive here.
    }

    // Write the list item text from the reader to the writer. Return
    // 'next' containing the next element's match and identity or null if
    // there are no more list releated elements.
    fun readToNext(reader: Io.Reader, writer: Io.Writer): ItemState {
        // The reader should be at the line following the first line of the list
        // item (or EOF).
        var next: ItemState?
        while (true) {
            if (reader.eof()) return ItemState.NO_MATCH
            if (reader.cursor === "") {
                // Encountered blank line.
                reader.next()
                if (reader.cursor === "") {
                    // A second blank line terminates the list.
                    return ItemState.NO_MATCH
                }
                if (reader.eof()) return ItemState.NO_MATCH
                // A single blank line separates list item from ensuing text.
                return matchItem(reader, listOf("indented", "quote-paragraph"))
            }
            next = matchItem(reader, listOf("comment", "code", "division", "html", "quote"))
            if (next != ItemState.NO_MATCH) {
                // Encountered list item or attached Delimited Block.
                return next
            }
            // Current line is list item text so write it to the output and move to the next input line.
            writer.write(reader.cursor)
            writer.write("\n")
            reader.next()
        }
    }

    // Check if the line at the reader cursor matches a list related element.
    // 'attachments' specifies the names of allowed Delimited Block elements (in addition to list items).
    // If it matches a list item return ItemState.
    // If it matches an attached Delimiter Block return ItemState.BLOCK_MATCH.
    // If it does not match a list related element return ItemState.NO_MATCH.
    fun matchItem(reader: Io.Reader, attachments: List<String> = listOf<String>()): ItemState {
        // Check if the line matches a List definition.
        val line = reader.cursor
        // Check if the line matches a list item.
        for (def in defs) {
            val match = def.match.find(line)
            if (match != null) {
                if (match.groupValues[0][0] == '\\') {
                    reader.cursor = reader.cursor.substring(1)   // Drop backslash.
                    return ItemState.NO_MATCH
                }
                return ItemState(groupValues = match.groupValues,
                        def = def,
                        id = match.groupValues[match.groupValues.size - 2] // The second to last match group is the list ID.
                )
            }
        }
        // Check if the line matches an allowed attached Delimited block.
        for (name in attachments) {
            val def = DelimitedBlocks.getDefinition(name)!! // TODO: Should always match?
            if (def.openMatch.matches(line)) {
                return ItemState.BLOCK_MATCH
            }
        }
        return ItemState.NO_MATCH
    }

}
