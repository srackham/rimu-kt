import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemOutRule
import org.rimumarkup.main


class RimucTest {
    @Rule
    @JvmField
    val systemOutRule = SystemOutRule().enableLog()

    @Rule
    @JvmField
    val exit = ExpectedSystemExit.none()

    @Test
    fun helpCommand() {
        exit.expectSystemExitWithStatus(0)
        main(arrayOf("-h"))
        assertTrue("help message starts with NAME", systemOutRule.log.startsWith("\nNAME"))
    }

    @Test
    fun noOutputFileSpecified() {
        exit.expectSystemExitWithStatus(1)
        main(arrayOf("-o"))
    }
}
