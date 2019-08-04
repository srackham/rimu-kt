package org.rimumarkup

object Macros {

    // Matches a line starting with a macro invocation. $1 = macro invocation.
    val MATCH_LINE = Regex("""^(\{(?:[\w\-]+)(?:[!=|?](?:|.*?[^\\]))?}).*$""")
    // Match single-line macro definition. $1 = name, $2 = delimiter, $3 = value.
    val LINE_DEF = Regex("""^\\?\{([\w\-]+\??)}\s*=\s*(['`])(.*)\2$""")
    // Match multi-line macro definition literal value open delimiter. $1 is first line of macro.
    val LITERAL_DEF_OPEN = Regex("""^\\?\{[\w\-]+\??}\s*=\s*'(.*)$""")
    val LITERAL_DEF_CLOSE = Regex("""^(.*)'$""")
    // Match multi-line macro definition expression value open delimiter. $1 is first line of macro.
    val EXPRESSION_DEF_OPEN = Regex("""^\\?\{[\w\-]+\??}\s*=\s*`(.*)$""")
    val EXPRESSION_DEF_CLOSE = Regex("""^(.*)`$""")


    data class Macro(
            val name: String = "",
            var value: String = ""
    )

    val defs = mutableListOf<Macro>()

    // Reset definitions to defaults.
    fun init() {
        defs.clear()
        // Initialize predefined macros.
        defs.add(Macro(name = "--", value = ""))
        defs.add(Macro(name = "--header-ids", value = ""))
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
    // `quote` is a single character: ' if a literal value, ` if an expression value.
    fun setValue(name: String, value: String, quote: String) {
        if (Options.skipMacroDefs()) {
            return  // Skip if a safe mode is set.
        }
        if (name == "--" && value != "") {
            Options.errorCallback("the predefined blank '--' macro cannot be redefined")
            return
        }
        if (quote == "`") {
            Options.errorCallback("""unsupported: expression macro values: `${value}`""")
        }
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
    // Render Simple invocations first, followed by Parametized, Inclusion and Exclusion invocations.
    fun render(text: String, silent: Boolean = false): String {
        val MATCH_COMPLEX = Regex("""\\?\{([\w\-]+)([!=|?](?:|.*?[^\\]))}""", RegexOption.DOT_MATCHES_ALL) // Parametrized, Inclusion and Exclusion invocations.
        val MATCH_SIMPLE = Regex("""\\?\{([\w\-]+)()}""")                       // Simple macro invocation.
        var result = text
        listOf(MATCH_SIMPLE, MATCH_COMPLEX).forEach { find ->
            result = find.replace(result, fun(match: MatchResult): String {
                if (match.value.startsWith('\\')) {
                    return match.value.substring(1)
                }
                var params = match.groupValues[2]
                if (params.startsWith('?')) { // DEPRECATED: Existential macro invocation.
                    if (!silent) {
                        Options.errorCallback("existential macro invocations are deprecated: ${match.value}")
                    }
                    return match.value
                }
                val name = match.groupValues[1]
                var value = getValue(name)  // Macro value is null if macro is undefined.
                if (value == null) {
                    if (!silent) {
                        Options.errorCallback("undefined macro: ${match.value}: $text")
                    }
                    return match.value
                }
                if (find === MATCH_SIMPLE) {
                    return value
                }
                params = Regex("""\\}""").replace(params, "}")   // Unescape escaped } characters.
                when (params.firstOrNull()) {
                    '|' -> {   // Parametrized macro.
                        val paramsList = params.substring(1).split('|')
                        // Substitute macro parameters.
                        // Matches macro definition formal parameters [$]$<param-number>[[\]:<default-param-value>$]
                        // 1st group: [$]$
                        // 2nd group: <param-number> (1, 2..)
                        // 3rd group: [\]:<default-param-value>$
                        // 4th group: <default-param-value>
                        val PARAM_RE = Regex("""\\?(\$\$?)(\d+)(\\?:(|.*?[^\\])\$)?""", RegexOption.DOT_MATCHES_ALL)
                        value = PARAM_RE.replace(value, fun(mr: MatchResult): String {
                            if (mr.value.startsWith('\\')) {  // Unescape escaped macro parameters.
                                return mr.value.substring(1)
                            }
                            val p1 = mr.groupValues[1]
                            val p2 = mr.groupValues[2].toInt()
                            val p3 = mr.groupValues[3]
                            val p4 = mr.groupValues[4]
                            if (p2 == 0) {
                                return mr.value // $0 is not a valid parameter name.
                            }
                            // Unassigned parameters are replaced with a blank string.
                            var param = if (paramsList.size < p2) "" else paramsList[p2 - 1]
                            if (p3.isNotBlank()) {
                                if (p3.startsWith('\\')) { // Unescape escaped default parameter.
                                    param += p3.substring(1)
                                } else {
                                    if (param == "") {
                                        param = p4 // Assign default parameter value.
                                        param = Regex("""\\\$""").replace(param, "\\$") // Unescape escaped $ characters in the default value.
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
                    '!', '=' -> { // Exclusion and Inclusion macro.
                        val pattern = params.substring(1)
                        var skip: Boolean
                        try {
                            skip = !Regex("^$pattern$").matches(value)
                        } catch (e: java.util.regex.PatternSyntaxException) {
                            if (!silent) {
                                Options.errorCallback("illegal macro regular expression: " + pattern + ": " + text)
                            }
                            return match.value
                        }
                        if (params[0] == '!') {
                            skip = !skip
                        }
                        return if (skip) "\u0002" else ""   // Line deletion flag.
                    }
                    else -> {
                        Options.errorCallback("illegal macro syntax: " + match.value)
                        return ""
                    }
                }
            })
        }
        // Delete lines flagged by Inclusion/Exclusion macros.
        if (result.indexOf('\u0002') >= 0) {
            result = result.split('\n')
                    .filter { !it.contains('\u0002') }
                    .joinToString("\n")
        }
        return result
    }

}
