package org.rimumarkup

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

// Global Block Attributes state singleton.
object BlockAttributes {
    var classes: String = ""     // Space separated HTML class names.
    var id: String = ""          // HTML element id.
    var css: String = ""         // HTML CSS styles.
    var attributes: String = ""  // Other HTML element attributes.
    var options: ExpansionOptions = ExpansionOptions()

    val ids = mutableListOf<String>() // List of allocated HTML ids.

    fun init() {
        classes = ""
        id = ""
        css = ""
        attributes = ""
        options = ExpansionOptions()
        ids.clear()
    }

    fun parse(match: MatchResult): Boolean {
        // Parse Block Attributes.
        // class names = $1, id = $2, css-properties = $3, html-attributes = $4, block-options = $5
        var text = match.groupValues[0]
        text = Utils.replaceInline(text, ExpansionOptions(macros = true))
        val m = Regex("""^\\?\.((?:\s*[a-zA-Z][\w\-]*)++)*+(?:\s+)?(#[a-zA-Z][\w\-]*\s*)?+(?:\s+)?(?:"(.+?)")?+(?:\s+)?(\[.+])?+(?:\s+)?([+-][ \w+-]+)?+$""").find(text)
        if (m == null) {
            return false
        }
        if (!Options.skipBlockAttributes()) {
            if (m.groupValues[1].isNotBlank()) { // HTML element class names.
                classes += " ${m.groupValues[1].trim()}"
                classes = classes.trim()
            }
            if (m.groupValues[2].isNotBlank()) { // HTML element id.
                id = m.groupValues[2].trim().substring(1)
            }
            if (m.groupValues[3].isNotBlank()) { // CSS properties.
                css = m.groupValues[3]
            }
            if (m.groupValues[4].isNotBlank() && !Options.isSafeModeNz()) { // HTML attributes.
                attributes += " " + m.groupValues[4].trim().removeSurrounding("[", "]")
                attributes = attributes.trim()
            }
            options.parse(m.groupValues[5])
        }
        return true
    }

    // Inject HTML attributes into the HTML `tag` and return result.
    // Consume HTML attributes unless the 'tag' argument is blank.
    fun injectHtmlAttributes(tag: String): String {
        var result = tag
        if (result.isBlank()) {
            return result
        }
        var attrs = ""
        if (classes.isNotBlank()) {
            if (result.contains(Regex("""class="\S.*""""))) {
                // Inject class names into existing class attribute.
                result = result.replace(Regex("""class="(\S.*?)""""), """class="${classes} $1"""")
            } else {
                attrs = """class="${classes}""""
            }
        }
        if (id.isNotBlank()) {
            attrs += """ id="${id}""""
        }
        if (css.isNotBlank()) {
            attrs += """ style="${css}""""
        }
        if (attributes.isNotBlank()) {
            attrs += """ ${attributes}"""
        }
        attrs = attrs.trim()
        if (attrs.isNotBlank()) {
            val match = Regex("""^<([a-zA-Z]+|h[1-6])(?=[ >])""").find(result)
            if (match != null) {
                // Inject attributes after tag name.
                val before = result.substring(0..match.groupValues[0].length - 1)
                val after = result.substring(match.groupValues[0].length)
                result = before + " " + attrs + after
            }
        }
        // Consume the attributes.
        classes = ""
        id = ""
        css = ""
        attributes = ""
        return result
    }

}

data class ExpansionOptions(
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

    // Parse block-options string into blockOptions.
    fun parse(options: String) {
        if (options.isNotBlank()) {
            val opts = options.trim().split(Regex("""\s+"""))
            for (opt in opts) {
                if (Options.isSafeModeNz() && opt == "-specials") {
                    Options.errorCallback("-specials block option not valid in safeMode")
                    continue
                }
                if (Regex("""^[+-](macros|spans|specials|container|skip)$""").matches(opt)) {
                    val value = opt[0] == '+'
                    when (opt.substring(1)) {
                        "macros" -> this.macros = value
                        "spans" -> this.spans = value
                        "specials" -> this.specials = value
                        "container" -> this.container = value
                        "skip" -> this.skip = value
                    }
                } else {
                    Options.errorCallback("illegal block option: " + opt)
                }
            }
        }
    }
}

// Utils "namespace" contains Rimu specific code ported from utils.js.
object Utils {

    // TODO: Make this a String extension function.
    fun replaceSpecialChars(s: String): String {
        return s.replace("&", "&amp;")
                .replace(">", "&gt;")
                .replace("<", "&lt;")
    }

    // Replace pattern '$1' or '$$1', '$2' or '$$2'... in `replacement` with corresponding match match group values.
    // If pattern starts with one '$' character add specials to `expansionOptions`,
    // if it starts with two '$' characters add spans to `expansionOptions`.
    fun replaceMatch(groupValues: List<String>,
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
            val text = groupValues[i]           // match group text.
            return replaceInline(text, expansionOptions)
        })
    }

    // Replace the inline elements specified in options in text and return the result.
    fun replaceInline(text: String, expansionOptions: ExpansionOptions): String {
        var result = text
        if (expansionOptions.macros ?: false) {
            result = Macros.render(result)
        }
        // Spans also expand special characters.
        if (expansionOptions.spans ?: false) {
            result = Spans.render(result)
        } else if (expansionOptions.specials ?: false) {
            result = replaceSpecialChars(result)
        }
        return result
    }

}


// Mutablelist push/pop extension functions.

/**
 * Prepends [element] to start of queue.
 */
fun <E> MutableList<E>.pushFirst(element: E) {
    this.add(0, element)
}

/**
 * Removes and returns first element.
 * @return first element.
 * @throws [NoSuchElementException] if queue is empty.
 */
fun <E> MutableList<E>.popFirst(): E {
    return this.removeAt(0)
}

/**
 * Appends [element] to end of queue.
 */
fun <E> MutableList<E>.pushLast(element: E) {
    this.add(element)
}

/**
 * Removes and returns last element.
 * @return last element.
 * @throws [NoSuchElementException] if queue is empty.
 */
fun <E> MutableList<E>.popLast(): E {
    return this.removeAt(this.size - 1)
}


// General purpose utility functions.

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
fun readResource(fileName: String): String {
    // The anonymous object provides a Java class to call getResouce() against.
    val url = object {}::class.java.getResource(fileName)
    if (url == null) {
        throw FileNotFoundException("Missing resource file: $fileName")
    }
    return url.readText()
}

