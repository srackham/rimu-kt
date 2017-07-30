import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api
import org.rimumarkup.DelimitedBlocks
import org.rimumarkup.Quotes
import org.rimumarkup.Replacements

class ApiTest {

    @Before
    fun before() {
        Api.init()
    }

    /**
     * Ensure definitions are reset by init().
     */
    @Test
    fun initTest() {
        val def1 = DelimitedBlocks.getDefinition("paragraph")!!
        def1.expansionOptions.spans = false
        DelimitedBlocks.init()
        val def2 = DelimitedBlocks.getDefinition("paragraph")!!
        assertTrue("DelimitedBlocks.init() shallow copy", def2 !== def1)
        assertTrue("DelimitedBlocks.init() deep copy", def2.expansionOptions.spans == true)

        Quotes.defs[0].openTag = "TEST"
        Quotes.init()
        assertTrue("Quotes.init() shallow copy", Quotes.defs[0].openTag != "TEST")

        Replacements.defs[0].replacement = "TEST"
        Replacements.init()
        assertTrue("Replacements.init() shallow copy", Replacements.defs[0].replacement != "TEST")
    }

    /**
     * Basic rendering tests (full syntax tested in RenderTest).
     */
    @Test
    fun renderTest() {
        val input = """# Title
Paragraph **bold** `code` _emphasised text_

.test-class [title="Code"]
  Indented `paragraph`

- Item 1
""
Quoted
""
- Item 2
 . Nested 1

{x} = '1${'$'}1${'$'}2'
{x?} = '2'
\{x}={x|}
{x|2|3}
"""
        val result = Api.render(input)
        assertEquals("""<h1>Title</h1>
<p>Paragraph <strong>bold</strong> <code>code</code> <em>emphasised text</em></p>
<pre class="test-class" title="Code"><code>Indented `paragraph`</code></pre>
<ul><li>Item 1<blockquote><p>Quoted</p></blockquote>
</li><li>Item 2<ol><li>Nested 1</li></ol></li></ul><p>{x}=1
123</p>""",
                result)
    }

}

