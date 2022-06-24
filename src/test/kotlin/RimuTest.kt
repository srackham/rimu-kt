import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.beust.klaxon.*
import org.rimumarkup.*

/*

  Rimu syntax tests driven by rimu-tests.json.
  The same tests are run by rimu-js and are driven by the same JSON resource file
  Tests the Rimu render() API (c.f. rimu-tests.js).

 */

private class RimuTestOptions(
    val reset: Boolean = false,
    val safeMode: Int? = null,
    val htmlReplacement: String? = null
)

private class RimuTestSpec(
    val unsupported: String = "",
    val description: String,
    val input: String,
    val expectedOutput: String,
    val expectedCallback: String,
    val options: RimuTestOptions
)

private fun parseRimuTestSpecs(jsonText: String): List<RimuTestSpec>? {
    return Klaxon().parseArray(jsonText)
}

class RimuTest {

    private val catchLint: CallbackFunction = fun(message: CallbackMessage) {
        throw AssertionError("unexpected callback: ${message.type}: ${message.text}")
    }

    /**
     * Execute test cases specified in JSON file rimu-tests.json
     */
    @Test
    fun rimuCompatibilityTests() {
        val jsonText = readResource("/rimu-tests.json")
        val tests = parseRimuTestSpecs(jsonText)!!
        for ((index, test) in tests.withIndex()) {
            val description = test.description
            System.err.println("$index: $description")
            val renderOptions = RenderOptions()
            val unsupported = test.unsupported.contains("kt")
            renderOptions.safeMode = test.options.safeMode
            renderOptions.htmlReplacement = test.options.htmlReplacement
            renderOptions.reset = test.options.reset
            var msg = ""
            if (test.expectedCallback.isNotBlank() || unsupported) {
                renderOptions.callback = fun(message) { msg += "${message.type}: ${message.text}\n" } // Capture the callback message.
            } else {
                renderOptions.callback = catchLint  // Callback should not occur, this will throw an error.
            }
            val result = render(test.input, renderOptions)
            if (unsupported) {
                assertTrue( msg.trim().startsWith("error: unsupported"),description)
            } else {
                assertEquals(test.expectedOutput, result,description)
                if (test.expectedCallback.isNotBlank()) {
                    assertTrue(msg.trim() == test.expectedCallback,description)
                }
            }
        }
    }
}