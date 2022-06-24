import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.rimumarkup.Api
import org.rimumarkup.Io
import org.rimumarkup.Lists

class ListsBlocksTest {

    @BeforeEach
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        val input = """- Item 1
""
Quoted
""
- Item 2
 . Nested 1"""
        val reader = Io.Reader(input)
        val writer = Io.Writer()

        Lists.render(reader, writer)
        assertEquals("""<ul><li>Item 1<blockquote><p>Quoted</p></blockquote>
</li><li>Item 2<ol><li>Nested 1</li></ol></li></ul>""",
                writer.toString())
    }
}


