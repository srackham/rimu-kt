import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.rimumarkup.Document
import org.rimumarkup.DelimitedBlocks
import org.rimumarkup.Io

class DelimitedBlocksTest {

    @BeforeEach
    fun before() {
        Document.init()
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
