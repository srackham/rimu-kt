package org.rimumarkup

object Document {

    init {
        init()
    }

    // Set API to default state.
    fun init() {
        Options.init()
        BlockAttributes.init()
        DelimitedBlocks.init()
        Macros.init()
        Quotes.init()
        Replacements.init()
    }

    // Render text with current Options state.
    fun render(text: String): String {
        val reader = Io.Reader(text)
        val writer = Io.Writer()
        while (!reader.eof()) {
            reader.skipBlankLines()
            if (reader.eof()) break
            if (LineBlocks.render(reader, writer)) continue
            if (Lists.render(reader, writer)) continue
            if (DelimitedBlocks.render(reader, writer)) continue
            // This code should never be executed (normal paragraphs should match anything).
            Options.panic("no matching delimited block found")
        }
        return writer.toString()

    }

}

