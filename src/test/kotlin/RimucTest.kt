import org.junit.Test
import org.junit.Assert.*
import org.rimumarkup.main
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class RimucTest {
    @Test fun help() {
        val savedOut = System.out
        val out: String
        try {
            val myOut = ByteArrayOutputStream()
            System.setOut(PrintStream(myOut))
            main(arrayOf("-h"))
            out = myOut.toString()
        } finally {
            System.setOut(savedOut)
        }
        print("out: $out")
        assertTrue("help message starts with NAME",out.startsWith("\nNAME"))
    }
}
