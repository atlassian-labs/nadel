package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.tests.yaml.YamlComment.Position.Block
import graphql.nadel.tests.yaml.YamlComment.Position.End
import graphql.nadel.tests.yaml.YamlComment.Position.Inline
import graphql.nadel.tests.yaml.collectComments
import graphql.nadel.tests.yaml.traverseYaml
import graphql.nadel.tests.yaml.writeYamlTo
import java.io.File

// For autocomplete navigation
private typealias TestFixtureUpdater = Unit

fun main() {
    val path = ClassLoader.getSystemClassLoader().getResource("")!!.path
    val projectDir = path.substringBeforeLast("test/build/")

    val fixturesDir = File(projectDir, "test/src/test/resources/fixtures")

    update(fixturesDir)
}

private fun update(fixturesDir: File) {
    val yamlExtensions = setOf("yaml", "yml")

    fixturesDir.walkTopDown()
        .filter {
            it.extension in yamlExtensions
        }
        .forEach { file ->
            println(file.name)

            // If you don't have trimmed line ends then the writer seems to not want to spit out multi line strings
            val cleanFileContents = file
                .readLines()
                .joinToString(separator = "\n", transform = String::trimEnd)

            val testFixture = yamlObjectMapper.readValue<TestFixture>(cleanFileContents)

            val newFixture = update(testFixture)
            writeOut(file, newFixture)
        }
}

fun update(testFixture: TestFixture): TestFixture {
    return testFixture
}

fun writeOut(fixtureFile: File, testFixture: TestFixture) {
    val commentsFromOldYaml = collectComments(fixtureFile).toMutableMap()

    val rootNodes = traverseYaml(yamlObjectMapper.writeValueAsString(testFixture)) { path, node ->
        val comments = commentsFromOldYaml.remove(path)

        node.blockComments = node.blockComments ?: mutableListOf()
        node.inLineComments = node.inLineComments ?: mutableListOf()
        node.endComments = node.endComments ?: mutableListOf()

        comments?.forEach {
            when (it.position) {
                Block -> node.blockComments.add(it.comment)
                Inline -> node.inLineComments.add(it.comment)
                End -> node.endComments.add(it.comment)
            }
        }
    }

    writeYamlTo(rootNodes, fixtureFile)
}
