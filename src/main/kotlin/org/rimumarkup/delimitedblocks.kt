package org.rimumarkup

typealias DelimiterFilter = (match: MatchResult, def: DelimitedBlocks.Definition) -> String
typealias ContentFilter = (text: String, match: MatchResult, expansionOptions: ExpansionOptions) -> String

object DelimitedBlocks {

    val MATCH_INLINE_TAG = Regex("""(?i)^(a|abbr|acronym|address|b|bdi|bdo|big|blockquote|br|cite|code|del|dfn|em|i|img|ins|kbd|mark|q|s|samp|small|span|strike|strong|sub|sup|time|tt|u|var|wbr)$""")

    // delimiterFilter for code, division and quote blocks.
// Inject $2 into block class attribute, set close delimiter to $1.
    val classInjectionFilter: DelimiterFilter = fun(match: MatchResult, def: Definition): String {
        val name = match.groupValues[2].trim()
        if (name.isNotBlank()) {
            BlockAttributes.classes = name
        }
        def.closeMatch = Regex("^" + Regex.escape(match.groupValues[1]) + "$")
        return ""
    }

    // delimiterFilter that returns opening delimiter line text from match group $1.
    val delimiterTextFilter: DelimiterFilter = fun(match: MatchResult, _): String {
        return match.groupValues[1]
    }

    // Multi-line block element definition.
    data class Definition(
            val name: String = "", // Optional unique identifier.
            val openMatch: Regex, // $1 (if defined) is appended to block content.
            var closeMatch: Regex?,
            var openTag: String,
            var closeTag: String,
            val verify: ((match: MatchResult) -> Boolean)? = null, // Additional match verification checks.
            val delimiterFilter: DelimiterFilter? = null, // Process opening delimiter. Return any delimiter content.
            val contentFilter: ContentFilter? = null,
            val expansionOptions: ExpansionOptions
    )

    val defs = mutableListOf<Definition>()  // Mutable definitions initialized by DEFAULT_DEFS.

