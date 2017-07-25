import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api
import org.rimumarkup.Io
import org.rimumarkup.Lists

class ListsBlocksTest {

    @Before
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        val input = """- Item 1
- Item 2
 . Nested 1"""
        val reader = Io.Reader(input)
        val writer = Io.Writer()

        Lists.render(reader, writer)
        assertEquals("<ul><li>Item 1</li><li>Item 2<ol><li>Nested 1</li></ol></li></ul>", writer.toString())
    }
}


