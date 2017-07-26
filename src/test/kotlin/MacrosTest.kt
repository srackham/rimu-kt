import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.rimumarkup.Api
import org.rimumarkup.Macros

class MacrosTest {

    @Before
    fun before() {
        Api.init()
    }

    @Test
    fun renderTest() {
        assertEquals(0, Macros.defs.size)

        Macros.setValue("x", "1")
        assertEquals(1, Macros.defs.size)
        assertEquals("1", Macros.getValue("x"))
        assertEquals(null, Macros.getValue("y"))

        assertEquals("{x} = 1", Macros.render("\\{x} = {x}"))

        val input = """{x} = '1$1$2'
{x?} = '2'
\{x}={x|}
{x|2|3}"""
        val result = Api.render(input)
        assertEquals("""<p>{x}=1
123</p>""", result)
    }
}

