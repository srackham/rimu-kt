import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.rimumarkup.RenderOptions
import org.rimumarkup.render

/**
 * Simplest Kotlin Rimu example.
 */
class KotlinExamplesTest {

    @BeforeEach
    fun before() {
        // Initialize Rimu to default state.
        render("", RenderOptions(reset = true))
    }

    @Test
    fun simpleExampleTest() {
        val result = render("Hello *Rimu*!", RenderOptions(reset = true, safeMode = 2))
        assertEquals("<p>Hello <em>Rimu</em>!</p>", result)
    }

    @Test
    fun callbackExampleTest() {
        val options = RenderOptions()
        var callbackMessage = ""
        options.callback = fun(message) { callbackMessage = "${message.type}: ${message.text}" } // Capture the callback message.
        val result = render("Unknown {x}", options)
        assertEquals("<p>Unknown {x}</p>", result)
        assertEquals("error: undefined macro: {x}: Unknown {x}", callbackMessage)
    }
}

