import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.rimumarkup.Api
import org.rimumarkup.Io
import org.rimumarkup.LineBlocks

class LineBlocksTest {

    @BeforeEach
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        val input = "# Test"
        val reader = Io.Reader(input)
        val writer = Io.Writer()

        LineBlocks.render(reader, writer)
        assertEquals("<h1>Test</h1>", writer.toString())
    }
}

