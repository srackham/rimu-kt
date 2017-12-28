/**
 * Rimu command-line compiler.
 */

package org.rimumarkup

import java.nio.file.Files
import java.nio.file.Paths

val MANPAGE = """
NAME
  rimuc - convert Rimu source to HTML

SYNOPSIS
  rimuc [OPTIONS...] [FILES...]

DESCRIPTION
  Reads Rimu source markup from stdin, converts them to HTML
  then writes the HTML to stdout. If FILES are specified
  the Rimu source is read from FILES. The contents of files
  with an .html extension are passed directly to the output.

  If a file named .rimurc exists in the user's home directory
  then its contents is processed (with --safe-mode 0), this
  happens after --prepend processing but prior to other inputs.
  This behavior can be disabled with the --no-rimurc option.

OPTIONS
  -h, --help
    Display help message.

  --layout LAYOUT
    Generate a styled HTML document with one of the following
    predefined document layouts:

    'classic': Desktop-centric layout.
    'flex':    Flexbox mobile layout (experimental).
    'sequel':  Responsive cross-device layout.

    If only one source file is specified and the --output
    option is not specified then the output is written to a
    same-named file with an .html extension.
    This option enables --header-ids.

  -o, --output OUTFILE
    Write output to file OUTFILE instead of stdout.
    If OUTFILE is a hyphen '-' write to stdout.

  -p, --prepend SOURCE
    Process the SOURCE text before other inputs.
    Rendered with --safe-mode 0.

  --no-rimurc
    Do not process .rimurc from the user's home directory.

  --safe-mode NUMBER
    Non-zero safe modes ignore: Definition elements; API option elements;
    HTML attributes in Block Attributes elements.
    Also specifies how to process HTML elements:

    --safe-mode 0 renders HTML (default).
    --safe-mode 1 ignores HTML.
    --safe-mode 2 replaces HTML with --html-replacement option value.
    --safe-mode 3 renders HTML as text.

    Add 4 to --safe-mode to ignore Block Attribute elements.
    Add 8 to --safe-mode to allow Macro Definitions.

  --html-replacement TEXT
    Embedded HTML is replaced by TEXT when --safe-mode is set to 2.
    Defaults to '<mark>replaced HTML</mark>'.

  --theme THEME, --lang LANG, --title TITLE, --highlightjs, --mathjax,
  --no-toc, --custom-toc, --section-numbers, --header-ids, --header-links
    Shortcuts for the following prepended macro definitions:

    --prepend "{--custom-toc}='true'"
    --prepend "{--header-ids}='true'"
    --prepend "{--header-links}='true'"
    --prepend "{--highlightjs}='true'"
    --prepend "{--lang}='LANG'"
    --prepend "{--mathjax}='true'"
    --prepend "{--no-toc}='true'"
    --prepend "{--section-numbers}='true'"
    --prepend "{--theme}='THEME'"
    --prepend "{--title}='TITLE'"

LAYOUT OPTIONS
  The following options are available when the --layout option is
  used:

  Option             Description
  _______________________________________________________________
  --custom-toc       Set to a non-blank value if a custom table
                     of contents is used.
  --header-links     Set to a non-blank value to generate h2 and
                     h3 header header links.
  --highlightjs      Set to non-blank value to enable syntax
                     highlighting with Highlight.js.
  --lang             HTML document language attribute value.
  --mathjax          Set to a non-blank value to enable MathJax.
  --no-toc           Set to a non-blank value to suppress table of
                     contents generation.
  --section-numbers  Apply h2 and h3 section numbering.
  --theme            Styling theme. Theme names:
                     'legend', 'graystone', 'vintage'.
  --title            HTML document title.
  _______________________________________________________________
  These options are translated by rimuc to corresponding layout
  macro definitions using the --prepend option.

LAYOUT CLASSES
  The following CSS classes are available for use in Rimu Block
  Attributes elements when the --layout option is used:

  CSS class        Description
  ______________________________________________________________
  align-center     Text alignment center.
  align-left       Text alignment left.
  align-right      Text alignment right.
  bordered         Adds table borders.
  cite             Quote and verse attribution.
  dl-horizontal    Format labeled lists horizontally.
  dl-numbered      Number labeled list items.
  dl-counter       Prepend dl item counter to element content.
  ol-counter       Prepend ol item counter to element content.
  ul-counter       Prepend ul item counter to element content.
  no-auto-toc      Exclude heading from table of contents.
  no-page-break    Avoid page break inside the element.
  no-print         Do not print.
  page-break       Force page break before the element.
  preserve-breaks  Honor line breaks in source text.
  sidebar          Sidebar format (paragraphs, division blocks).
  verse            Verse format (paragraphs, division blocks).
  ______________________________________________________________

PREDEFINED MACROS
  Macro name         Description
  _______________________________________________________________
  --                 Blank macro (empty string).
                     The Blank macro cannot be redefined.
  --header-ids       Set to a non-blank value to generate h1, h2
                     and h3 header id attributes.
  _______________________________________________________________
"""

/**
 * Thrown by the Rimu compiler on encountering illegal command options or missing inputs.
 */
class RimucException(message: String) : Exception(message)

/**
 * Main wrapper to handle execeptions and set system exit code.
 */
fun main(args: Array<String>) {
    try {
        rimuc(args)
    } catch (e: RimucException) {
        if (!e.message.isNullOrBlank()) {
            System.err.println(e.message)
        }
        System.exit(1)
    } catch (e: Exception) {
        System.err.println("${e::class.java.name}: ${e.message}")
        System.exit(2)
    }
    System.exit(0)
}

