package org.rimumarkup

object Quotes {

    data class Definition(
            val quote: String, // Single quote character.
            var openTag: String,
            var closeTag: String,
            var spans: Boolean       // Allow span elements inside quotes.
    )

    val defs = mutableListOf<Definition>()  // Mutable definitions initialized by DEFAULT_DEFS.

    val DEFAULT_DEFS = arrayOf(
            Definition(
                    quote = "**",
                    openTag = "<strong>",
                    closeTag = "</strong>",
                    spans = true
            ),
            Definition(
                    quote = "*",
                    openTag = "<em>",
                    closeTag = "</em>",
                    spans = true
            ),
            Definition(
                    quote = "__",
                    openTag = "<strong>",
                    closeTag = "</strong>",
                    spans = true
            ),
            Definition(
                    quote = "_",
                    openTag = "<em>",
                    closeTag = "</em>",
                    spans = true
            ),
            Definition(
                    quote = "``",
                    openTag = "<code>",
                    closeTag = "</code>",
                    spans = false
            ),
            Definition(
                    quote = "`",
                    openTag = "<code>",
                    closeTag = "</code>",
                    spans = false
            ),
            Definition(
                    quote = "~~",
                    openTag = "<del>",
                    closeTag = "</del>",
                    spans = true
            ))

    var quotesRe = Regex("") // Searches for quoted text.
    var unescapeRe = Regex("")      // Searches for escaped quotes.

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
        DEFAULT_DEFS.mapTo(defs) { it.copy() }
        initializeRegExps()
    }

    // Synthesise re's to find and unescape quotes.
    fun initializeRegExps() {
        val quotes = defs.joinToString("|") { def -> Regex.escape(def.quote) }
        // $1 is quote character(s), $2 is quoted text.
        // Quoted text cannot begin or end with whitespace.
        // Quoted can span multiple lines.
        // Quoted text cannot end with a backslash.
        quotesRe = Regex("\\\\?($quotes)([^\\s\\\\]|\\S[\\s\\S]*?[^\\s\\\\])\\1")
        // $1 is quote character(s).
        unescapeRe = Regex("\\\\($quotes)")
    }

    // Return the quote definition corresponding to 'quote' character, return null if not found.
    fun getDefinition(quote: String): Definition? {
        for (def in defs) {
            if (def.quote == quote) return def
        }
        return null
    }

    // Strip backslashes from quote characters.
    fun unescape(s: String): String {
        return s.replace(unescapeRe, "$1")
    }

    // Update existing or add new quote definition.
    fun setDefinition(def: Definition) {
        for (d in defs) {
            if (d.quote == def.quote) {
                // Update existing definition.
                d.openTag = def.openTag
                d.closeTag = def.closeTag
                d.spans = def.spans
                return
            }
        }
        // Double-quote definitions are prepended to the array so they are matched
        // before single-quote definitions (which are appended to the array).
        if (def.quote.length == 2) {
            defs.add(0, def)
        } else {
            defs.add(def)
        }
        initializeRegExps()
    }

}
