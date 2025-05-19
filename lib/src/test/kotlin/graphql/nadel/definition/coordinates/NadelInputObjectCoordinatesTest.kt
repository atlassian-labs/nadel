package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInputObjectType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelInputObjectCoordinatesTest {
    @Test
    fun `can resolve input object coordinates`() {
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

        val coords = NadelInputObjectCoordinates("UserInput")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "UserInput")
        assertTrue(result === schema.getTypeAs<GraphQLInputObjectType>("UserInput"))
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

        val coords = NadelInputObjectCoordinates("Saucer")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
