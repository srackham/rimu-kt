import org.junit.Before;
import org.junit.Test;
import org.rimumarkup.CallbackMessage;
import org.rimumarkup.RenderOptions;
import org.rimumarkup.Rimu;

import static org.junit.Assert.assertEquals;

/**
 * Java Rimu examples.
 */
public class JavaExamplesTest {

    @Before
    public void before() {
        // Initialize Rimu to default state.
        RenderOptions options = new RenderOptions();
        options.reset = true;
        Rimu.render("", options);
    }

    @Test
    public final void simpleExampleTest() {
        String result = Rimu.render("Hello *Rimu*!");
        assertEquals("<p>Hello <em>Rimu</em>!</p>", result);
    }

    private String callbackMessage;

    @Test
    public final void callbackExampleTest() {
        RenderOptions options = new RenderOptions();
        // Capture the callback message.
        options.callback = (CallbackMessage message) -> {
            // NOTE: Java does not support closures and Java lambdas cannot assign variables in the method scope at runtime,
            // which is why callbackMessage is outside this method.
            callbackMessage = message.type + ": " + message.text;
            return null;
        };
        String result = Rimu.render("Unknown {x}", options);
        assertEquals("<p>Unknown {x}</p>", result);
        assertEquals("error: undefined macro: {x}: Unknown {x}", callbackMessage);
    }
}
