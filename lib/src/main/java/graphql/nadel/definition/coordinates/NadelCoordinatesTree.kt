package graphql.nadel.definition.coordinates

/**
 * Converts a flat [Set] of [NadelSchemaMemberCoordinates] to and from an equivalent tree.
 *
 * Each coordinate carries its full parent chain, so serializing the flat [Set] repeats shared ancestors
 * once per descendant. In the tree each coordinate appears once and parents are shared via nesting.
 */
object NadelCoordinatesTree {
    fun toTree(coordinates: Set<NadelSchemaMemberCoordinates>): List<NadelCoordinatesNode> {
        val builders = LinkedHashMap<NadelSchemaMemberCoordinates, MutableNode>()
        val roots = mutableListOf<MutableNode>()

        fun ensure(coordinate: NadelSchemaMemberCoordinates): MutableNode {
            return builders.getOrPut(coordinate) {
                val node = MutableNode(coordinate.kind, coordinate.name)
                val parent = coordinate.parentOrNull
                if (parent == null) {
                    roots.add(node)
                } else {
                    ensure(parent).children.add(node)
                }
                node
            }
        }

        coordinates
            .asSequence()
            .sorted()
            .forEach(::ensure)

        return roots.map { it.toNode() }
    }

    fun fromTree(nodes: List<NadelCoordinatesNode>): Set<NadelSchemaMemberCoordinates> {
        val coordinates = LinkedHashSet<NadelSchemaMemberCoordinates>()

        fun visit(node: NadelCoordinatesNode, parent: NadelSchemaMemberCoordinates?) {
            val coordinate = if (parent == null) buildRoot(node) else buildChild(parent, node)
            coordinates.add(coordinate)
            node.children.forEach { child ->
                visit(child, coordinate)
            }
        }

        nodes.forEach { node ->
            visit(node, parent = null)
        }

        return coordinates
    }

    private fun buildRoot(node: NadelCoordinatesNode): NadelSchemaMemberCoordinates {
        return when (node.kind) {
            NadelCoordinateKind.Object -> NadelObjectCoordinates(node.name)
            NadelCoordinateKind.Interface -> NadelInterfaceCoordinates(node.name)
            NadelCoordinateKind.Union -> NadelUnionCoordinates(node.name)
            NadelCoordinateKind.Enum -> NadelEnumCoordinates(node.name)
            NadelCoordinateKind.InputObject -> NadelInputObjectCoordinates(node.name)
            NadelCoordinateKind.Scalar -> NadelScalarCoordinates(node.name)
            NadelCoordinateKind.Directive -> NadelDirectiveCoordinates(node.name)
            NadelCoordinateKind.Field,
            NadelCoordinateKind.InputObjectField,
            NadelCoordinateKind.Argument,
            NadelCoordinateKind.EnumValue,
            NadelCoordinateKind.AppliedDirective,
            NadelCoordinateKind.AppliedDirectiveArgument,
            -> error("${node.kind} cannot be a top level coordinate")
        }
    }

    private fun buildChild(
        parent: NadelSchemaMemberCoordinates,
        node: NadelCoordinatesNode,
    ): NadelSchemaMemberCoordinates {
        return when (node.kind) {
            NadelCoordinateKind.Field -> (parent as NadelFieldContainerCoordinates).field(node.name)
            NadelCoordinateKind.Argument -> (parent as NadelArgumentParentCoordinates).argument(node.name)
            NadelCoordinateKind.AppliedDirective -> (parent as NadelAppliedDirectiveParentCoordinates).appliedDirective(node.name)
            NadelCoordinateKind.InputObjectField -> (parent as NadelInputObjectCoordinates).field(node.name)
            NadelCoordinateKind.EnumValue -> (parent as NadelEnumCoordinates).enumValue(node.name)
            NadelCoordinateKind.AppliedDirectiveArgument -> (parent as NadelAppliedDirectiveCoordinates).argument(node.name)
            NadelCoordinateKind.Object,
            NadelCoordinateKind.Interface,
            NadelCoordinateKind.Union,
            NadelCoordinateKind.Enum,
            NadelCoordinateKind.InputObject,
            NadelCoordinateKind.Scalar,
            NadelCoordinateKind.Directive,
            -> error("${node.kind} cannot be a child coordinate")
        }
    }

    private class MutableNode(
        private val kind: NadelCoordinateKind,
        private val name: String,
    ) {
        val children = mutableListOf<MutableNode>()

        fun toNode(): NadelCoordinatesNode {
            return NadelCoordinatesNode(
                kind = kind,
                name = name,
                children = children.map { it.toNode() },
            )
        }
    }
}
