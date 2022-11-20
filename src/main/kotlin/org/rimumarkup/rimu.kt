/*
    The Rimu published API.
 */
@file:JvmName("Rimu")

package org.rimumarkup

data class CallbackMessage(
        @JvmField
        val type: String,
        @JvmField
        val text: String
)

typealias CallbackFunction = (message: CallbackMessage) -> Unit

val DO_NOTHING: CallbackFunction = fun(_: CallbackMessage) = Unit   // Default render() callback.

data class RenderOptions(
        @JvmField
        var safeMode: Int? = null,
        @JvmField
        var htmlReplacement: String? = null,
        @JvmField
        var reset: Boolean = false,
        @JvmField
        var callback: CallbackFunction? = null
)

/**
 * Public API to translate Rimu Markup to HTML.
 * @throws [Exception]
 */
@JvmOverloads
fun render(text: String, options: RenderOptions = RenderOptions()): String {
    // Force object instantiation before Options.update().
    // Otherwise the ensuing Document.render() will instanitate Document and the Document init{} block will reset Options.
    Document // Ensure Document is instantiated.
    Options.update(options)
    return Document.render(text)
}
