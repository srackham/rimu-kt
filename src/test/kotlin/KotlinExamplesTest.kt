import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.rimumarkup.RenderOptions
import org.rimumarkup.render

/**
 * Simplest Kotlin Rimu example.
 */
class KotlinExamplesTest() {

    @Test
    fun simpleExampleTest() {
        val result = render("Hello *Rimu*!", RenderOptions(reset = true))
        assertEquals("<p>Hello <em>Rimu</em>!</p>", result)
    }

    @Test
    fun optionsExampleTest() {
        val result = render("Hello <br>", RenderOptions(safeMode = 2, htmlReplacement = "XXX"))
        assertEquals("<p>Hello XXX</p>", result)
    }

    @Test
    @Ignore
    fun callbackExampleTest() {
        val options = RenderOptions()
        var callbackMessage = ""
        options.callback = fun(message) { callbackMessage = "${message.type}: ${message.text}" } // Capture the callback message.
        val result = render("{undefined-macro}", options)
        assertEquals("<p>{undefined-macro}</p>", result)
        Assert.assertEquals("error: undefined macro: {undefined-macro}: {undefined-macro}", callbackMessage)
    }
}

