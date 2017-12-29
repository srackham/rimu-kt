package org.rimumarkup

typealias LineBlockFilter = (match: MatchResult, reader: Io.Reader, def: LineBlocks.Definition) -> String
typealias LineBlockVerify = (match: MatchResult, reader: Io.Reader) -> Boolean   // Additional match verification checks.

object LineBlocks {

    data class Definition(
            val match: Regex,
            val replacement: String = "",
            val name: String = "", // Optional unique identifier.
            val filter: LineBlockFilter? = null,
            val verify: LineBlockVerify? = null // Additional match verification checks.
    )

    val defs = arrayOf(
            // Expand lines prefixed with a macro invocation prior to all other processing.
            // macro name = $1, macro value = $2
            Definition(
                    match = Macros.MATCH_LINE,
                    verify = fun(match: MatchResult, reader: Io.Reader): Boolean {
                        if (Macros.LITERAL_DEF_OPEN.matches(match.groupValues[0]) || Macros.EXPRESSION_DEF_OPEN.matches(match.groupValues[0])) {
                            // Do not process macro definitions.
                            return false
                        }
                        // Silent because any macro expansion errors will be subsequently addressed downstream.
                        val value = Macros.render(match.groupValues[0], true)
                        if (value.startsWith(match.groupValues[1])) {
                            // The leading macro invocation expansion failed or returned itself.
                            // This stops infinite recursion.
                            return false
                        }
                        reader.lines.addAll(reader.pos + 1, value.split("\n"))
                        return true
                    },
                    filter = fun(match: MatchResult, reader: Io.Reader, _): String {
                        return "" // Already processed in the `verify` function.
                    }
            ),
            // Delimited Block definition.
            // name = $1, definition = $2
            Definition(
                    match = Regex("""^\\?\|([\w\-]+)\|\s*=\s*'(.*)'$"""),
                    filter = fun(match: MatchResult, _, _): String {
                        if (Options.isSafeModeNz()) {
                            return ""   // Skip if a safe mode is set.
                        }
                        val value = Utils.replaceInline(match.groupValues[2], ExpansionOptions(macros = true))
                        DelimitedBlocks.setDefinition(match.groupValues[1], value)
                        return ""
                    }
            ),
            // Quote definition.
            // quote = $1, openTag = $2, separator = $3, closeTag = $4
            Definition(
                    match = Regex("""^(\S{1,2})\s*=\s*'([^|]*)(\|{1,2})(.*)'$"""),
                    filter = fun(match: MatchResult, _, _): String {
                        if (Options.isSafeModeNz()) {
                            return ""   // Skip if a safe mode is set.
                        }
                        Quotes.setDefinition(Quotes.Definition(
                                quote = match.groupValues[1],
                                openTag = Utils.replaceInline(match.groupValues[2], ExpansionOptions(macros = true)),
                                closeTag = Utils.replaceInline(match.groupValues[4], ExpansionOptions(macros = true)),
                                spans = match.groupValues[3] == "|"
                        ))
                        return ""
                    }
            ),
            // Replacement definition.
            // pattern = $1, flags = $2, replacement = $3
            Definition(
                    match = Regex("""^\\?/(.+)/([igm]*)\s*=\s*'(.*)'$"""),
                    filter = fun(match: MatchResult, _, _): String {
                        if (Options.isSafeModeNz()) {
                            return ""   // Skip if a safe mode is set.
                        }
                        val pattern = match.groupValues[1]
                        val flags = match.groupValues[2]
                        var replacement = match.groupValues[3]
                        replacement = Utils.replaceInline(replacement, ExpansionOptions(macros = true))
                        Replacements.setDefinition(pattern, flags, replacement)
                        return ""
                    }
            ),
            // Macro definition.
            // name = $1, value = $2
            Definition(
                    match = Macros.LINE_DEF,
                    filter = fun(match: MatchResult, _, _): String {
                        val name = match.groupValues[1]
                        val quote = match.groupValues[2]
                        var value = match.groupValues[3]
                        value = Utils.replaceInline(value, ExpansionOptions(macros = true))
                        Macros.setValue(name, value, quote)
                        return ""
                    }
            ),
            // Headers.
            // $1 is ID, $2 is header text.
            Definition(
                    match = Regex("""^\\?((?:[#=]){1,6})\s+(.+?)(?:\s+\1)?$"""),
                    replacement = "<h$1>$$2</h$1>",
                    filter = fun(match: MatchResult, _, def): String {
                        // Replace $1 with header number e.g. "###" -> "3"
                        val groupValues = match.groupValues.mapIndexed { index, s -> if (index == 1) s.length.toString() else s }.toMutableList()
                        if (!Macros.getValue("--header-ids").isNullOrBlank() && BlockAttributes.id == "") {
                            BlockAttributes.id = BlockAttributes.slugify(groupValues[2])
                        }
                        return Utils.replaceMatch(groupValues, def.replacement, ExpansionOptions(macros = true))
                    }
            ),
            // Comment line.
            Definition(
                    match = Regex("""^\\?/{2}(.*)$""")
            ),
            // Block image: <image:src|alt>
            // src = $1, alt = $2
            Definition(
                    match = Regex("""^\\?<image:([^\s|]+)\|(.+?)>$""", RegexOption.DOT_MATCHES_ALL),
                    replacement = "<img src=\"$1\" alt=\"$2\">"
            ),
            // Block image: <image:src>
            // src = $1, alt = $1
            Definition(
                    match = Regex("""^\\?<image:([^\s|]+?)>$"""),
                    replacement = "<img src=\"$1\" alt=\"$1\">"
            ),
            // DEPRECATED as of 3.4.0.
            // Block anchor: <<#id>>
            // id = $1
            Definition(
                    match = Regex("""^\\?<<#([a-zA-Z][\w\-]*)>>$"""),
                    replacement = "<div id=\"$1\"></div>",
                    filter = fun(match: MatchResult, _, def): String {
                        if (Options.skipBlockAttributes()) {
                            return ""
                        } else {
                            // Default (non-filter) replacement processing.
                            return Utils.replaceMatch(match.groupValues, def.replacement, ExpansionOptions(macros = true))
                        }
                    }
            ),
            // Block Attributes.
            // Syntax: .class-names #id [html-attributes] block-options
            Definition(
                    name = "attributes",
                    match = Regex("""^\\?\.[a-zA-Z#"\[+-].*$"""), // A loose match because Block Attributes can contain macro references.
                    verify = fun(match: MatchResult, _): Boolean {
                        return BlockAttributes.parse(match)
                    }
            ),
            // API Option.
            // name = $1, value = $2
            Definition(
                    match = Regex("""^\\?\.(\w+)\s*=\s*'(.*)'$"""),
                    filter = fun(match: MatchResult, _, _): String {
                        if (!Regex("""^(safeMode|htmlReplacement|reset)$""").matches(match.groupValues[1])) {
                            Options.errorCallback("illegal API option: " + match.groupValues[1] + ": " + match.groupValues[0])
                        } else if (!Options.isSafeModeNz()) {
                            val value = Utils.replaceInline(match.groupValues[2], ExpansionOptions(macros = true))
                            Options.update(match.groupValues[1], value)
                        }
                        return ""
                    }
            )

    )

    // If the next element in the reader is a valid line block render it
    // and return true, else return false.
    fun render(reader: Io.Reader, writer: Io.Writer, allowed: List<String> = listOf()): Boolean {
        if (reader.eof()) Options.panic("premature eof")
        for (def in defs) {
            if (allowed.size > 0 && !allowed.contains(def.name)) continue
            val match = def.match.find(reader.cursor)
            if (match != null) {
                if (match.groupValues[0].startsWith('\\')) {
                    // Drop backslash escape and continue.
                    reader.cursor = reader.cursor.substring(1)
                    continue
                }
                if (def.verify != null && !(def.verify)(match, reader)) {
                    continue
                }
                var text: String
                if (def.filter == null) {
                    text = Utils.replaceMatch(match.groupValues, def.replacement, ExpansionOptions(macros = true))
                } else {
                    text = (def.filter)(match, reader, def)
                }
                if (text.isNotBlank()) {
                    text = BlockAttributes.injectHtmlAttributes(text)
                    writer.write(text)
                    reader.next()
                    if (!reader.eof()) {
                        writer.write("\n")  // Add a trailing '\n' if there are more lines.
                    }
                } else {
                    reader.next()
                }
                return true
            }
        }
        return false
    }

}
