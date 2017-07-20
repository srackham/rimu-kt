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
    // The anonymous object is necessary to retrieve a Java class to call getResouce() against.
    val url = object {}::class.java.getResource(fileName)
    if (url === null) {
        throw FileNotFoundException("Missing resource file: $fileName")
    }
    return url.readText()
}

// TODO: Make this a String extension function.
fun replaceSpecialChars(s: String): String {
    return s.replace("&", "&amp;")
            .replace(">", "&gt;")
            .replace("<", "&lt;")
}