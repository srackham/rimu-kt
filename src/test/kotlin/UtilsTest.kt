import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.rimumarkup.Utils
import org.rimumarkup.fileToString
import org.rimumarkup.readResouce
import org.rimumarkup.stringToFile
import java.nio.file.Paths

class UtilsTest {

    @Rule
    @JvmField
    var tempFolderRule = TemporaryFolder()

    @Test
    fun readResouceTest() {
        val text = readResouce("/test.txt") // Throws exception if not found.
        assertEquals("Test resouce", text)
    }

    @Test
    fun stringToFromFile() {
        // Test file read/write string function.
        val fileName = Paths.get(tempFolderRule.root.path, "test.txt").toString()
        stringToFile("Hello World!", fileName)
        assertEquals("Hello World!", fileToString(fileName))
    }

    @Test
    fun replaceSpecialCharsTest() {
        assertEquals("&lt;&lt;Hello &amp; goodbye!&gt;&gt;", Utils.replaceSpecialChars("<<Hello & goodbye!>>"))
    }

}
