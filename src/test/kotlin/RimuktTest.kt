import com.beust.klaxon.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemErrRule
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.contrib.java.lang.system.TextFromStandardInputStream
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.rimumarkup.*
import java.io.FileOutputStream
import java.nio.file.Paths


fun parseJsonText(jsonText: String): Any? {
    val parser = Parser()
    val stringBuilder = StringBuilder(jsonText)
    val result = parser.parse(stringBuilder)
    return result
}

class RimuktTest {
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
    private fun noRimurc(args: Array<String>) {
        // Do not include the .rimurc file.
        val argsList = arrayListOf("--no-rimurc")
        argsList.addAll(args)
        rimukt(argsList.toTypedArray())
    }

    private fun expectException(args: Array<String>, type: Class<out Exception>, message: String) {
        exceptionRule.expect(type)
        exceptionRule.expectMessage(message)
        noRimurc(args)
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
        // Throws java.io.FileNotFoundException.
        main(arrayOf("--no-rimurc", "missing-file-name"))
    }

    /*
        rimukt() compiler tests.
     */
    @Test
    fun checkResourceExists() {
        // Throws exception if there is a missing resource file.
        for (style in arrayOf("classic", "flex", "v8")) {
            readResource("$style-header.rmu")
            readResource("$style-footer.rmu")
        }
    }

    @Test
    fun helpCommand() {
        noRimurc(arrayOf("-h"))
        assertTrue("help message starts with NAME", systemOutRule.log.startsWith("\nNAME"))
    }

    @Test
    fun missingOutputArgument() {
        expectException(
                args = arrayOf("--output"),
                type = RimucException::class.java,
                message = "missing --output option value"
        )
    }

    @Test
    fun missingInputFile() {
        expectException(
                args = arrayOf("missing-file-name"),
                type = RimucException::class.java,
                message = "missing-file-name"
        )
    }

    @Test
    fun compileFromFiles() {
        // Write two temporary input files to be compiled.
        val file1 = tempFolderRule.newFile("test-file-1")
        FileOutputStream(file1).writeTextAndClose("Hello World!")
        val file2 = tempFolderRule.newFile("test-file-2")
        FileOutputStream(file2).writeTextAndClose("Hello again World!")
        noRimurc(arrayOf(file1.path, file2.path))
        assertEquals("<p>Hello World!</p>\n<p>Hello again World!</p>", systemOutRule.log)
    }

    @Test
    fun compileToFile() {
        stdinMock.provideLines("Hello World!")
        val fileName = Paths.get(tempFolderRule.root.path, "test-file").toString()
        noRimurc(arrayOf("-o", fileName))
        assertEquals("<p>Hello World!</p>", fileToString(fileName))
    }

    @Test
    fun compileToImplicitStyledOutputFile() {
        // If the --styled option is specified and a single input file then an output HTML file with the same file name is generated.
        val infile = tempFolderRule.newFile("test-file.rmu")
        FileOutputStream(infile).writeTextAndClose("Hello World!")
        noRimurc(arrayOf("--styled", infile.path))
        val outfile = infile.path.replaceAfterLast('.', "html")
        val text = fileToString(outfile)
        assertTrue(text.contains("<!DOCTYPE HTML>"))
        assertTrue(text.contains("<p>Hello World!</p>"))
    }

    @Test
    fun compileHtmlFile() {
        // Input files with .html extensions are passed through.
        val infile = tempFolderRule.newFile("test-file.html")
        FileOutputStream(infile).writeTextAndClose("<p>Hello World!</p>")
        noRimurc(arrayOf(infile.path))
        assertEquals("<p>Hello World!</p>", systemOutRule.log)
    }

    /**
     * Execute test cases specified in JSON file rimuc-tests.json
     */
    @Test
    fun rimucCompatibilityTests() {
        val jsonText = readResource("/rimuc-tests.json")
        @Suppress("UNCHECKED_CAST")
        val tests = parseJsonText(jsonText) as JsonArray<JsonObject>
        for ((index, test) in tests.withIndex()) {
            val unsupported = (test.string("unsupported") ?: "").contains("kt")
            if (unsupported) {
                continue
            }
            for (layout in listOf<String>("", "classic", "flex", "sequel")) {
                val layouts = test.boolean("layouts") ?: false
                // Skip if not a layouts test and we have a layout, or if it is a layouts test but no layout is specified.
                if (!layouts && layout.isNotBlank() || layouts && layout.isBlank()) {
                    continue
                }
                val description = test.string("description") ?: ""
                System.err.println("$index: $description")
                val input = test.string("input") ?: ""
                val expectedOutput = (test.string("expectedOutput") ?: "")
                        .replace("./test/fixtures/", "./src/test/fixtures/")
                val predicate = test.string("predicate") ?: ""
                val exitCode = test.int("exitCode") ?: 0
                // Convert args String to Array<String>.
                var args = (test.string("args") ?: "")
                        .replace("./test/fixtures/", "./src/test/fixtures/")
                        .replace("./src/examples/example-rimurc.rmu", "./src/test/fixtures/example-rimurc.rmu")
                if (layout.isNotBlank()) {
                    args = """--layout $layout $args"""
                }
                val argsArray: Array<String>
                if (args.isNotBlank()) {
                    argsArray = args.trim().split(Regex("""\s+"""))
                            .map { it.removeSurrounding("\"") }
                            .toTypedArray()
                } else {
                    argsArray = arrayOf()   // Use empty array is there are no arguments.
                }
                stdinMock.provideLines(input)
                systemOutRule.clearLog()
                systemErrRule.clearLog()
                var exceptionThrown = false
                try {
                    noRimurc(argsArray)
                } catch (e: Exception) {
                    exceptionThrown = true
                }
                val output = systemOutRule.log + systemErrRule.log
                if (exitCode != 0) {
                    assertTrue(description, exceptionThrown)
                } else {
                    assertFalse(description, exceptionThrown)
                }
                when (predicate) {
                    "contains" ->
                        assertTrue(description, output.contains(expectedOutput))
                    "!contains" ->
                        assertFalse(description, output.contains(expectedOutput))
                    "equals" ->
                        assertEquals(description, expectedOutput, output)
                    "!equals" ->
                        assertNotEquals(description, expectedOutput, output)
                    "startsWith" ->
                        assertTrue(description, output.startsWith(expectedOutput))
                    else -> throw IllegalArgumentException("""${description}: illegal predicate: ${predicate}""")
                }
            }
        }
    }
}
