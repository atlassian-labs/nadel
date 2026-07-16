package graphql.nadel.definition.coordinates

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class NadelCoordinatesTreeTest {
    private val mapper = jacksonObjectMapper()

    private fun node(
        kind: NadelCoordinateKind,
        name: String,
        children: List<NadelCoordinatesNode> = emptyList(),
    ): NadelCoordinatesNode {
        return NadelCoordinatesNode(kind = kind, name = name, children = children)
    }

    @Test
    fun `toTree returns an empty list for an empty set`() {
        // When
        val tree = NadelCoordinatesTree.toTree(emptySet())

        // Then
        assertEquals(emptyList(), tree)
    }

    @Test
    fun `toTree converts a single top level coordinate`() {
        // Given
        val coordinates = setOf(NadelObjectCoordinates("Query"))

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(NadelCoordinateKind.Object, "Query"),
            ),
            tree,
        )
    }

    @Test
    fun `toTree nests a child under its parent`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("issue"),
        )

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(
                    NadelCoordinateKind.Object,
                    "Query",
                    listOf(
                        node(NadelCoordinateKind.Field, "issue"),
                    ),
                ),
            ),
            tree,
        )
    }

    @Test
    fun `toTree shares a parent between multiple children instead of repeating it`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("a"),
            NadelObjectCoordinates("Query").field("b"),
        )

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(
                    NadelCoordinateKind.Object,
                    "Query",
                    listOf(
                        node(NadelCoordinateKind.Field, "a"),
                        node(NadelCoordinateKind.Field, "b"),
                    ),
                ),
            ),
            tree,
        )
    }

    @Test
    fun `toTree materializes missing ancestors so children are always reachable`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query").field("issue").argument("id"),
        )

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(
                    NadelCoordinateKind.Object,
                    "Query",
                    listOf(
                        node(
                            NadelCoordinateKind.Field,
                            "issue",
                            listOf(
                                node(NadelCoordinateKind.Argument, "id"),
                            ),
                        ),
                    ),
                ),
            ),
            tree,
        )
    }

    @Test
    fun `toTree sorts roots by kind then name`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Zebra"),
            NadelObjectCoordinates("Apple"),
            NadelInterfaceCoordinates("Node"),
        )

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(NadelCoordinateKind.Interface, "Node"),
                node(NadelCoordinateKind.Object, "Apple"),
                node(NadelCoordinateKind.Object, "Zebra"),
            ),
            tree,
        )
    }

    @Test
    fun `toTree sorts children by kind then name`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("zzz"),
            NadelObjectCoordinates("Query").field("aaa"),
            NadelObjectCoordinates("Query").appliedDirective("auth"),
        )

        // When
        val tree = NadelCoordinatesTree.toTree(coordinates)

        // Then
        assertEquals(
            listOf(
                node(
                    NadelCoordinateKind.Object,
                    "Query",
                    listOf(
                        node(NadelCoordinateKind.AppliedDirective, "auth"),
                        node(NadelCoordinateKind.Field, "aaa"),
                        node(NadelCoordinateKind.Field, "zzz"),
                    ),
                ),
            ),
            tree,
        )
    }

    @Test
    fun `toTree produces the same tree regardless of input iteration order`() {
        // Given
        val coordinates = listOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("b"),
            NadelObjectCoordinates("Query").field("a"),
            NadelInterfaceCoordinates("Node"),
            NadelInterfaceCoordinates("Node").field("id"),
        )

        // When
        val fromOneOrder = NadelCoordinatesTree.toTree(LinkedHashSet(coordinates))
        val fromAnotherOrder = NadelCoordinatesTree.toTree(LinkedHashSet(coordinates.shuffled()))

        // Then
        assertEquals(fromOneOrder, fromAnotherOrder)
    }

    @Test
    fun `fromTree returns an empty set for an empty list`() {
        // When
        val coordinates = NadelCoordinatesTree.fromTree(emptyList())

        // Then
        assertEquals(emptySet(), coordinates)
    }

    @Test
    fun `fromTree rebuilds every top level kind`() {
        // Given
        val tree = listOf(
            node(NadelCoordinateKind.Object, "AnObject"),
            node(NadelCoordinateKind.Interface, "AnInterface"),
            node(NadelCoordinateKind.Union, "AUnion"),
            node(NadelCoordinateKind.Enum, "AnEnum"),
            node(NadelCoordinateKind.InputObject, "AnInputObject"),
            node(NadelCoordinateKind.Scalar, "AScalar"),
            node(NadelCoordinateKind.Directive, "aDirective"),
        )

        // When
        val coordinates = NadelCoordinatesTree.fromTree(tree)

        // Then
        assertEquals(
            setOf(
                NadelObjectCoordinates("AnObject"),
                NadelInterfaceCoordinates("AnInterface"),
                NadelUnionCoordinates("AUnion"),
                NadelEnumCoordinates("AnEnum"),
                NadelInputObjectCoordinates("AnInputObject"),
                NadelScalarCoordinates("AScalar"),
                NadelDirectiveCoordinates("aDirective"),
            ),
            coordinates,
        )
    }

    @Test
    fun `fromTree rebuilds every child kind under a valid parent`() {
        // Given
        val tree = listOf(
            node(
                NadelCoordinateKind.Object,
                "Query",
                listOf(
                    node(
                        NadelCoordinateKind.Field,
                        "issue",
                        listOf(
                            node(NadelCoordinateKind.Argument, "id"),
                            node(
                                NadelCoordinateKind.AppliedDirective,
                                "auth",
                                listOf(
                                    node(NadelCoordinateKind.AppliedDirectiveArgument, "role"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            node(
                NadelCoordinateKind.Enum,
                "Color",
                listOf(
                    node(NadelCoordinateKind.EnumValue, "RED"),
                ),
            ),
            node(
                NadelCoordinateKind.InputObject,
                "Filter",
                listOf(
                    node(NadelCoordinateKind.InputObjectField, "term"),
                ),
            ),
            node(
                NadelCoordinateKind.Directive,
                "include",
                listOf(
                    node(NadelCoordinateKind.Argument, "if"),
                ),
            ),
        )

        // When
        val coordinates = NadelCoordinatesTree.fromTree(tree)

        // Then
        assertEquals(
            setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("issue"),
                NadelObjectCoordinates("Query").field("issue").argument("id"),
                NadelObjectCoordinates("Query").field("issue").appliedDirective("auth"),
                NadelObjectCoordinates("Query").field("issue").appliedDirective("auth").argument("role"),
                NadelEnumCoordinates("Color"),
                NadelEnumCoordinates("Color").enumValue("RED"),
                NadelInputObjectCoordinates("Filter"),
                NadelInputObjectCoordinates("Filter").field("term"),
                NadelDirectiveCoordinates("include"),
                NadelDirectiveCoordinates("include").argument("if"),
            ),
            coordinates,
        )
    }

    @Test
    fun `fromTree throws when a child kind appears at the top level`() {
        // Given
        val tree = listOf(
            node(NadelCoordinateKind.Field, "issue"),
        )

        // When
        val error = assertFailsWith<IllegalStateException> {
            NadelCoordinatesTree.fromTree(tree)
        }

        // Then
        assertEquals("Field cannot be a top level coordinate", error.message)
    }

    @Test
    fun `fromTree throws when a top level kind appears as a child`() {
        // Given
        val tree = listOf(
            node(
                NadelCoordinateKind.Object,
                "Query",
                listOf(
                    node(NadelCoordinateKind.Object, "NestedObject"),
                ),
            ),
        )

        // When
        val error = assertFailsWith<IllegalStateException> {
            NadelCoordinatesTree.fromTree(tree)
        }

        // Then
        assertEquals("Object cannot be a child coordinate", error.message)
    }

    @Test
    fun `fromTree throws when a child is nested under an incompatible parent`() {
        // Given
        val tree = listOf(
            node(
                NadelCoordinateKind.Enum,
                "Color",
                listOf(
                    node(NadelCoordinateKind.Field, "notAField"),
                ),
            ),
        )

        // When & Then
        assertFailsWith<ClassCastException> {
            NadelCoordinatesTree.fromTree(tree)
        }
    }

    @Test
    fun `round trip fromTree of toTree equals the original parent closed set`() {
        // Given
        val coordinates = parentClosedCoordinates()

        // When
        val result = NadelCoordinatesTree.fromTree(NadelCoordinatesTree.toTree(coordinates))

        // Then
        assertEquals(coordinates, result)
    }

    @Test
    fun `round trip adds ancestors when the input set is not parent closed`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query").field("issue").argument("id"),
        )

        // When
        val result = NadelCoordinatesTree.fromTree(NadelCoordinatesTree.toTree(coordinates))

        // Then
        assertEquals(
            setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("issue"),
                NadelObjectCoordinates("Query").field("issue").argument("id"),
            ),
            result,
        )
    }

    @Test
    fun `jackson serializes a node without any polymorphic type information`() {
        // Given
        val tree = listOf(node(NadelCoordinateKind.Object, "Query"))

        // When
        val json = mapper.readTree(mapper.writeValueAsBytes(tree)).single()

        // Then
        assertFalse(json.has("@class"))
        assertEquals("Object", json["kind"].asText())
        assertEquals("Query", json["name"].asText())
    }

    @Test
    fun `jackson round trips a tree`() {
        // Given
        val tree = NadelCoordinatesTree.toTree(
            setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("issue"),
                NadelObjectCoordinates("Query").field("issue").argument("id"),
            ),
        )

        // When
        val json = mapper.writeValueAsBytes(tree)
        val deserialized = mapper.readValue<List<NadelCoordinatesNode>>(json)

        // Then
        assertEquals(tree, deserialized)
    }

    @Test
    fun `jackson round trips coordinates through the full serialization pipeline`() {
        // Given
        val coordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("issue"),
            NadelObjectCoordinates("Query").field("issue").argument("id"),
            NadelEnumCoordinates("Color"),
            NadelEnumCoordinates("Color").enumValue("RED"),
        )

        // When
        val json = mapper.writeValueAsBytes(NadelCoordinatesTree.toTree(coordinates))
        val result = NadelCoordinatesTree.fromTree(mapper.readValue<List<NadelCoordinatesNode>>(json))

        // Then
        assertEquals(coordinates, result)
    }

    private fun parentClosedCoordinates(): Set<NadelSchemaMemberCoordinates> {
        return setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("issue"),
            NadelObjectCoordinates("Query").field("issue").argument("id"),
            NadelObjectCoordinates("Query").field("issue").appliedDirective("auth"),
            NadelObjectCoordinates("Query").field("issue").appliedDirective("auth").argument("role"),
            NadelInterfaceCoordinates("Node"),
            NadelInterfaceCoordinates("Node").field("id"),
            NadelUnionCoordinates("SearchResult"),
            NadelEnumCoordinates("Color"),
            NadelEnumCoordinates("Color").enumValue("RED"),
            NadelInputObjectCoordinates("Filter"),
            NadelInputObjectCoordinates("Filter").field("term"),
            NadelScalarCoordinates("DateTime"),
            NadelDirectiveCoordinates("include"),
            NadelDirectiveCoordinates("include").argument("if"),
        )
    }
}
