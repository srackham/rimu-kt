import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemErrRule
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.contrib.java.lang.system.TextFromStandardInputStream
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.rimumarkup.*
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Paths


class RimucTest {
    @Rule
    @JvmField
    val systemOutRule = SystemOutRule().enableLog()

    @Rule
    @JvmField
    val systemErrRule = SystemErrRule().enableLog()

    // NOTE: The target of this rule does not return if it executes a System.exit() so you can't follow it with additional tests.
    @Rule
    @JvmField
    val exitRule = ExpectedSystemExit.none()

    @Rule
    @JvmField
    val stdinMock = TextFromStandardInputStream.emptyStandardInputStream()

    @Rule
    @JvmField
    var exceptionRule = ExpectedException.none()

    @Rule
    @JvmField
    var tempFolderRule = TemporaryFolder()


    /*
        Helper functions.
     */
    fun compileStdin(message: String, args: Array<String> = arrayOf(), source: String, expected: String) {
        stdinMock.provideLines(source)
        Rimuc(args)
        assertEquals(message, expected, systemOutRule.log)
    }

    fun expectException(args: Array<String>, type: Class<out Exception>, message: String) {
        exceptionRule.expect(type)
        exceptionRule.expectMessage(message)
        Rimuc(args)
    }

    /*
        main() wrapper tests.
     */
    @Test
    fun exitCodeZero() {
        exitRule.expectSystemExitWithStatus(0)
        main(arrayOf("-h"))
    }

    @Test
    fun exitCodeOne() {
        exitRule.expectSystemExitWithStatus(1)
        // Throws RimuException.
        main(arrayOf("--illegal-option"))
    }

    @Test
    fun exitCodeTwo() {
        exitRule.expectSystemExitWithStatus(2)
        // Throws java.io.FileNotFoundException.
        main(arrayOf("MISSING_FILE_NAME"))
    }

    /*
        Rimuc() compiler tests.
     */
    @Test
    fun helpCommand() {
        Rimuc(arrayOf("-h"))
        assertTrue("help message starts with NAME", systemOutRule.log.startsWith("\nNAME"))
    }

    @Test
    fun missingOutputArgument() {
        expectException(
                args = arrayOf("-o"),
                type = RimucException::class.java,
                message = "missing --output argument"
        )
    }

    @Test
    fun missingInputFile() {
        expectException(
                args = arrayOf("MISSING_FILE_NAME"),
                type = FileNotFoundException::class.java,
                message = "MISSING_FILE_NAME"
        )
    }

    @Test
    fun compilations() {
        compileStdin(
                source = "Hello World!",
                expected = "<p>Hello World!</p>",
                message = "rimuc basic test"
        )
    }

    @Test
    fun compileFromFiles() {
        // Write two temporary input files to be compiled.
        val file1 = tempFolderRule.newFile("TEST_FILE_1")
        FileOutputStream(file1).writeTextAndClose("Hello World!")
        val file2 = tempFolderRule.newFile("TEST_FILE_2")
        FileOutputStream(file2).writeTextAndClose("Hello again World!")
        Rimuc(arrayOf(file1.path, file2.path))
        assertEquals("<p>Hello World!</p>\n<p>Hello again World!</p>", systemOutRule.log)
    }

    @Test
    fun compileToFile() {
        stdinMock.provideLines("Hello World!")
        val fileName = Paths.get(tempFolderRule.root.path, "TEST_FILE").toString()
        Rimuc(arrayOf("-o", fileName))
        assertEquals("<p>Hello World!</p>", fileToString(fileName))
    }
}
