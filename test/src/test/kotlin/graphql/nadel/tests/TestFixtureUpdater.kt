package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.engine.transform.result.json.JsonNodePath
import graphql.nadel.engine.transform.result.json.JsonNodePathSegment
import java.io.File

// For autocomplete navigation
private typealias TestFixtureUpdater = Unit

fun main() {
    val path = ClassLoader.getSystemClassLoader().getResource("")!!.path
    val projectDir = path.substringBeforeLast("test/build/")

    val fixturesDir = File(projectDir, "test/src/test/resources/fixtures")

    update(fixturesDir)
}

private data class Comment(
    val data: String,
    val position: JsonNodePath,
)

private fun update(fixturesDir: File) {
    val yamlExtensions = setOf("yaml", "yml")

    fixturesDir.walkTopDown()
        .filter {
            it.extension in yamlExtensions
        }
        // .take(1) // delet
        .forEach { file ->
            val cleanFileContents = file
                .readLines()
                .joinToString(separator = "\n", transform = String::trimEnd)

            val testFixture = yamlObjectMapper.readValue<TestFixture>(cleanFileContents)
            update(file, testFixture)
        }
}

fun update(fixtureFile: File, testFixture: TestFixture) {
    fixtureFile.writeText(yamlObjectMapper.writeValueAsString(testFixture))

    // val position = mutableListOf<JsonNodePathSegment<*>>()
    // var isLineBlockComment = false
    // var isNextLineBlockComment = false
    //
    // fixtureFile.readLines()
    //     .asSequence()
    //     .filter(String::isNotBlank)
    //     .map(String::trimEnd)
    //     .forEach { line ->
    //         if (isNextLineBlockComment) {
    //             isNextLineBlockComment = false
    //             isLineBlockComment = true
    //         }
    //
    //         val level = line.asSequence()
    //             .takeWhile(Char::isWhitespace)
    //             // Count indentation here, where indentation is two spaces
    //             // So divide by 2 to get level
    //             .count() / 2
    //
    //         // Naive impl, hash count technically be inside String
    //         val lineWithoutComment = line.substringBefore("#").trim()
    //         val comment = line.substringAfter("#", missingDelimiterValue = "").trim()
    //
    //         // Block comment
    //         if (lineWithoutComment.endsWith("|-") || lineWithoutComment.endsWith("|")) {
    //             isNextLineBlockComment = true
    //         }
    //
    //         if (level <= position.size) {
    //             isLineBlockComment = false
    //         }
    //
    //         if (lineWithoutComment.isNotEmpty()) {
    //             // Try get out of block comment if possible
    //             if (!isLineBlockComment) {
    //                 // println(line)
    //
    //                 val newPrefix = position.take(level)
    //                 position.clear()
    //                 position.addAll(newPrefix)
    //
    //                 if (position.size != level) {
    //                     error("Not enough")
    //                 }
    //
    //                 if (lineWithoutComment.startsWith("-")) {
    //                     position.add(JsonNodePathSegment.Int(0))
    //                 }
    //
    //                 // Same level or lower
    //                 if (lineWithoutComment.endsWith(":")) {
    //                     val key = lineWithoutComment.substringBefore(":")
    //                     position.add(JsonNodePathSegment.String(key))
    //                 }
    //             }
    //         }
    //
    //         if (!isNextLineBlockComment && comment.isNotBlank()) {
    //             println("At $position")
    //             println(comment)
    //         }
    //     }
}
