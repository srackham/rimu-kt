package org.rimumarkup

object Options {

    // Global option values.
    // These are initialized when the Option object is instaniated.
    var safeMode = 0
        set(value) {
            if (safeMode !in 0..15) {
                throw IllegalArgumentException("Illegal safeMode value")
            }
            field = value
        }
    var htmlReplacement = ""
    var callback: CallbackFunction = DO_NOTHING

    // Reset options to default values.
    fun init() {
        safeMode = 0
        htmlReplacement = "<mark>replaced HTML</mark>"
        callback = DO_NOTHING
    }

    // Return true if safeMode is non-zero.
    fun isSafeModeNz(): Boolean {
        return safeMode != 0
    }

    // Return true if Macro Definitions are ignored.
    fun skipMacroDefs(): Boolean {
        return safeMode != 0 && (safeMode and 0x8) == 0
    }

    // Return true if Block Attribute elements are ignored.
    fun skipBlockAttributes(): Boolean {
        return safeMode != 0 && (safeMode and 0x4) != 0
    }

    fun update(options: RenderOptions) {
        callback = options.callback ?: callback // Install callback first to ensure option errors are logged.
        if (options.reset) Api.init()           // Reset takes priority.
        callback = options.callback ?: callback // Install callback again in case it has been reset.
        // Only update specified (non-null) options.
        if (options.safeMode != null)
            update("safeMode",options.safeMode.toString())
        htmlReplacement = options.htmlReplacement ?: htmlReplacement
        callback = options.callback ?: callback
    }

    // Set named option value.
    fun update(name: String, value: String) {
        when (name) {
            "safeMode" -> {
                val n = value.toIntOrNull()
                if (n == null || n < 0 || n > 15) {
                    errorCallback("illegal safeMode API option value: " + value)
                } else {
                    safeMode = n
                }
            }
            "reset" -> {
                if (value == "true") Api.init()
                else if (value != "false") errorCallback("illegal reset API option value: " + value)
            }
            "htmlReplacement" ->
                htmlReplacement = value
            else ->
                errorCallback("illegal API option name: " + name)
        }
    }

    // Filter HTML based on current safeMode.
    val htmlSafeModeFilter: ContentFilter = fun(html: String, _, _): String {
        when (safeMode and 0x3) {
            0 ->   // Raw HTML (default behavior).
                return html
            1 ->   // Drop HTML.
                return ""
            2 ->   // Replace HTML with 'htmlReplacement' option string.
                return htmlReplacement
            3 ->   // Render HTML as text.
                return Utils.replaceSpecialChars(html)
            else ->
                return ""
        }
    }

    fun errorCallback(message: String) {
        callback(CallbackMessage(type = "error", text = message))
    }

    // Called when an unexpected program error occurs.
    fun panic(message: String) {
        val msg = "panic: " + message
        System.err.println(msg)
        errorCallback(msg)
    }
}
