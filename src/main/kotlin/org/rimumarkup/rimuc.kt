/**
 * Rimu command-line compiler.
 */

package org.rimumarkup

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
        Rimuc(args)
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
 */
fun Rimuc(args: Array<String>) {

    // Helpers.
    fun die(message: String) {
        throw RimucException(message)
    }

    val argsList = Deque(args.toMutableList())
//    var safe_mode = 0
//    var html_replacement = ""
//    var styled = false
//    var styled_name = "classic"
//    var no_rimurc = false
    var lint = false
//
//    var source = ""
    var outfile = ""

    outer@
    while (!argsList.isEmpty()) {
        var arg = argsList.popFirst()
        when (arg) {
            "--help", "-h" -> {
                print(MANPAGE)
                return
            }
            "--lint", "-l" -> {
                lint = true
            }
            "--output", "-o" -> {
                if (argsList.isEmpty()) {
                    die("missing --output argument")
                }
                outfile = argsList.popFirst()
            }
            else -> {
                if (arg[0] == '-') {
                    die("illegal option: $arg")
                }
                argsList.pushFirst(arg); // argv contains source file names.
                break@outer
            }
        }
    }
    var html = ""
    if (argsList.isEmpty()) {
        html += render(System.`in`.readTextAndClose())
    } else {
        html += argsList.fold("") { total, next -> total + render(fileToString(next)) + "\n" }
    }
    html = html.trim()
    if (outfile.isBlank()) {
        print(html)
    } else {
        stringToFile(html, outfile)
    }
}
