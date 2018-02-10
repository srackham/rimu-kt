import com.beust.klaxon.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.rimumarkup.*

/*

  Rimu syntax tests driven by rimu-tests.json.
  The same tests are run by rimu-js and are driven by the same JSON resource file
  Tests the Rimu render() API (c.f. rimu-tests.js).

 */

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
        @Suppress("UNCHECKED_CAST")
        val tests = parseJsonText(jsonText) as JsonArray<JsonObject>
        for ((index, test) in tests.withIndex()) {
            val description = test.string("description") ?: ""
            System.err.println("$index: $description")
            val input = test.string("input") ?: ""
            val expectedOutput = test.string("expectedOutput") ?: ""
            val expectedCallback = test.string("expectedCallback") ?: ""
            val options = test.obj("options") as JsonObject
            val renderOptions = RenderOptions()
            val unsupported = (test.string("unsupported") ?: "").contains("kt")
            renderOptions.safeMode = options.int("safeMode")
            renderOptions.htmlReplacement = options.string("htmlReplacement")
            renderOptions.reset = options.boolean("reset") ?: false
            var msg = ""
            if (expectedCallback.isNotBlank() || unsupported) {
                renderOptions.callback = fun(message) { msg += "${message.type}: ${message.text}\n" } // Capture the callback message.
            } else {
                renderOptions.callback = catchLint  // Callback should not occur, this will throw an error.
            }
            val result = render(input, renderOptions)
            if (unsupported) {
                assertTrue(description, msg.trim().startsWith("error: unsupported"))
            } else {
                assertEquals(description, expectedOutput, result)
                if (expectedCallback.isNotBlank()) {
                    assertTrue(description, msg.trim().equals(expectedCallback))
                }
            }
        }
    }
}