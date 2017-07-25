import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api

class ApiTest {

    @Before
    fun before() {
        Api.init()
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
"""
        val result = Api.render(input)
        assertEquals("""<h1>Title</h1>
<p>Paragraph <strong>bold</strong> <code>code</code> <em>emphasised text</em></p>
<pre class="test-class" title="Code"><code>Indented `paragraph`</code></pre>
<ul><li>Item 1<blockquote><p>Quoted</p></blockquote>
</li><li>Item 2<ol><li>Nested 1</li></ol></li></ul>""",
                result)
    }

}

