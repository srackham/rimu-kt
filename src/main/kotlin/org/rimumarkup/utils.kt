package org.rimumarkup

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

fun OutputStream.writeTextAndClose(text: String, charset: Charset = Charsets.UTF_8) {
    this.bufferedWriter(charset).use { it.write(text) }
}

fun fileToString(fileName: String): String {
    return FileInputStream(fileName).readTextAndClose()
}

fun stringToFile(text: String, fileName: String) {
    Files.deleteIfExists(Paths.get(fileName))
    return FileOutputStream(fileName).writeTextAndClose(text)
}

/**
 * Read contents of resource file.
 * @throws [FileNotFoundException] if resource file is missing.
 */
fun readResouce(fileName: String): String {
    // The anonymous object provides a Java class to call getResouce() against.
    val url = object {}::class.java.getResource(fileName)
    if (url === null) {
        throw FileNotFoundException("Missing resource file: $fileName")
    }
    return url.readText()
}

// Utils "namespace" contains Rimu specific code ported from utils.js.
object Utils {

    data class ExpansionOptions(
            //TODO: What's this for?
//        [key: string]: boolean | undefined

            // Processing priority (highest to lowest): container, skip, spans and specials.
            // If spans is true then both spans and specials are processed.
            // They are assumed false if they are not explicitly defined.
            // If a custom filter is specified their use depends on the filter.
            val macros: Boolean = false,
            var container: Boolean = false,
            var skip: Boolean = false,
            var spans: Boolean = false, // Span substitution also expands special characters.
            var specials: Boolean = false
    )

    // TODO: Make this a String extension function.
    fun replaceSpecialChars(s: String): String {
        return s.replace("&", "&amp;")
                .replace(">", "&gt;")
                .replace("<", "&lt;")
    }

    // Replace pattern '$1' or '$$1', '$2' or '$$2'... in `replacement` with corresponding match groups
// from `match`. If pattern starts with one '$' character add specials to `expansionOptions`,
// if it starts with two '$' characters add spans to `expansionOptions`.
    fun replaceMatch(match: MatchResult,
                     replacement: String,
                     expansionOptions: ExpansionOptions = ExpansionOptions()
    ): String {
        return replacement.replace(Regex("(\\\${1,2})(\\d)"), fun(mr: MatchResult): String {
            // Replace $1, $2 ... with corresponding match groups.
            if (mr.groupValues[1] == "$$") {
                expansionOptions.spans = true
            } else {
                expansionOptions.specials = true
            }
            val i = mr.groupValues[2].toInt()  // match group number.
            val text = match.groupValues[i]           // match group text.
            return replaceInline(text, expansionOptions)
        })
    }

    // Replace the inline elements specified in options in text and return the result.
    fun replaceInline(text: String, expansionOptions: ExpansionOptions): String {
        var result = text
        if (expansionOptions.macros) {
//TODO: Uncomment once implemented.            
//            result = Macros.render(result)
//            result = result === null ? '' : result
        }
        // Spans also expand special characters.
        if (expansionOptions.spans) {
            result = Spans.render(result)
        } else if (expansionOptions.specials) {
            result = replaceSpecialChars(result)
        }
        return result
    }

}