    val DEFAULT_DEFS = arrayOf(
            // Delimited blocks cannot be escaped with a backslash.

// Macro definition block.
            Definition(
                    openMatch = Macros.MACRO_DEF_OPEN, // $1 is first line of macro.
                    closeMatch = Macros.MACRO_DEF_CLOSE, // $1 is last line of macro.
                    openTag = "",
                    closeTag = "",
                    expansionOptions = ExpansionOptions(
                            macros = true
                    ),
                    delimiterFilter = delimiterTextFilter,
                    contentFilter = fun(text, match, expansionOptions): String {
                        // Process macro definition.
                        if (Options.skipMacroDefs()) {
                            return ""   // Skip if a safe mode is set.
                        }
                        val name = Regex("""^\{([\w\-]+\??)\}""").find(match.value)!!.groupValues[1]  // Extract macro name from opening delimiter.
                        var value = text
                        value = value.replace(Regex("""' *\\\n"""), "'\n")            // Unescape line-continuations.
                        value = value.replace(Regex("""(' *[\\]+)\\\n"""), "$1\n")     // Unescape escaped line-continuations.
                        value = Utils.replaceInline(value, expansionOptions) // Expand macro invocations.
                        Macros.setValue(name, value)
                        return ""
                    }
            ),
            // Comment block.
            Definition(
                    name = "comment",
                    openMatch = Regex("""^\\?\/\*+$"""),
                    closeMatch = Regex("""^\*+\/$"""),
                    openTag = "",
                    closeTag = "",
                    expansionOptions = ExpansionOptions(
                            skip = true,
                            specials = true // Fall-back if skip is disabled.
                    )
            ),
            // Division block.
            Definition(
                    name = "division",
                    openMatch = Regex("""^\\?(\.{2,})([\w\s-]*)$"""), // $1 is delimiter text, $2 is optional class names.
                    closeMatch = null,
                    openTag = "<div>",
                    closeTag = "</div>",
                    expansionOptions = ExpansionOptions(
                            container = true,
                            specials = true // Fall-back if container is disabled.
                    ),
                    delimiterFilter = classInjectionFilter
            ),
            // Quote block.
            Definition(
                    name = "quote",
                    openMatch = Regex("""^\\?("{2,})([\w\s-]*)$"""), // $1 is delimiter text, $2 is optional class names.
                    closeMatch = null,
                    openTag = "<blockquote>",
                    closeTag = "</blockquote>",
                    expansionOptions = ExpansionOptions(
                            container = true,
                            specials = true // Fall-back if container is disabled.
                    ),
                    delimiterFilter = classInjectionFilter
            ),
            // Code block.
            Definition(
                    name = "code",
                    openMatch = Regex("""^\\?(\-{2,}|`{2,})([\w\s-]*)$"""), // $1 is delimiter text, $2 is optional class names.
                    closeMatch = null,
                    openTag = "<pre><code>",
                    closeTag = "</code></pre>",
                    expansionOptions = ExpansionOptions(
                            macros = false,
                            specials = true
                    ),
                    verify = fun(match): Boolean {
                        // The deprecated '-' delimiter does not support appended class names.
                        return !(match.groupValues[1][0] == '-' && match.groupValues[2].trim() !== "")
                    },
                    delimiterFilter = classInjectionFilter
            ),
            // HTML block.
            Definition(
                    name = "html",
                    // Block starts with HTML comment, DOCTYPE directive or block-level HTML start or end tag.
                    // $1 is first line of block.
                    // $2 is the alphanumeric tag name.
                    openMatch = Regex("""(?i)^(<!--.*|<!DOCTYPE(?:\s.*)?|<\/?([a-z][a-z0-9]*)(?:[\s>].*)?)$"""),
                    closeMatch = Regex("""^$"""), // Blank line or EOF.
                    openTag = "",
                    closeTag = "",
                    expansionOptions = ExpansionOptions(
                            macros = true
                    ),
                    verify = fun(match: MatchResult): Boolean {
                        // Return false if the HTML tag is an inline (non-block) HTML tag.
                        if (match.groupValues[2].isNotBlank()) { // Matched alphanumeric tag name.
                            return !MATCH_INLINE_TAG.matches(match.groupValues[2])
                        } else {
                            return true   // Matched HTML comment or doctype tag.
                        }
                    },
                    delimiterFilter = delimiterTextFilter,
                    contentFilter = Options.htmlSafeModeFilter
            ),
            // Indented paragraph.
            Definition(
                    name = "indented",
                    openMatch = Regex("""^\\?(\s+.*)$"""), // $1 is first line of block.
                    closeMatch = Regex("""^$"""), // Blank line or EOF.
                    openTag = "<pre><code>",
                    closeTag = "</code></pre>",
                    expansionOptions = ExpansionOptions(
                            macros = false,
                            specials = true
                    ),
                    delimiterFilter = delimiterTextFilter,
                    contentFilter = fun(text: String, _, _): String {
                        // Strip indent from start of each line.
                        val first_indent = Regex("""\S""").find(text)!!.range.first
                        var lines = text.split("\n")
                        lines = lines.map { line ->
                            // Strip first line indent width or up to first non-space character.
                            var indent = Regex("""\S""").find(line)!!.range.first
                            if (indent > first_indent) indent = first_indent
                            line.substring(indent)
                        }
                        return lines.joinToString("\n")
                    }
            ),
            // Quote paragraph.
            Definition(
                    name = "quote-paragraph",
                    openMatch = Regex("""^\\?(>.*)$"""), // $1 is first line of block.
                    closeMatch = Regex("""^$"""), // Blank line or EOF.
                    openTag = "<blockquote><p>",
                    closeTag = "</p></blockquote>",
                    expansionOptions = ExpansionOptions(
                            macros = true,
                            spans = true,
                            specials = true       // Fall-back if spans is disabled.
                    ),
                    delimiterFilter = delimiterTextFilter,
                    contentFilter = fun(text: String, _, _): String {
                        // Strip leading > from start of each line and unescape escaped leading >.
                        var lines = text.split("\n")
                        lines = lines.map { line ->
                            line.replace(Regex("""^>"""), "")
                                    .replace(Regex("""^\\>"""), ">")
                        }
                        return lines.joinToString("\n")
                    }
            ),
            // Paragraph (lowest priority, cannot be escaped).
            Definition(
                    name = "paragraph",
                    openMatch = Regex("""(.*)"""), // $1 is first line of block.
                    closeMatch = Regex("""^$"""), // Blank line or EOF.
                    openTag = "<p>",
                    closeTag = "</p>",
                    expansionOptions = ExpansionOptions(
                            macros = true,
                            spans = true,
                            specials = true       // Fall-back if spans is disabled.
                    ),
                    delimiterFilter = delimiterTextFilter
            )
    )

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
        defs.addAll(DEFAULT_DEFS)
    }

    // If the next element in the reader is a valid delimited block render it
