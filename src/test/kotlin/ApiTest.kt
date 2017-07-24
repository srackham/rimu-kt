import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api

class ApiTest {

    @Before
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        var input = """Paragraph

  Indented paragraph
"""
        var result = Api.render(input)
        assertTrue(result.matches(Regex("""(?s)<p>.*?</p>.*?<pre><code>.*?</code></pre>""")))
    }

}

