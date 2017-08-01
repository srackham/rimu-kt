import org.junit.Test;
import org.rimumarkup.RenderOptions;
import org.rimumarkup.Rimu;

import static org.junit.Assert.assertEquals;

/**
 * Java Rimu examples.
 */
public class JavaExamplesTest {

    @Test
    public final void simpleExampleTest() {
        RenderOptions options = new RenderOptions();
        options.reset = true;
        String result = Rimu.render("Hello *Rimu*!", options);
        assertEquals("<p>Hello <em>Rimu</em>!</p>", result);
    }

    @Test
    public final void optionsExampleTest() {
        String result = Rimu.render("Hello <br>", new RenderOptions(2, "XXX", false, null));
        assertEquals("<p>Hello XXX</p>", result);
    }

    private String callbackMessage;

    @Test
    public final void callabckExampleTest() {
        RenderOptions options = new RenderOptions();
        // Capture the callback message.
        options.callback = (message) -> {
            // NOTE: Java does not support closures and Java lambdas cannot assign variables in the method scope at runtime,
            // which is why callbackMessage is outside this method.
            callbackMessage = message.type + ": " + message.text;
            return null;
        };
        String result = Rimu.render("{undefined-macro}", options);
        assertEquals("<p>{undefined-macro}</p>", result);
        assertEquals("error: undefined macro: {undefined-macro}: {undefined-macro}", callbackMessage);
    }
}
