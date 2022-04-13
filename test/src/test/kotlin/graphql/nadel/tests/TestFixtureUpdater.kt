package graphql.nadel.tests

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.engine.transform.result.json.JsonNodePath
import graphql.nadel.tests.yaml.YamlComment.Position.Block
import graphql.nadel.tests.yaml.YamlComment.Position.End
import graphql.nadel.tests.yaml.YamlComment.Position.Inline
import graphql.nadel.tests.yaml.collectComments
import graphql.nadel.tests.yaml.traverseYaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer
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

    val output = fixtureFile.writer()

    val options = DumperOptions()

    options.defaultScalarStyle = ScalarStyle.PLAIN
    options.defaultFlowStyle = FlowStyle.BLOCK
    options.isProcessComments = true
    options.splitLines = false
    options.indentWithIndicator = true
    options.indicatorIndent = 2
    options.indent = 2

    val serializer = Serializer(Emitter(output, options), Resolver(), options, null)
    serializer.open()

    rootNodes.forEach { rootNode ->
        serializer.serialize(rootNode)
    }

    serializer.close()
    output.close()
}
