package graphql.nadel.tests.util

import io.kotest.matchers.file.exist
import java.io.File
import java.io.FileNotFoundException

fun File.requireIsDirectory(): File = apply {
    if (!exists()) {
        FileNotFoundException("No file at $absolutePath")
    }
    if (!isDirectory) {
        FileNotFoundException("No directory at $absolutePath")
    }
}

fun File.getAncestorFile(name: String): File {
    var dir = this
    do { // Traverse up while ancestor does not match file name
        dir = dir.parentFile
    } while (dir.nameWithoutExtension != name)
    return dir
}
