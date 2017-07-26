package org.rimumarkup

/*
 This module renders inline text containing Quote and Replacement elements.

 Quote and replacement processing involves splitting the source text into
 fragments where at the points where quotes and replacements occur then splicing fragments
 containing output markup into the breaks. A fragment is flagged as 'done' to
 exclude it from further processing.

 Once all quotes and replacements are processed fragments not yet flagged as
 'done' have special characters (&, <, >) replaced with corresponding special
 character entities. The fragments are then reassembled (defraged) into a
 resultant HTML string.
 */
object Spans {

    data class Fragment(
            var text: String,
            val done: Boolean,
            val verbatim: String = ""   // Replacements text rendered verbatim.
    )

    fun render(source: String): String {
        var result: String
        result = preReplacements(source)
        var fragments = mutableListOf<Fragment>(Fragment(text = result, done = false))
        fragments = fragQuotes(fragments).toMutableList()
        fragSpecials(fragments)
        result = defrag(fragments)
        return postReplacements(result)
    }

    // Converts fragments to a string.
    fun defrag(fragments: List<Fragment>): String {
        return fragments.fold("") {
            result, fragment ->
            result + fragment.text
        }
    }

    // Fragment quotes in all fragments and return resulting fragments list.
    fun fragQuotes(fragments: List<Fragment>): List<Fragment> {
        val result = mutableListOf<Fragment>()
        for (fragment in fragments) {
            result.addAll(fragQuote(fragment))
        }
        // Strip backlash from escaped quotes in non-done fragments.
        for (fragment in result) {
            if (!fragment.done) fragment.text = Quotes.unescape(fragment.text)
        }
        return result
    }

    // Fragment quotes in a single fragment and return resulting fragments array.
    fun fragQuote(fragment: Fragment): List<Fragment> {
        if (fragment.done) {
            return listOf(fragment)
        }
        val quotesRe = Quotes.quotesRe
        var match: MatchResult?
        var quote: String
        var nextIndex = 0
        while (true) {
            match = quotesRe.find(fragment.text, nextIndex)
            if (match == null) {
                return listOf(fragment)
            }
            quote = match.groupValues[1]
            // Check if quote is escaped.
            if (match.value.startsWith('\\')) {
                // Restart search after escaped opening quote.
                nextIndex = match.range.first + quote.length + 1
                continue
            }
            nextIndex = match.range.last + 1
            break
        }
        match!!
        val result = mutableListOf<Fragment>()
        // Arrive here if we have a matched quote.
        // The quote splits the input fragment into 5 or more output fragments:
        // Text before the quote, left quote tag, quoted text, right quote tag and text after the quote.
        val def = Quotes.getDefinition(quote)!!
        // Check for same closing quote one character further to the right.
        var quoted = match.groupValues[2]
        while (nextIndex < fragment.text.length && fragment.text[nextIndex] == quote[0]) {
            // Move to closing quote one character to right.
            quoted += quote[0]
            nextIndex += 1
        }
        val before = fragment.text.substring(0..match.range.first - 1)
        val after = fragment.text.substring(nextIndex)
        result.add(Fragment(text = before, done = false))
        result.add(Fragment(text = def.openTag, done = true))
        if (!def.spans) {
            // Spans are disabled so render the quoted text verbatim.
            quoted = Utils.replaceSpecialChars(quoted)
            quoted = quoted.replace('\u0000', '\u0001')   // Substitute verbatim replacement placeholder.
            result.add(Fragment(text = quoted, done = true))
        } else {
            // Recursively process the quoted text.
            result.addAll(fragQuote(Fragment(text = quoted, done = false)))
        }
        result.add(Fragment(text = def.closeTag, done = true))
        // Recursively process the following text.
        result.addAll(fragQuote(Fragment(text = after, done = false)))
        return result
    }

    // Stores placeholder replacement fragments saved by `preReplacements()` and restored by `postReplacements()`.
    val savedReplacements = mutableListOf<Fragment>()

    // Return text with replacements replaced with placeholders (see `postReplacements()`).
    fun preReplacements(text: String): String {
        savedReplacements.clear()
        val fragments = fragReplacements(listOf<Fragment>(Fragment(text = text, done = false)))
        // Reassemble text with replacement placeholders.
        return fragments.fold("", { result, fragment ->
            if (fragment.done) {
                savedReplacements.add(fragment)  // Save replaced text.
                result + '\u0000'          // Placeholder for replaced text.
            } else {
                result + fragment.text
            }
        })
    }

    // Replace replacements placeholders with replacements text from savedReplacements[].
    fun postReplacements(text: String): String {
        return text.replace(Regex("""[\u0000\u0001]"""), { match ->
            val fragment = savedReplacements.removeAt(0)
            if (match.value == "\u0000")
                fragment.text
            else
                Utils.replaceSpecialChars(fragment.verbatim)
        })
    }

    // Fragment replacements in all fragments and return resulting fragments array.
    fun fragReplacements(fragments: List<Fragment>): List<Fragment> {
        var result = fragments.toList()
        val tmp = mutableListOf<Fragment>()
        Replacements.defs.forEach({ def ->
            tmp.clear()
            result.forEach({ fragment ->
                tmp.addAll(fragReplacement(fragment, def))
            })
            result = tmp.toList()
        })
        return result
    }

    // Fragment replacements in a single fragment for a single replacement definition.
    // Return resulting fragments list.
    fun fragReplacement(fragment: Fragment, def: Replacements.Definition): List<Fragment> {
        if (fragment.done) {
            return listOf(fragment)
        }
        val match = def.match.find(fragment.text)
        if (match == null) {
            return listOf(fragment)
        }
        // Arrive here if we have a matched replacement.
        // The replacement splits the input fragment into 3 output fragments:
        // Text before the replacement, replaced text and text after the replacement.
        val before: String = fragment.text.substring(0..match.range.first - 1)
        val after: String = fragment.text.substring(match.range.last + 1)
        val result = mutableListOf<Fragment>()
        result.add(Fragment(text = before, done = false))
        val replacement: String
        if (match.value.startsWith('\\')) {
            // Remove leading backslash.
            replacement = Utils.replaceSpecialChars(match.value.removeRange(0..0))
        } else {
            if (def.filter == null) {
                replacement = Utils.replaceMatch(match.groupValues, def.replacement)
            } else {
                replacement = (def.filter)(match, def)
            }
        }
        result.add(Fragment(text = replacement, done = true, verbatim = match.value))
        // Recursively process the remaining text.
        result.addAll(fragReplacement(Fragment(text = after, done = false), def))
        return result
    }

    fun fragSpecials(fragments: List<Fragment>) {
        // Replace special characters in all non-done fragments.
        for (fragment in fragments) {
            if (!fragment.done) fragment.text = Utils.replaceSpecialChars(fragment.text)
        }
    }

}
