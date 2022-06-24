/**
 * Rimu command-line compiler.
 */

package org.rimumarkup

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

val VERSION = "11.1.5"

/**
 * Thrown by the Rimu compiler on encountering illegal command options or missing inputs.
 */
class RimucException(message: String) : Exception(message)

/**
 * Main wrapper to handle execeptions and set system exit code.
 */
fun main(args: Array<String>) {
    try {
        rimukt(args)
    } catch (e: Exception) {
        System.exit(1)
    }
    System.exit(0)
}

/**
 * Rimu command-line compiler.
 * Resides in org.rimumarkup.rimukt package.
 */
fun rimukt(args: Array<String>) {
    try {
        val RESOURCE_TAG = "resource:" // Tag for resource files.
        val PREPEND = "--prepend options"
        val STDIN = "-"

        val argsList = args.toMutableList()

        // Command option values.
        var safe_mode = 0
        var html_replacement: String? = null
        var layout = ""
        var no_rimurc = false
        val prepend_files: MutableList<String> = mutableListOf()
        var pass = false

        // Helpers.
        fun die(message: String = "") {
            if (message.isNotBlank()) {
                System.err.println(message)
            }
            throw RimucException(message)
        }

        fun popOptionValue(arg: String): String {
            if (argsList.isEmpty()) {
                die("missing $arg option value")
            }
            return argsList.popFirst()
        }

        // Parse command-line options.
        var prepend = ""
        var outfile = ""
        outer@
        while (!argsList.isEmpty()) {
            val arg = argsList.popFirst()
            when (arg) {
                "--help", "-h" -> {
                    print("\n" + readResource("manpage.txt") + "\n")
                    return
                }
                "--version" -> {
                    print(VERSION + "\n")
                    return
                }
                "--lint", "-l" -> { // Deprecated in Rimu 10.0.0
                }
                "--output", "-o" -> {
                    outfile = popOptionValue(arg)
                }
                "--pass" -> {
                    pass = true
                }
                "--prepend", "-p" -> {
                    prepend += popOptionValue(arg) + "\n"
                }
                "--prepend-file" -> {
                    val prepend_file = popOptionValue(arg)
                    prepend_files.pushLast(prepend_file)
                }
                "--no-rimurc" -> {
                    no_rimurc = true
                }
                "--safe-mode",
                "--safeMode" -> { // Deprecated in Rimu 7.1.0
                    safe_mode = popOptionValue(arg).toInt()
                    if (safe_mode !in 0..15) {
                        die("illegal --safe-mode option value: $safe_mode")
                    }
                }
                "--html-replacement",
                "--htmlReplacement" -> { // Deprecated in Rimu 7.1.0
                    html_replacement = popOptionValue(arg)
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
                    prepend += "{$arg}='$macro_value'\n"
                }
                "--layout",
                "--styled-name" -> { // Deprecated in Rimu 10.0.0
                    layout = popOptionValue(arg)
                    if (!arrayOf("classic", "flex", "plain", "sequel", "v8").contains(layout)) {
                        die("illegal --layout: $layout")    // NOTE: Imported layouts are not supported.
                    }
                    prepend += "{--header-ids}='true'\n"
                }
                "--styled", "-s" -> {
                    prepend += "{--header-ids}='true'\n"
                    prepend += "{--no-toc}='true'\n"
                    layout = "sequel"
                }
                else -> {
                    argsList.pushFirst(arg) // Contains source file names.
                    break@outer
                }
            }
        }
        if (argsList.isEmpty()) {
            argsList.pushFirst(STDIN)
        }
        if (argsList.size == 1 && layout.isNotBlank() && argsList[0] != "-" && outfile.isEmpty()) {
            // Use the source file name with .html extension for the output file.
            val infile = argsList[0]
            outfile = if ('.' in infile) {
                infile.replaceAfterLast('.', "html")
            } else {
                "$infile.html"
            }
        }
        if (layout.isNotBlank()) {
            // Envelope source files with header and footer resource file names.
            argsList.pushFirst("${RESOURCE_TAG}${layout}-header.rmu")
            argsList.pushLast("${RESOURCE_TAG}${layout}-footer.rmu")
        }
        // Prepend $HOME/.rimurc file if it exists.
        val RIMURC = Paths.get(System.getProperty("user.home"), ".rimurc")
        if (!no_rimurc && Files.exists(RIMURC)) {
            prepend_files.pushFirst(RIMURC.toString())
        }
        if (prepend != "") {
            prepend_files.pushLast(PREPEND)
        }
        for (f in prepend_files.reversed()) argsList.pushFirst(f)    // Prepend infiles with prepend_files.
        // Convert Rimu source files to HTML.
        var output = ""
        Api.init()
        var errors = 0
        val options = RenderOptions()
        if (html_replacement != null) {
            options.htmlReplacement = html_replacement
        }
        for (infile in argsList) {
            var source = when {
                infile.startsWith(RESOURCE_TAG) -> readResource(infile.removePrefix(RESOURCE_TAG))
                infile == STDIN -> System.`in`.readTextAndClose()
                infile == PREPEND -> prepend
                else -> {
                    if (!File(infile).exists()) {
                        die("source file does not exist: " + infile)
                    }
                    fileToString(infile)
                }
            }
            if (!(infile.endsWith(".html") || (pass && infile === STDIN))) {
                // resources, prepended source and prepended files (including rimurc) are trusted with safeMode.
                options.safeMode =
                    if (infile.startsWith(RESOURCE_TAG) || infile == PREPEND || prepend_files.contains(infile)) 0 else safe_mode
                options.callback = fun(message: CallbackMessage) {
                    var s = "${message.type}: ${if (infile == STDIN) "/dev/stdin" else infile}: ${message.text}"
                    if (s.length > 120) {
                        s = s.substring(0..116) + "..."
                    }
                    System.err.println(s)
                    if (message.type == "error") {
                        errors += 1
                    }
                }
                source = render(source, options) + "\n"
            }
            source = source.trim()
            if (source != "") {
                output += source + "\n"
            }
        }
        output = output.trim()
        if (outfile.isEmpty() || outfile == "-") {
            print(output)
        } else {
            stringToFile(output, outfile)
        }
        if (errors != 0) {
            die()
        }
    } catch (e: Exception) {
        if (e !is RimucException) {
            System.err.println("${e::class.java.name}: ${e.message}")
        }
        throw e
    }
}
