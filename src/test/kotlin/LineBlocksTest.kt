//TODO: Is this file necessary?
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api
import org.rimumarkup.Io
import org.rimumarkup.LineBlocks

class LineBlocksTest {

    @Before
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