// and return true, else return false.
    fun render(reader: Io.Reader, writer: Io.Writer): Boolean {
        if (reader.eof()) throw RimucException("Unexpected EOF")
        for (def in defs) {
            val match = def.openMatch.find(reader.cursor)
            //TODO if == null continue, it clearer and less indented.
            if (match != null) {
                // Escape non-paragraphs.
                if (match.value == "\\" && def.name != "paragraph") {
                    // Drop backslash escape and continue.
                    reader.cursor = reader.cursor.substring(1)
                    continue
                }
                if (def.verify != null) {
                    if (!(def.verify)(match)) {
                        continue
                    }
                }
                // Process opening delimiter.
                //TODO: Is this a Kotlin bug? I should not have to !! but if not get error "reference has nullable type ..."
                val delimiterText = if (def.delimiterFilter != null) (def.delimiterFilter)(match, def) else ""
                // Read block content into lines.
                val lines = mutableListOf<String>()
                if (delimiterText.isNotBlank()) {
                    lines.add(delimiterText)
                }
                // Read content up to the closing delimiter.
                reader.next()
                val content = reader.readTo(def.closeMatch ?: def.openMatch)
                if (content == null) {
                    Options.errorCallback("unterminated delimited block: " + match.value)
                } else lines.addAll(content)
                // Calculate block expansion options.
                val expansionOptions = ExpansionOptions()

                expansionOptions.merge(def.expansionOptions)
                expansionOptions.merge(BlockAttributes.options)
                // Translate block.
                if (!(expansionOptions.skip ?: false)) {
                    var text = lines.joinToString("\n")
                    if (def.contentFilter != null) {
                        text = (def.contentFilter)(text, match, expansionOptions)   // TODO: Why do we need !! ?
                    }
                    var opentag = def.openTag
                    if (def.name === "html") {
                        text = Utils.injectHtmlAttributes(text)
                    } else {
                        opentag = Utils.injectHtmlAttributes(opentag)
                    }
                    if (expansionOptions.container ?: false) {
                        BlockAttributes.options.container = null  // Consume before recursion.
                        text = Api.render(text)
                    } else {
                        text = Utils.replaceInline(text, expansionOptions)
                    }
                    var closetag = def.closeTag
                    if (def.name === "division" && opentag === "<div>") {
                        // Drop div tags if the opening div has no attributes.
                        opentag = ""
                        closetag = ""
                    }
                    writer.write(opentag)
                    writer.write(text)
                    writer.write(closetag)
                    if (!reader.eof() && (opentag + text + closetag).isNotBlank()) {
                        // Add a trailing '\n' if we've written a non-blank line and there are more source lines left.
                        writer.write("\n")
                    }
                }
                // Reset consumed Block Attributes expansion options.
                BlockAttributes.options = ExpansionOptions()
                return true
            }
        }
        return false  // No matching delimited block found.
    }

    // Return block definition or undefined if not found.
    fun getDefinition(name: String): Definition? {
        return defs.first { def -> def.name == name }
    }

    // Update existing named definition.
// Value syntax: <open-tag>|<close-tag> block-options
    fun setDefinition(name: String, value: String) {
        val def = getDefinition(name)
        if (def == null) {
            Options.errorCallback("illegal delimited block name: $name: |$name|='$value'")
            return
        }
        val match = Regex("""^(?:(<[a-zA-Z].*>)\|(<[a-zA-Z/].*>))?(?:\s*)?([+-][ \w+-]+)?$""").find(value.trim())
        if (match != null) {
            if (match.groupValues.size > 1) def.openTag = match.groupValues[1]
            if (match.groupValues.size > 2) def.closeTag = match.groupValues[2]
            if (match.groupValues.size > 3) def.expansionOptions.parse(match.groupValues[3])
        }
    }

}
