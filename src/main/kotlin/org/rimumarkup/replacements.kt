package org.rimumarkup

object Replacements {

    data class Definition(
            var match: Regex,
            var replacement: String,
            val filter: ((match: MatchResult, def: Definition) -> String)? = null
    )

    val defs = mutableListOf<Definition>()  // Initialized by DEFAULT_DEFS.

    val DEFAULT_DEFS = arrayOf<Definition>(
            // Begin match with \\? to allow the replacement to be escaped.
            // Global flag must be set on match re's so that the RegExp lastIndex property is set.
            // Replacements and special characters are expanded in replacement groups ($1..).
            // Replacement order is important.

            // DEPRECATED as of 3.4.0.
            // Anchor: <<#id>>
            Definition(
                    match = Regex("""\\?<<#([a-zA-Z][\w\-]*)>>"""),
                    replacement = """<span id="$1"></span>""",
                    filter = fun(match: MatchResult, def: Definition): String {
                        if (Options.skipBlockAttributes()) {
                            return ""
                        }
                        // Default (non-filter) replacement processing.
                        return Utils.replaceMatch(match, def.replacement, Utils.ExpansionOptions())
                    }
            ),

            // Image: <image:src|alt>
            // src = $1, alt = $2
            Definition(
                    //                    match = Regex("""\\?<image:([^\s\|]+)\|([^]*?)>"""),
                    match = Regex("""\\?<image:([^\s\|]+)\|(.*?)>""", RegexOption.DOT_MATCHES_ALL),
                    replacement = """<img src="$1" alt="$2">"""
            ),

            // Image: <image:src>
            // src = $1, alt = $1
            Definition(
                    match = Regex("""\\?<image:([^\s\|]+?)>"""),
                    replacement = """<img src="$1" alt="$1">"""
            ),

            // Image: ![alt](url)
            // alt = $1, url = $2
            Definition(
                    //                    match = Regex("""\\?!\[([^[]*?)\]\((\S+?)\)"""),
                    match = Regex("""\\?!\[([^\[]*?)\]\((\S+?)\)"""),
                    replacement = """<img src="$2" alt="$1">"""
            ),

            // Email: <address|caption>
            // address = $1, caption = $2
            Definition(
                    //                    match = Regex("""\\?<(\S+@[\w\.\-]+)\|([^]+?)>"""),
                    match = Regex("""\\?<(\S+@[\w\.\-]+)\|(.+?)>""", RegexOption.DOT_MATCHES_ALL),
                    replacement = """<a href="mailto:$1">$$2</a>"""
            ),

            // Email: <address>
            // address = $1, caption = $1
            Definition(
                    match = Regex("""\\?<(\S+@[\w\.\-]+)>"""),
                    replacement = """<a href="mailto:$1">$1</a>"""
            ),

            // Link: [caption](url)
            // caption = $1, url = $2
            Definition(
                    //                    match = Regex("""\\?\[([^[]*?)\]\((\S+?)\)"""),
                    match = Regex("""\\?\[([^\[]*?)\]\((\S+?)\)"""),
                    replacement = """<a href="$2">$$1</a>"""
            ),

            // Link: <url|caption>
            // url = $1, caption = $2
            Definition(
                    //                    match = Regex("""\\?<(\S+?)\|([^]*?)>"""),
                    match = Regex("""\\?<(\S+?)\|(.*?)>""", RegexOption.DOT_MATCHES_ALL),
                    replacement = """<a href="$1">$$2</a>"""
            ),

            // HTML inline tags.
            // Match HTML comment or HTML tag.
            // $1 = tag, $2 = tag name
            Definition(
                    match = Regex("""\\?(<!--(?:[^<>&]*)?-->|<\/?([a-z][a-z0-9]*)(?:\s+[^<>&]+)?>)/""", RegexOption.IGNORE_CASE),
                    replacement = "",
                    filter = fun(match: MatchResult, _: Definition): String {
                        return Options.htmlSafeModeFilter(match.groupValues[1]) // Matched HTML comment or inline tag.
                    }
            ),

            // Link: <url>
            // url = $1
            Definition(
                    match = Regex("""\\?<([^|\s]+?)>"""),
                    replacement = """<a href="$1">$1</a>"""
            ),

            // Auto-encode (most) raw HTTP URLs as links.
            Definition(
                    match = Regex("""\\?((?:http|https):\/\/[^\s"']*[A-Za-z0-9/#])"""),
                    replacement = """<a href="$1">$1</a>"""
            ),

            // Character entity.
            Definition(
                    match = Regex("""\\?(&[\w#][\w]+;)"""),
                    replacement = "",
                    filter = fun(match: MatchResult, _: Definition): String {
                        return match.groupValues[1]   // Pass the entity through verbatim.
                    }
            ),

            // Line-break (space followed by \ at end of line).
            Definition(
                    match = Regex("""[\\ ]\\(\n|$)"""),
                    replacement = """<br>$1"""
            ),

            // This hack ensures backslashes immediately preceding closing code quotes are rendered
            // verbatim (Markdown behaviour).
            // Works by finding escaped closing code quotes and replacing the backslash and the character
            // preceding the closing quote with itself.
            Definition(
                    match = Regex("""(\S\\)(?=`)"""),
                    replacement = """$1"""
            ),

            // This hack ensures underscores within words rendered verbatim and are not treated as
            // underscore emphasis quotes (GFM behaviour).
            Definition(
                    match = Regex("""([a-zA-Z0-9]_)(?=[a-zA-Z0-9])"""),
                    replacement = """$1"""
            )
    )

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
        defs.addAll(DEFAULT_DEFS)
    }

    // Update existing or add new replacement definition.
    // TODO: rename to updateDefinition()
    fun setDefinition(pattern: String, flags: String, replacement: String) {
        val reopts = mutableSetOf<RegexOption>()
        if (flags.contains("i"))
            reopts.add(RegexOption.IGNORE_CASE)
        if (!flags.contains("m")) {
            reopts.add(RegexOption.MULTILINE)
        }
        for (def in defs) {
            if (def.match.pattern == pattern) {
                // Update existing definition.
                // Flag properties are read-only so have to create new RegExp.
                def.match = Regex(pattern, reopts.toSet())
                def.replacement = replacement
                return
            }
        }
        // Append new definition to end of defs list (custom definitons have lower precedence).
        defs.add(Definition(match = Regex(pattern, reopts.toSet()), replacement = replacement))
    }

}
