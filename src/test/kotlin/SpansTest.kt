import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Quotes
import org.rimumarkup.Replacements
import org.rimumarkup.Spans

/**
 * Basic rendering tests (full syntax tested in RenderTest).
 */
class SpansTest {
    @Before
    fun init() {
        Quotes.init()
        assertEquals(Quotes.DEFAULT_DEFS.size, Quotes.defs.size)
        Replacements.init()
        assertEquals(Replacements.DEFAULT_DEFS.size, Replacements.defs.size)
    }

    @Test
    fun quotesTest() {
        var def: Quotes.Definition? = Quotes.getDefinition("`")
        assertNotNull(def?.openTag)
        assertEquals("<code>", def?.openTag)

        def = Quotes.getDefinition("x")
        assertNull(def?.openTag)

        Quotes.setDefinition(Quotes.Definition(quote = "__", openTag = "<u>", closeTag = "</u>", spans = true))
        assertEquals(Quotes.DEFAULT_DEFS.size, Quotes.defs.size)
        Quotes.setDefinition(Quotes.Definition(quote = "==", openTag = "<del>", closeTag = "</del>", spans = true))
        assertEquals(Quotes.DEFAULT_DEFS.size + 1, Quotes.defs.size)
    }

    @Test
    fun replacementsTest() {
        Replacements.setDefinition(pattern = """\\?--(?!>)""", flags = "i", replacement = "&mdash;")
        assertEquals(Replacements.DEFAULT_DEFS.size + 1, Replacements.defs.size)
    }

    @Test
    fun spansTest() {
        var input = "Hello *Cruel* World!"
        val frags = Spans.fragQuote(Spans.Fragment(text = input, done = false))
        assertEquals(5, frags.size)
        var output = Spans.defrag(frags)
        assertEquals("Hello <em>Cruel</em> World!", output)
        assertEquals(output, Spans.render(input))

        input = "**[Link](http://example.com)**"
        output = Spans.render(input)
        assertEquals("<strong><a href=\"http://example.com\">Link</a></strong>", output)
    }
}
