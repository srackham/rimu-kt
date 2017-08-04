import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.rimumarkup.RenderOptions
import org.rimumarkup.render

/**
 * Simplest Kotlin Rimu example.
 */
class KotlinExamplesTest() {

    @Before
    fun before() {
        // Initialize Rimu to default state.
        render("", RenderOptions(reset = true))
    }

    @Test
    fun simpleExampleTest() {
        val result = render("Hello *Rimu*!", RenderOptions(reset = true))
        assertEquals("<p>Hello <em>Rimu</em>!</p>", result)
    }

    @Test
    @Ignore
    fun callbackExampleTest() {
        val options = RenderOptions()
        var callbackMessage = ""
        options.callback = fun(message) { callbackMessage = "${message.type}: ${message.text}" } // Capture the callback message.
        val result = render("Unknown {x}", options)
        assertEquals("<p>Unknown {x}</p>", result)
        Assert.assertEquals("error: undefined macro: {x}: Unknown {x}", callbackMessage)
    }
}

