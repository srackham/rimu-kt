import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemErrRule
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.contrib.java.lang.system.TextFromStandardInputStream
import org.junit.rules.ExpectedException
import org.rimumarkup.Rimuc
import org.rimumarkup.RimucException
import org.rimumarkup.main

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
        main(arrayOf("--illegal-option"))
    }

    /*
        Rimuc() fompiler tests.
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
    @Ignore
    fun missingInputFile() {
        expectException(
                args = arrayOf("MISSING_FILE_NAME"),
                type = RimucException::class.java,
                message = "missing input file: MISSING_FILE_NAME"
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
}
