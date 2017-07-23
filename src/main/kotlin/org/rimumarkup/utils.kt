package org.rimumarkup

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

// Global Block Attributes state singleton.
object BlockAttributes {
    var classes: String = ""     // Space separated HTML class names.
    var attributes: String = ""  // HTML element attributes (incorporates 'style' and 'id' attributes).
    var options: Utils.ExpansionOptions = Utils.ExpansionOptions()

    fun init() {
        classes = ""
        attributes = ""
        options = Utils.ExpansionOptions()
    }
}

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
            var macros: Boolean? = null,
            var container: Boolean? = null,
            var skip: Boolean? = null,
            var spans: Boolean? = null, // Span substitution also expands special characters.
            var specials: Boolean? = null
    ) {
        fun merge(from: ExpansionOptions) {
            this.macros = from.macros ?: this.macros
            this.container = from.container ?: this.container
            this.skip = from.skip ?: this.skip
            this.spans = from.spans ?: this.spans
            this.specials = from.specials ?: this.specials
        }

    }

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
        if (expansionOptions.macros ?: false) {
//TODO: Uncomment once implemented.            
//            result = Macros.render(result)
//            result = result === null ? '' : result
        }
        // Spans also expand special characters.
        if (expansionOptions.spans ?: false) {
            result = Spans.render(result)
        } else if (expansionOptions.specials ?: false) {
            result = replaceSpecialChars(result)
        }
        return result
    }

    // Inject HTML attributes from attrs into the opening tag and return result.
// Consume HTML attributes unless the 'tag' argument is blank.
    fun injectHtmlAttributes(tag: String): String {
        if (tag.isBlank()) {
            return tag
        }
        var result = tag
        if (BlockAttributes.classes.isNotBlank()) {
            if (tag.matches(Regex("""class="\S.*""""))) {
                // Inject class names into existing class attribute.
                result = tag.replace(Regex("""class="(\S.*?)""""), """class="${BlockAttributes.classes} $1"""")
            } else {
                // Prepend new class attribute to HTML attributes.
                BlockAttributes.attributes = """class="${BlockAttributes.classes}" ${BlockAttributes.attributes}""".trim()
            }
        }
        if (BlockAttributes.attributes.isNotBlank()) {
            val match = Regex("""^<([a-zA-Z]+|h[1-6])(?=[ >])""").find(tag)
            if (match != null) {
                // Inject attributes after tag name.
                val before = tag.substring(0..match.groupValues[0].length - 1)
                val after = tag.substring(match.groupValues[0].length)
                result = before + " " + BlockAttributes.attributes + after
            }
        }
        // Consume the attributes.
        BlockAttributes.classes = ""
        BlockAttributes.attributes = ""
        return result
    }

}
