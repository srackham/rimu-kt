import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.rimumarkup.*
import java.nio.file.Path

class UtilsTest {

    @BeforeEach
    fun before() {
        Api.init()
    }

    @TempDir
    @JvmField
    var tempDir: Path? = null

    @Test
    fun readResouceTest() {
        val text = readResource("/test.txt") // Throws exception if not found.
        assertEquals("Test resouce", text)
    }

    @Test
    fun stringToFromFile() {
        // Test file read/write string function.
        val fileName= (tempDir?.resolve("test.txt")).toString()
        stringToFile("Hello World!", fileName)
        assertEquals("Hello World!", fileToString(fileName))
    }

    @Test
    fun replaceSpecialCharsTest() {
        assertEquals("&lt;&lt;Hello &amp; goodbye!&gt;&gt;", Utils.replaceSpecialChars("<<Hello & goodbye!>>"))
    }

    @Test
    fun expansionOptionsTest() {
        var opts = ExpansionOptions(macros = true, specials = false)
        opts.merge(ExpansionOptions(macros = false, container = true))
        assertEquals(ExpansionOptions(
                macros = false,
                container = true,
                skip = null,
                spans = null,
                specials = false),
                opts)

        opts = ExpansionOptions(macros = true, specials = false)
        opts.parse("-macros +spans")
        assertEquals(ExpansionOptions(
                macros = false,
                container = null,
                skip = null,
                spans = true,
                specials = false),
                opts)
    }

    @Test
    fun replaceMatchTest() {
        val match = Regex("""...(...)6(...)""").find("0123456*&*")
        val actual = Utils.replaceMatch(match?.groupValues!!, "$1 $2 $$2", ExpansionOptions())
        assertEquals("345 *&amp;* <em>&amp;</em>", actual)
    }

    @Test
    fun injectHtmlAttributesTest() {
        BlockAttributes.classes = "foo bar"
        BlockAttributes.attributes = """title="Hello!""""
        BlockAttributes.options = ExpansionOptions()
        val result = BlockAttributes.injectHtmlAttributes("""<p class="do">""")
        assertEquals("""<p title="Hello!" class="foo bar do">""", result)
    }
}
