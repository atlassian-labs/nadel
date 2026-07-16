package graphql.nadel.definition.coordinates

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NadelCoordinatesTreeTest : DescribeSpec({
    val mapper = jacksonObjectMapper()

    fun node(
        kind: NadelCoordinateKind,
        name: String,
        children: List<NadelCoordinatesNode> = emptyList(),
    ): NadelCoordinatesNode {
        return NadelCoordinatesNode(kind = kind, name = name, children = children)
    }

    describe("toTree") {
        it("returns an empty list for an empty set") {
            // When
            val tree = NadelCoordinatesTree.toTree(emptySet())

            // Then
            tree shouldBe emptyList()
        }

        it("converts a single top level coordinate") {
            // Given
            val coordinates = setOf(NadelObjectCoordinates("Query"))

            // When
            val tree = NadelCoordinatesTree.toTree(coordinates)

            // Then
            tree shouldBe listOf(
                node(NadelCoordinateKind.Object, "Query"),
            )
        }

        it("nests a child under its parent") {
            // Given
            val coordinates = setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("issue"),
            )

            // When
            val tree = NadelCoordinatesTree.toTree(coordinates)

            // Then
            tree shouldBe listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(NadelCoordinateKind.Field, "issue"),
                    ),
                ),
            )
        }

        it("shares a parent between multiple children instead of repeating it") {
            // Given
            val coordinates = setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("a"),
                NadelObjectCoordinates("Query").field("b"),
            )

            // When
            val tree = NadelCoordinatesTree.toTree(coordinates)

            // Then
            tree shouldBe listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(NadelCoordinateKind.Field, "a"),
                        node(NadelCoordinateKind.Field, "b"),
                    ),
                ),
            )
        }

        it("materializes missing ancestors so children are always reachable") {
            // Given
            val coordinates = setOf(
                NadelObjectCoordinates("Query").field("issue").argument("id"),
            )

            // When
            val tree = NadelCoordinatesTree.toTree(coordinates)

            // Then
            tree shouldBe listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(
                            NadelCoordinateKind.Field, "issue",
                            listOf(
                                node(NadelCoordinateKind.Argument, "id"),
                            ),
                        ),
                    ),
                ),
            )
        }

        it("sorts roots by kind then name") {
            // Given
            val coordinates = setOf(
                NadelObjectCoordinates("Zebra"),
                NadelObjectCoordinates("Apple"),
                NadelInterfaceCoordinates("Node"),
            )

            // When
            val tree = NadelCoordinatesTree.toTree(coordinates)

            // Then
            tree shouldBe listOf(
                node(NadelCoordinateKind.Interface, "Node"),
                node(NadelCoordinateKind.Object, "Apple"),
                node(NadelCoordinateKind.Object, "Zebra"),
            )
        }

        it("sorts children by kind then name") {
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
            tree shouldBe listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(NadelCoordinateKind.AppliedDirective, "auth"),
                        node(NadelCoordinateKind.Field, "aaa"),
                        node(NadelCoordinateKind.Field, "zzz"),
                    ),
                ),
            )
        }

        it("produces the same tree regardless of input iteration order") {
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
            fromOneOrder shouldBe fromAnotherOrder
        }
    }

    describe("fromTree") {
        it("returns an empty set for an empty list") {
            // When
            val coordinates = NadelCoordinatesTree.fromTree(emptyList())

            // Then
            coordinates shouldBe emptySet()
        }

        it("rebuilds every top level kind") {
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
            coordinates shouldBe setOf(
                NadelObjectCoordinates("AnObject"),
                NadelInterfaceCoordinates("AnInterface"),
                NadelUnionCoordinates("AUnion"),
                NadelEnumCoordinates("AnEnum"),
                NadelInputObjectCoordinates("AnInputObject"),
                NadelScalarCoordinates("AScalar"),
                NadelDirectiveCoordinates("aDirective"),
            )
        }

        it("rebuilds every child kind under a valid parent") {
            // Given
            val tree = listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(
                            NadelCoordinateKind.Field, "issue",
                            listOf(
                                node(NadelCoordinateKind.Argument, "id"),
                                node(
                                    NadelCoordinateKind.AppliedDirective, "auth",
                                    listOf(
                                        node(NadelCoordinateKind.AppliedDirectiveArgument, "role"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                node(
                    NadelCoordinateKind.Enum, "Color",
                    listOf(
                        node(NadelCoordinateKind.EnumValue, "RED"),
                    ),
                ),
                node(
                    NadelCoordinateKind.InputObject, "Filter",
                    listOf(
                        node(NadelCoordinateKind.InputObjectField, "term"),
                    ),
                ),
                node(
                    NadelCoordinateKind.Directive, "include",
                    listOf(
                        node(NadelCoordinateKind.Argument, "if"),
                    ),
                ),
            )

            // When
            val coordinates = NadelCoordinatesTree.fromTree(tree)

            // Then
            coordinates shouldBe setOf(
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
            )
        }

        it("throws when a child kind appears at the top level") {
            // Given
            val tree = listOf(
                node(NadelCoordinateKind.Field, "issue"),
            )

            // When
            val error = shouldThrow<IllegalStateException> {
                NadelCoordinatesTree.fromTree(tree)
            }

            // Then
            error.message shouldBe "Field cannot be a top level coordinate"
        }

        it("throws when a top level kind appears as a child") {
            // Given
            val tree = listOf(
                node(
                    NadelCoordinateKind.Object, "Query",
                    listOf(
                        node(NadelCoordinateKind.Object, "NestedObject"),
                    ),
                ),
            )

            // When
            val error = shouldThrow<IllegalStateException> {
                NadelCoordinatesTree.fromTree(tree)
            }

            // Then
            error.message shouldBe "Object cannot be a child coordinate"
        }

        it("throws when a child is nested under an incompatible parent") {
            // Given
            val tree = listOf(
                node(
                    NadelCoordinateKind.Enum, "Color",
                    listOf(
                        node(NadelCoordinateKind.Field, "notAField"),
                    ),
                ),
            )

            // When & Then
            shouldThrow<ClassCastException> {
                NadelCoordinatesTree.fromTree(tree)
            }
        }
    }

    describe("round trip") {
        val parentClosedCoordinates = setOf(
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

        it("fromTree(toTree(x)) equals x for a parent closed set") {
            // When
            val result = NadelCoordinatesTree.fromTree(NadelCoordinatesTree.toTree(parentClosedCoordinates))

            // Then
            result shouldBe parentClosedCoordinates
        }

        it("adds ancestors when the input set is not parent closed") {
            // Given
            val coordinates = setOf(
                NadelObjectCoordinates("Query").field("issue").argument("id"),
            )

            // When
            val result = NadelCoordinatesTree.fromTree(NadelCoordinatesTree.toTree(coordinates))

            // Then
            result shouldBe setOf(
                NadelObjectCoordinates("Query"),
                NadelObjectCoordinates("Query").field("issue"),
                NadelObjectCoordinates("Query").field("issue").argument("id"),
            )
        }
    }

    describe("jackson serialization") {
        it("serializes a node without any polymorphic type information") {
            // Given
            val tree = listOf(node(NadelCoordinateKind.Object, "Query"))

            // When
            val json = mapper.readTree(mapper.writeValueAsBytes(tree)).single()

            // Then
            json.has("@class") shouldBe false
            json["kind"].asText() shouldBe "Object"
            json["name"].asText() shouldBe "Query"
        }

        it("round trips a tree through jackson") {
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
            deserialized shouldBe tree
        }

        it("round trips coordinates through the full serialization pipeline") {
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
            result shouldBe coordinates
        }
    }
})
