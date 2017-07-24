import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api
import org.rimumarkup.DelimitedBlocks
import org.rimumarkup.Io

class DelimitedBlocksTest {

    @Before
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        var input = "Test"
        var reader = Io.Reader(input)
        var writer = Io.Writer()

        DelimitedBlocks.render(reader, writer)
        assertEquals("<p>Test</p>", writer.toString())

        input = "  Indented"
        reader = Io.Reader(input)
        writer = Io.Writer()
        DelimitedBlocks.render(reader, writer)
        assertEquals("<pre><code>Indented</code></pre>", writer.toString())
    }
}
