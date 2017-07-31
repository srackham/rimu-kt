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
  then its contents is processed (with --safe-mode 0) after
  --prepend sources but before any other inputs.
  This behavior can be disabled with the --no-rimurc option.

OPTIONS
  -h, --help
    Display help message.

  -l, --lint
    Check the Rimu source for inconsistencies and errors.

  -o, --output OUTFILE
    Write output to file OUTFILE instead of stdout.
    If OUTFILE is a hyphen '-' write to stdout.

  -p, --prepend SOURCE
    Process the SOURCE text before other inputs.
    Rendered with --safe-mode 0.

  --no-rimurc
    Do not process .rimurc from the user's home directory.

  -s, --styled
    Include an HTML header and footer for styling the HTML output
    document. If only one source file is specified and the --output
    option is not specified then the output is written to a
    same-named file with an .html extension.

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
  --sidebar-toc, --dropdown-toc, --custom-toc, --section-numbers
    Shortcuts for the following prepended macro definitions:
    --prepend "{--theme}='THEME'"
    --prepend "{--lang}='LANG'"
    --prepend "{--title}='TITLE'"
    --prepend "{--highlightjs}='true'"
    --prepend "{--mathjax}='true'"
    --prepend "{--sidebar-toc}='true'"
    --prepend "{--dropdown-toc}='true'"
    --prepend "{--custom-toc}='true'"
    --prepend "{--section-numbers}='true'"

  --styled-name NAME
    Specify the --styled option header and footer files:
    'classic': Default styling.
    'flex':    Flexbox "mobile first" styling (experimental).
    'v8':      Rimu version 8 styling.

STYLING MACROS AND CLASSES
  The following macros and CSS classes are available when the
  --styled option is used:

  Macro name         Description
  _______________________________________________________________
  --                 Blank macro (empty string).
  --theme            Set styling themes.
                     Theme names: default, graystone.
  --lang             HTML document language attribute value.
  --title            HTML document title.
  --highlightjs      Set to non-blank value to enable syntax
                     highlighting with Highlight.js.
  --mathjax          Set to a non-blank value to enable MathJax.
  --sidebar-toc      Set to a non-blank value to generate a
                     table of contents sidebar.
  --dropdown-toc     Set to a non-blank value to generate a
                     table of contents dropdown menu.
  --custom-toc       Set to a non-blank value if a custom table
                     of contents is used.
  --section-numbers  Apply h2 and h3 section numbering.
  _______________________________________________________________
  These macros must be defined prior to processing (using rimuc
  options or in .rimurc).

  CSS class        Description
  ______________________________________________________________
  verse            Verse format (paragraphs, division blocks).
  sidebar          Sidebar format (paragraphs, division blocks).
  cite             Quote and verse attribution.
  bordered         Add borders to table.
  align-left       Text alignment left.
  align-center     Text alignment center.
  align-right      Text alignment right.
  no-print         Do not print.
  line-breaks      Honor line breaks in source text.
  page-break       Force page break before the element.
  no-page-break    Avoid page break inside the element.
  dl-numbered      Number labeled list items.
  dl-horizontal    Format labeled lists horizontally.
  dl-counter       Prepend dl item counter to element content.
  ol-counter       Prepend ol item counter to element content.
  ul-counter       Prepend ul item counter to element content.
  ______________________________________________________________
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
    } catch(e: RimucException) {
        System.err.println(e.message)
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
    var styled = false
    var styled_name = "classic"
    var no_rimurc = false
    var lint = false
    var source = ""
    var outfile = ""

    // Helpers.
    fun die(message: String) {
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
            "--lint", "-l" -> {
                lint = true
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
            "--safeMode"    /*DEPRECATED*/ -> {
                if (argsList.isEmpty()) {
                    die("missing --safe-mode argument")
                }
                safe_mode = popOptionValue(arg).toInt()
                if (safe_mode !in 0..15) {
                    die("illegal --safe-mode option value: $safe_mode")
                }
            }
            "--html-replacement",
            "--htmlReplacement" /*DEPRECATED*/ -> {
                html_replacement = popOptionValue(arg)
            }
            "--styled", "-s" -> {
                styled = true

            }
        // Styling macro definitions shortcut options.
            "--highlightjs",
            "--mathjax",
            "--section-numbers",
            "--theme",
            "--title",
            "--lang",
            "--toc", // DEPRECATED
            "--sidebar-toc",
            "--dropdown-toc",
            "--custom-toc" -> {
                val macro_value = if (arrayOf("--lang", "--title", "--theme").contains(arg))
                    popOptionValue(arg)
                else
                    "true"
                source += "{$arg}='$macro_value'\n"
            }
            "--styled-name" -> {
                styled_name = popOptionValue(arg)
                if (!arrayOf("classic", "flex", "v8").contains(styled_name)) {
                    die("illegal --styled-name: $styled_name")
                }
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
    if (styled && outfile.isEmpty() && infiles.size == 1) {
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
    if (styled) {
        // Envelope source files with header and footer resource file names.
        infiles.pushFirst("resource:/${styled_name}-header.rmu")
        infiles.pushLast("resource:/${styled_name}-footer.rmu")
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
    val options = Options.RenderOptions()
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
        if (lint) {
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
        System.exit(1)
    }
}