/**
 * Rimu command-line compiler.
 * Resides in org.rimumarkup.rimuc package.
 */
fun rimuc(args: Array<String>) {

    val argsList = args.toMutableList()

    // Command option values.
    var safe_mode = 0
    var html_replacement: String? = null
    var layout = ""
    var no_rimurc = false
    var source = ""
    var outfile = ""

    // Helpers.
    fun die(message: String = "") {
        throw RimucException(message)
    }

    fun popOptionValue(arg: String): String {
        if (argsList.isEmpty()) {
            die("missing $arg option value")
        }
        return argsList.popFirst()
    }

    outer@
    while (!argsList.isEmpty()) {
        val arg = argsList.popFirst()
        when (arg) {
            "--help", "-h" -> {
                print(MANPAGE)
                return
            }
            "--lint", "-l" -> { // Deprecated in Rimu 10.0.0
            }
            "--output", "-o" -> {
                outfile = popOptionValue(arg)
            }
            "--prepend", "-p" -> {
                source += popOptionValue(arg) + "\n"
            }
            "--no-rimurc" -> {
                no_rimurc = true
            }
            "--safe-mode",
            "--safeMode" -> { // Deprecated in Rimu 7.1.0
                if (argsList.isEmpty()) {
                    die("missing --safe-mode argument")
                }
                safe_mode = popOptionValue(arg).toInt()
                if (safe_mode !in 0..15) {
                    die("illegal --safe-mode option value: $safe_mode")
                }
            }
            "--html-replacement",
            "--htmlReplacement" -> { // Deprecated in Rimu 7.1.0
                html_replacement = popOptionValue(arg)
            }
            "--styled", "-s" -> {   // Deprecated in Rimu 10.0.0
                source += "{--header-ids}='true'\n"
                if (layout === "") {
                    layout = "classic"
                }
            }
        // Styling macro definitions shortcut options.
            "--highlightjs",
            "--mathjax",
            "--section-numbers",
            "--theme",
            "--title",
            "--lang",
            "--toc", // Deprecated in Rimu 8.0.0
            "--no-toc",
            "--sidebar-toc", // Deprecated in Rimu 10.0.0
            "--dropdown-toc", // Deprecated in Rimu 10.0.0
            "--custom-toc",
            "--header-ids",
            "--header-links" -> {
                val macro_value = if (arrayOf("--lang", "--title", "--theme").contains(arg))
                    popOptionValue(arg)
                else
                    "true"
                source += "{$arg}='$macro_value'\n"
            }
            "--layout",
            "--styled-name" -> { // Deprecated in Rimu 10.0.0
                layout = popOptionValue(arg)
                if (!arrayOf("classic", "flex", "sequel", "v8").contains(layout)) {
                    die("illegal --layout: $layout")
                }
                source += "{--header-ids}='true'\n"
            }
            else -> {
                if (arg[0] == '-') {
                    die("illegal option: $arg")
                }
                argsList.pushFirst(arg) // Contains source file names.
                break@outer
            }
        }
    }
    val infiles = argsList // argsList contains the list of source files.
    if (layout.isNotBlank() && outfile.isEmpty() && infiles.size == 1) {
        // Use the source file name with .html extension for the output file.
        val infile = infiles[0]
        outfile = if ('.' in infile) {
            infile.replaceAfterLast('.', "html")
        } else {
            "$infile.html"
        }
    }
    if (infiles.isEmpty()) {
        infiles.pushFirst("/dev/stdin")
    }
    if (layout.isNotBlank()) {
        // Envelope source files with header and footer resource file names.
        infiles.pushFirst("resource:${layout}-header.rmu")
        infiles.pushLast("resource:${layout}-footer.rmu")
    }
    // Include .rimurc file if it exists.
    val rimurc = Paths.get(System.getProperty("user.home"), ".rimurc")
    if (!no_rimurc) {
        if (Files.exists(rimurc)) {
            infiles.pushFirst(rimurc.toString())
        }
    }
    var html = ""
    Api.init()
    // Start by processing --prepend options source.
    if (source.isNotBlank()) {
        html = render(source) + '\n'
    }
    var errors = 0
    val options = RenderOptions()
    if (html_replacement != null) {
        options.htmlReplacement = html_replacement
    }
    for (infile in infiles) {
        val text = if (infile.startsWith("resource:"))
            readResource(infile.removePrefix("resource:"))
        else if (infile == "/dev/stdin")
            System.`in`.readTextAndClose()
        else
            fileToString(infile)
        if (infile.endsWith(".html")) {
            html += "$text\n"
            continue
        }
        // rimurc and resouces trusted with safeMode.
        options.safeMode = if (infile == rimurc.toString() || infile.startsWith("resouce:")) 0 else safe_mode
        options.callback = fun(message: CallbackMessage) {
            var s = "${message.type}: $infile: ${message.text}"
            if (s.length > 120) {
                s = s.substring(0..116) + "..."
            }
            System.err.println(s)
            if (message.type == "error") {
                errors += 1
            }
        }
        html += render(text, options) + "\n"
    }
    html = html.trim()
    if (outfile.isEmpty()) {
        print(html)
    } else {
        stringToFile(html, outfile)
    }
    if (errors != 0) {
        die()
    }
}
