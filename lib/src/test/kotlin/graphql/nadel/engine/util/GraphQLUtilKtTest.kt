package graphql.nadel.engine.util

import graphql.Scalars.GraphQLString
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType.newObject
import io.kotest.core.spec.style.DescribeSpec

class GraphQLUtilKtTest : DescribeSpec({
    val bType = newObject().name("B")
        .field(newFieldDefinition().name("f2").type(GraphQLString))
        .build()

    val aType = newObject().name("A")
        .field(newFieldDefinition().name("f1").type(bType))
        .build()

    describe("getFieldAt tests") {
        it("can find fields at single path") {
            val fieldDef = aType.getFieldAt(listOf("f1"))

            assert(fieldDef?.name == "f1")
            assert(fieldDef?.type == bType)
        }

        it("can find fields at multi path") {
            val fieldDef = aType.getFieldAt(listOf("f1", "f2"))

            assert(fieldDef?.name == "f2")
            assert(fieldDef?.type == GraphQLString)
        }

        it("will return null with bogus paths") {
            val fieldDef = aType.getFieldAt(listOf("f1", "fX"))

            assert(fieldDef == null)
        }

        it("will return null with bogus paths that go over non containers") {
            val fieldDef = aType.getFieldAt(listOf("f1", "f2", "fX"))

            assert(fieldDef == null)
        }
    }

    describe("getFieldContainerAt tests") {
        it("can find container at single path") {
            val fieldContainer = aType.getFieldContainerAt(listOf("f1"))

            assert(fieldContainer?.name == "A")
        }

        it("can find field containers at multi path") {
            val fieldContainer = aType.getFieldContainerAt(listOf("f1", "f2"))

            assert(fieldContainer?.name == "B")
        }

        it("will return null with bogus paths") {
            val fieldContainer = aType.getFieldContainerAt(listOf("f1", "fX"))

            assert(fieldContainer == null)
        }

        it("will return null with bogus paths that go over non containers") {
            val fieldContainer = aType.getFieldContainerAt(listOf("f1", "f2", "fX"))

            assert(fieldContainer == null)
        }
    }

    describe("toGraphQLError") {
        it("handles case where error message is null") {
            val map = mapOf("message" to null)

            // When
            val error = toGraphQLError(map)

            // Then
            assert(error.message == "An error has occurred")
        }

        it("handles case where error message is missing") {
            val map: JsonMap = mapOf()

            // When
            val error = toGraphQLError(map)

            // Then
            assert(error.message == "An error has occurred")
        }

        it("copies error message") {
            val map = mapOf("message" to "banana")

            // When
            val error = toGraphQLError(map)

            // Then
            assert(error.message == "banana")
        }

        it("copies extensions") {
            val map = mapOf("extensions" to mapOf("hello" to "world"))

            // When
            val error = toGraphQLError(map)

            // Then
            assert(error.message == "An error has occurred")
            assert(error.extensions["hello"] == "world")
        }

        it("copies path") {
            val map = mapOf("path" to listOf("hello", "world"))

            // When
            val error = toGraphQLError(map)

            // Then
            assert(error.message == "An error has occurred")
            assert(error.path == listOf("hello", "world"))
        }
    }
})
