@file:JvmName("rimu")

package org.rimumarkup

typealias CallbackFunction = (message: CallbackMessage) -> Unit

data class CallbackMessage(val type: String, val text: String)

val doNothing: CallbackFunction = fun(_: CallbackMessage) = Unit   // Default render() callback.

/**
 * Public API to translate Rimu Markup to HTML.
 * @throws [Exception]
 */
fun render(text: String, opts: Options.RenderOptions = Options.RenderOptions()): String {
    Options.update(opts)
    return Api.render(text)
}

object Api {

    init {
        init()
    }

    // Set API to default state.
    fun init() {
        Options.init()
        BlockAttributes.init()
        DelimitedBlocks.init()
//        Macros.init()
        Quotes.init()
        Replacements.init()
    }

    // Render text with current Options state.
    // TODO: This seems to be redundant, it's equivealent to public render(text) with default opts, so this object is Init.
    fun render(text: String): String {
        val reader = Io.Reader(text)
        val writer = Io.Writer()
        while (!reader.eof()) {
            reader.skipBlankLines()
            if (reader.eof()) break
            if (LineBlocks.render(reader, writer)) continue
//            if (Lists.render(reader, writer)) continue
            if (DelimitedBlocks.render(reader, writer)) continue
            // This code should never be executed (normal paragraphs should match anything).
            throw RimucException("unexpected error: no matching delimited block found")
        }
        return writer.toString()

    }

}

