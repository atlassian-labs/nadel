package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInputObjectType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelInputObjectFieldCoordinatesTest {
    @Test
    fun `can resolve input object field coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                input UserInput {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInputObjectCoordinates("UserInput").field("id")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "id")
        assertTrue(result === schema.getTypeAs<GraphQLInputObjectType>("UserInput").getField("id"))
    }

    @Test
    fun `resolving non existent input object returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                input UserInput {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInputObjectCoordinates("Saucer").field("id")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `resolving non existent input object field returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                input UserInput {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInputObjectCoordinates("Node").field("name")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
