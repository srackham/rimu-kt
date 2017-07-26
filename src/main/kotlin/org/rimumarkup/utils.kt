package org.rimumarkup

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

// Global Block Attributes state singleton.
object BlockAttributes {
    var classes: String = ""     // Space separated HTML class names.
    var attributes: String = ""  // HTML element attributes (incorporates 'style' and 'id' attributes).
    var options: ExpansionOptions = ExpansionOptions()

    fun init() {
        classes = ""
        attributes = ""
        options = ExpansionOptions()
    }
}

// TODO: Should be a Map of <OptionEnum, Boolean> e.g option[OptionEnum.MACROS] = true
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

    // Inject HTML attributes from attrs into the opening tag and return result.
    // Consume HTML attributes unless the 'tag' argument is blank.
    fun injectHtmlAttributes(tag: String): String {
        var result = tag
        if (result.isBlank()) {
            return result
        }
        if (BlockAttributes.classes.isNotBlank()) {
            if (result.contains(Regex("""class="\S.*""""))) {
                // Inject class names into existing class attribute.
                result = result.replace(Regex("""class="(\S.*?)""""), """class="${BlockAttributes.classes} $1"""")
            } else {
                // Prepend new class attribute to HTML attributes.
                BlockAttributes.attributes = """class="${BlockAttributes.classes}" ${BlockAttributes.attributes}""".trim()
            }
        }
        if (BlockAttributes.attributes.isNotBlank()) {
            val match = Regex("""^<([a-zA-Z]+|h[1-6])(?=[ >])""").find(result)
            if (match != null) {
                // Inject attributes after tag name.
                val before = result.substring(0..match.groupValues[0].length - 1)
                val after = result.substring(match.groupValues[0].length)
                result = before + " " + BlockAttributes.attributes + after
            }
        }
        // Consume the attributes.
        BlockAttributes.classes = ""
        BlockAttributes.attributes = ""
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
fun readResouce(fileName: String): String {
    // The anonymous object provides a Java class to call getResouce() against.
    val url = object {}::class.java.getResource(fileName)
    if (url == null) {
        throw FileNotFoundException("Missing resource file: $fileName")
    }
    return url.readText()
}

