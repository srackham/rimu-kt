import com.beust.klaxon.Klaxon
import com.github.stefanbirkner.systemlambda.SystemLambda.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.rimumarkup.*
import java.io.FileOutputStream
import java.nio.file.Path


class RimuktTest {
    @TempDir
    @JvmField
    var tempDir: Path? = null

    /*
        Helpers.
     */
    private class RimucTestSpec(
        val unsupported: String = "",
        val description: String,
        val args: String,
        val input: String,
        val expectedOutput: String,
        val exitCode: Int = 0,
        val predicate: String,
        val layouts: Boolean = false
    )

    private fun parseRimucTestSpecs(jsonText: String): List<RimucTestSpec>? {
        return Klaxon().parseArray(jsonText)
    }

    private fun noRimurc(args: Array<String>) {
        // Do not include the .rimurc file.
        val argsList = arrayListOf("--no-rimurc")
        argsList.addAll(args)
        rimukt(argsList.toTypedArray())
    }

    private fun expectRimucException(args: Array<String>, message: String) {
        assertThrows(RimucException::class.java, {noRimurc(args)},message)
    }

    /*
        main() wrapper tests.
     */
    @Test
    fun exitCodeZero() {
        val statusCode = catchSystemExit {
            main(arrayOf("-h"))
        }
        assertEquals(0, statusCode)
    }

    @Test
    fun exitCodeOne() {
        val statusCode = catchSystemExit {
            main(arrayOf("--no-rimurc", "missing-file-name"))
        }
        assertEquals(1, statusCode)
    }

    /*
        rimukt() compiler tests.
     */
    @Test
    fun checkResourceExists() {
        // Throws exception if there is a missing resource file.
        for (style in arrayOf("classic", "flex", "plain","sequel","v8")) {
            readResource("$style-header.rmu")
            readResource("$style-footer.rmu")
        }
        readResource("manpage.txt")
    }

    @Test
    fun helpCommand() {
        val output = tapSystemOut {
            noRimurc(arrayOf("-h"))
        }
        assertTrue(output.startsWith("\nNAME"),"help message starts with NAME")
    }

    @Test
    fun missingOutputArgument() {
        expectRimucException(
            args = arrayOf("--output"),
            message = "missing --output option value"
        )
    }

    @Test
    fun missingInputFile() {
        expectRimucException(
            args = arrayOf("missing-file-name"),
            message = "missing-file-name"
        )
    }

    @Test
    fun compileFromFiles() {
        // Write two temporary input files to be compiled.
        val file1= (tempDir?.resolve("test-file-1")).toString()
        FileOutputStream(file1).writeTextAndClose("Hello World!")
        val file2= (tempDir?.resolve("test-file-2")).toString()
        FileOutputStream(file2).writeTextAndClose("Hello again World!")
        val output = tapSystemOut {
            noRimurc(arrayOf(file1, file2))
        }
        assertEquals("<p>Hello World!</p>\n<p>Hello again World!</p>", output)
    }

    @Test
    fun compileToFile() {
        val fileName= (tempDir?.resolve("test-file")).toString()
        withTextFromSystemIn("Hello World!")
            .execute {
                noRimurc(arrayOf("-o", fileName))
            }
        assertEquals("<p>Hello World!</p>", fileToString(fileName))
    }

    @Test
    fun compileToImplicitStyledOutputFile() {
        // If the --styled option is specified and a single input file then an output HTML file with the same file name is generated.
        val infile= (tempDir?.resolve("test-file.rmu")).toString()
        FileOutputStream(infile).writeTextAndClose("Hello World!")
        noRimurc(arrayOf("--styled", infile))
        val outfile = infile.replaceAfterLast('.', "html")
        val text = fileToString(outfile)
        assertTrue(text.contains("<!DOCTYPE HTML>"))
        assertTrue(text.contains("<p>Hello World!</p>"))
    }

    @Test
    fun compileHtmlFile() {
        // Input files with .html extensions are passed through.
        val infile= (tempDir?.resolve("test-file.html")).toString()
        FileOutputStream(infile).writeTextAndClose("<p>Hello World!</p>")
        val output = tapSystemErrAndOut {
            noRimurc(arrayOf(infile))
        }
        assertEquals("<p>Hello World!</p>", output)
    }

    /**
     * Execute test cases specified in JSON file rimuc-tests.json
     */
    @Test
    fun rimucCompatibilityTests() {
        val jsonText = readResource("/rimuc-tests.json")
        val tests = parseRimucTestSpecs(jsonText)!!
        for ((index, test) in tests.withIndex()) {
            if (test.unsupported.contains("kt")) {
                continue
            }
            for (layout in listOf("", "classic", "flex", "sequel")) {
                // Skip if not a layouts test and we have a layout, or if it is a layouts test but no layout is specified.
                if (!test.layouts && layout.isNotBlank() || test.layouts && layout.isBlank()) {
                    continue
                }
                val description = test.description
                System.err.println("$index: $description")
                val expectedOutput = test.expectedOutput
                    .replace("./test/fixtures/", "./src/test/fixtures/")
                val exitCode = test.exitCode
                // Convert args String to Array<String>.
                var args = test.args
                    .replace("./test/fixtures/", "./src/test/fixtures/")
                    .replace("./examples/example-rimurc.rmu", "./src/test/fixtures/example-rimurc.rmu")
                if (layout.isNotBlank()) {
                    args = """--layout $layout $args"""
                }
                val argsArray: Array<String> = if (args.isNotBlank()) {
                    args.trim().split(Regex("""\s+"""))
                        .map { it.removeSurrounding("\"") }
                        .toTypedArray()
                } else {
                    arrayOf()   // Use empty array is there are no arguments.
                }
                var exceptionThrown = false
                var output = tapSystemErrAndOut {
                    withTextFromSystemIn(test.input)
                        .execute {
                    try {
                        noRimurc(argsArray)
                    } catch (e: Exception) {
                        exceptionThrown = true
                    }
                }
                }
                output = output.replace("\r","")    // Strip Windows return characters.
                if (exitCode != 0) {
                    assertTrue( exceptionThrown,description)
                } else {
                    assertFalse( exceptionThrown,description)
                }
                when (test.predicate) {
                    "contains" ->
                        assertTrue( output.contains(expectedOutput),description)
                    "!contains" ->
                        assertFalse( output.contains(expectedOutput),description)
                    "equals" ->
                        assertEquals( expectedOutput, output,description)
                    "!equals" ->
                        assertNotEquals( expectedOutput, output,description)
                    "startsWith" ->
                        assertTrue( output.startsWith(expectedOutput),description)
                    else -> throw IllegalArgumentException("""${description}: illegal predicate: ${test.predicate}""")
                }
            }
        }
    }
}
