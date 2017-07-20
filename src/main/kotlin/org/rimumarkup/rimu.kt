@file:JvmName("rimu")

package org.rimumarkup

typealias CallbackFunction = (message: CallbackMessage) -> Unit

data class CallbackMessage(val type: String, val text: String)

val doNothing: CallbackFunction = fun(message: CallbackMessage) = Unit   // Default render() callback.

/**
 * Public API to translate Rimu Markup to HTML.
 */
fun render(source: String, opts: Options.RenderOptions = Options.RenderOptions()): String {
    Options.updateOptions(opts)
    return Api.render(source)
}

object Api {

    init {
        init()
    }

    // Set API to default state.
    fun init() {
        Options.init()
// TODO
//        BlockAttributes.init()
//        DelimitedBlocks.init()
//        Macros.init()
//        Quotes.init()
//        Replacements.init()
    }

    fun render(source: String): String {
        return "<p>${source.trim()}</p>"
    }

}

