package org.rimumarkup

object Macros {

    // Matches macro invocation. $1 = name, $2 = params.
// DEPRECATED: Matches existential macro invocations.
    val MATCH_MACRO = Regex("""\{([\w\-]+)([!=|?](?:|.*?[^\\]))?\}""", RegexOption.DOT_MATCHES_ALL)
    // Matches all macro invocations. $1 = name, $2 = params.
    val MATCH_MACROS = Regex("\\\\?" + MATCH_MACRO.pattern)
    // Matches a line starting with a macro invocation.
    val MACRO_LINE = Regex("^" + MATCH_MACRO.pattern + ".*$")
    // Match multi-line macro definition open delimiter. $1 is first line of macro.
    val MACRO_DEF_OPEN = Regex("""^\\?\{[\w\-]+\??\}\s*=\s*'(.*)$""")
    // Match multi-line macro definition open delimiter. $1 is last line of macro.
    val MACRO_DEF_CLOSE = Regex("""^(.*)'$""")
    // Match single-line macro definition. $1 = name, $2 = value.
    val MACRO_DEF = Regex("""^\\?\{([\w\-]+\??)\}\s*=\s*'(.*)'$= """)

    data class Macro(
            val name: String = "",
            val value: String = ""
    )

    val defs = mutableListOf<Macro>()

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
    }

    fun setValue(name: String, value: String) {
        //TODO Stub
    }

    fun render(text: String, inline: Boolean = true): String {
        //TODO Stub
        return text
    }
}
