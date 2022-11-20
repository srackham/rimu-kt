import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.rimumarkup.Document
import org.rimumarkup.DelimitedBlocks
import org.rimumarkup.Quotes
import org.rimumarkup.Replacements

class ApiTest {

    @BeforeEach
    fun before() {
        Document.init()
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
        assertTrue( def2 !== def1,"DelimitedBlocks.init() shallow copy")
        assertTrue( def2.expansionOptions.spans == true,"DelimitedBlocks.init() deep copy")

        Quotes.defs[0].openTag = "TEST"
        Quotes.init()
        assertTrue( Quotes.defs[0].openTag != "TEST","Quotes.init() shallow copy")

        Replacements.defs[0].replacement = "TEST"
        Replacements.init()
        assertTrue( Replacements.defs[0].replacement != "TEST","Replacements.init() shallow copy")
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
        val result = Document.render(input)
        assertEquals("""<h1>Title</h1>
<p>Paragraph <strong>bold</strong> <code>code</code> <em>emphasised text</em></p>
<pre class="test-class" title="Code"><code>Indented `paragraph`</code></pre>
<ul><li>Item 1<blockquote><p>Quoted</p></blockquote>
</li><li>Item 2<ol><li>Nested 1</li></ol></li></ul><p>{x}=1
123</p>""",
                result)
    }

}

