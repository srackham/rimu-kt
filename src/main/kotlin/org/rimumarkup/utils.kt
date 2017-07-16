package org.rimumarkup

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
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


