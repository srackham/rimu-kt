package org.rimumarkup

object Macros {

    // Matches macro invocation. $1 = name, $2 = params.
    // DEPRECATED: Matches existential macro invocations.
    val MATCH_MACRO = Regex("""\{([\w\-]+)([!=|?](?:|.*?[^\\]))?\}""", RegexOption.DOT_MATCHES_ALL)
    // Matches all macro invocations. $1 = name, $2 = params.
    val MATCH_MACROS = Regex("\\\\?" + MATCH_MACRO.pattern,RegexOption.DOT_MATCHES_ALL)
    // Matches a line starting with a macro invocation.
    val MACRO_LINE = Regex("^" + MATCH_MACRO.pattern + ".*$")
    // Match multi-line macro definition open delimiter. $1 is first line of macro.
    val MACRO_DEF_OPEN = Regex("""^\\?\{[\w\-]+\??\}\s*=\s*'(.*)$""")
    // Match multi-line macro definition open delimiter. $1 is last line of macro.
    val MACRO_DEF_CLOSE = Regex("""^(.*)'$""")
    // Match single-line macro definition. $1 = name, $2 = value.
    val MACRO_DEF = Regex("""^\\?\{([\w\-]+\??)\}\s*=\s*'(.*)'$""")

    data class Macro(
            val name: String = "",
            var value: String = ""
    )

    val defs = mutableListOf<Macro>()

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
    }

    // Return named macro value or null if it doesn't exist.
    fun getValue(name: String): String? {
        for (def in defs) {
            if (def.name == name) {
                return def.value
            }
        }
        return null
    }

    // Set named macro value or add it if it doesn't exist.
    // If the name ends with '?' then don't set the macro if it already exists.
    fun setValue(name: String, value: String) {
        for (def in defs) {
            if (def.name == name) {
                if (!name.endsWith('?')) {
                    def.value = value
                }
                return
            }
        }
        defs.add(Macro(
                name = if (name.endsWith('?')) name.dropLast(1) else name,
                value = value)
        )
    }

    // Render all macro invocations in text string.
    fun render(text: String, inline: Boolean = true): String {
        var result = MATCH_MACROS.replace(text, fun(match: MatchResult): String {
            if (match.value.startsWith('\\')) {
                return match.value.substring(1)
            }
            val name = match.groupValues[1]
            var value = getValue(name)  // Macro value is null if macro is undefined.
            if (value == null) {
                if (inline) {
                    Options.errorCallback("undefined macro: ' + match + ': $text")
                }
                return match.value
            }
            var params = match.groupValues[2]
//TODO makes more sense than when expression below
//            if (params.isBlank())
//                return value
            if (params.startsWith('?')) { // DEPRECATED: Existential macro invocation.
                if (inline) Options.errorCallback("existential macro invocations are deprecated: " + match)
                return match.value
            }
            params = Regex("""\\\}""").replace(params, "}")   // Unescape escaped } characters.
//            when (params[0]) {
                when (if (params.isBlank()) ' ' else params[0]) {
                '|' -> {   // Parametrized macro.
                    val paramsList = params.substring(1).split('|')
                    // Substitute macro parameters.
                    // Matches macro definition formal parameters [$]$<param-number>[[\]:<default-param-value>$]
                    // 1st group: [$]$
                    // 2nd group: <param-number> (1, 2..)
                    // 3rd group: :[\]<default-param-value>$
                    // 4th group: <default-param-value>
                    val PARAM_RE = Regex("""\\?(\$\$?)(\d+)(\\?:(|.*?[^\\])\$)?""", RegexOption.DOT_MATCHES_ALL)
                    value = PARAM_RE.replace(value, fun(mr: MatchResult): String {
                        if (mr.value[0] == '\\') {  // Unescape escaped macro parameters.
                            return mr.value.substring(1)
                        }
                        val p1 = mr.groupValues[1]
                        val p2 = mr.groupValues[2].toInt()
                        val p3 = mr.groupValues[3]
                        val p4 = mr.groupValues[4]
                        // Unassigned parameters are replaced with a blank string.
                        var param = if (paramsList.size < p2) "" else paramsList[p2 - 1]
                        if (p3.isNotBlank()) {
                            if (p3[0] == '\\') { // Unescape escaped default parameter.
                                param += p3.substring(1)
                            } else {
                                if (param == "") {
                                    param = p4                              // Assign default parameter value.
                                    param = Regex("""\\\$""").replace(param, "$")     // Unescape escaped $ characters in the default value.
                                }
                            }
                        }
                        if (p1 == "$$") {
                            param = Spans.render(param)
                        }
                        return param
                    })
                    return value
                }
                '!', '=' -> { // Inclusion macro.
                    val pattern = params.substring(1)
                    var skip = !Regex("^$pattern$").matches(value)
                    if (params[0] == '!') {
                        skip = !skip
                    }
                    return if (skip) "\u0000" else ""   // '\0' flags line for deletion.
                }
                else ->  // Plain macro.
                    return value

            }
        })
        // Delete lines marked for deletion by inclusion macros.
        if (result.indexOf('\u0000') >= 0) {
            result = result.split('\n').filter { it.contains('\u0000') }.joinToString("\n")
        }
        return result
    }

}
