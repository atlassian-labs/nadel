package graphql.nadel.tests.yaml

import graphql.nadel.engine.transform.result.json.JsonNodePath
import graphql.nadel.engine.transform.result.json.JsonNodePathSegment
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.comments.CommentLine
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.nodes.AnchorNode
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer
import java.io.File
import java.io.StringWriter
import java.io.Writer

data class YamlComment(
    val position: Position,
    val comment: CommentLine,
) {
    enum class Position {
        Block,
        Inline,
        End,
    }
}

fun writeYamlTo(nodes: List<Node>, file: File) {
    file.writer().use { writer ->
        writeYamlTo(nodes, writer)
    }
}

fun writeYamlAsString(nodes: List<Node>): String {
    StringWriter().use { writer ->
        writeYamlTo(nodes, writer)
        return writer.toString()
    }
}

private fun writeYamlTo(nodes: List<Node>, writer: Writer) {
    val options = DumperOptions()

    options.defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
    options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    options.isProcessComments = true
    options.splitLines = false
    options.indentWithIndicator = true
    options.indicatorIndent = 2
    options.indent = 2

    Serializer(Emitter(writer, options), Resolver(), options, null).use { serializer ->
        nodes.forEach { rootNode ->
            serializer.serialize(rootNode)
        }
    }
}

private fun Serializer.use(block: (Serializer) -> Unit) {
    open()
    try {
        block(this)
    } finally {
        close()
    }
}

fun collectComments(yaml: String): Map<JsonNodePath, List<YamlComment>> {
    return collectComments(StreamReader(yaml.reader()))
}

fun collectComments(yaml: File): Map<JsonNodePath, List<YamlComment>> {
    return collectComments(StreamReader(yaml.reader()))
}

fun collectComments(reader: StreamReader): Map<JsonNodePath, List<YamlComment>> {
    val collection = mutableMapOf<JsonNodePath, List<YamlComment>>()

    traverseYaml(reader) { path, node ->
        val comments = (node.blockComments ?: emptyList()).map { YamlComment(YamlComment.Position.Block, it) } +
            (node.inLineComments ?: emptyList()).map { YamlComment(YamlComment.Position.Inline, it) } +
            (node.endComments ?: emptyList()).map { YamlComment(YamlComment.Position.End, it) }

        if (comments.isNotEmpty()) {
            collection[path] = comments
        }
    }

    return collection
}

fun traverseYaml(yaml: String, function: (JsonNodePath, Node) -> Unit): List<Node> {
    return traverseYaml(StreamReader(yaml.reader()), function)
}

fun traverseYaml(yaml: File, function: (JsonNodePath, Node) -> Unit): List<Node> {
    return traverseYaml(StreamReader(yaml.reader()), function)
}

/**
 * Traverses ALL nodes and returns the root nodes.
 */
fun traverseYaml(reader: StreamReader, function: (JsonNodePath, Node) -> Unit): List<Node> {
    val composer = Composer(ParserImpl(reader, true), Resolver())
    return traverse(composer, function)
}

private fun traverse(
    composer: Composer,
    function: (JsonNodePath, Node) -> Unit,
): List<Node> {
    val rootNodes = mutableListOf<Node>()

    while (composer.checkNode()) {
        val rootNode = composer.node
        rootNodes.add(rootNode)

        traverse(rootNode, JsonNodePath.root, function)
    }

    return rootNodes
}

private fun traverse(
    node: Node,
    path: JsonNodePath,
    function: (JsonNodePath, Node) -> Unit,
) {
    function(path, node)

    when (node) {
        is AnchorNode -> { // Do nothing?
        }
        is MappingNode -> {
            for (nodeTuple in node.value) {
                val key = (nodeTuple.keyNode as ScalarNode).value
                traverse(nodeTuple.keyNode, path + JsonNodePathSegment.String(key), function)
                traverse(nodeTuple.valueNode, path + JsonNodePathSegment.String(key), function)
            }
        }
        is SequenceNode -> {
            node.value.forEachIndexed { index, sequenceElementNode ->
                traverse(sequenceElementNode, path + JsonNodePathSegment.Int(index), function)
            }
        }
        is ScalarNode -> { // Do nothing?
        }
    }
}

