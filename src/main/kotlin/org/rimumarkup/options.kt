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
    var callback: CallbackFunction = doNothing

    data class RenderOptions(
            var safeMode: Int? = null,
            var htmlReplacement: String? = null,
            var reset: Boolean = false,
            var callback: CallbackFunction? = null
    )

    // Reset options to default values.
    fun init() {
        safeMode = 0
        htmlReplacement = "<mark>replaced HTML</mark>"
        callback = doNothing
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
        if (options.reset) Api.init() // Reset takes priority.
        // Only update specified (non-null) options.
        safeMode = options.safeMode ?: safeMode
        htmlReplacement = options.htmlReplacement ?: htmlReplacement
        callback = options.callback ?: callback
    }

    // Set named option value.
    fun update(name: String, value: String) {
        try {
            when (name) {
                "safeMode" -> safeMode = value.toInt()
                "htmlReplacement" -> htmlReplacement = value
                "reset" -> if (value.toBoolean()) Api.init()
                else -> throw IllegalArgumentException()
            }
        } catch(e: Exception) {
            throw IllegalArgumentException("Illegal API option: '$name=$value'")
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

}
