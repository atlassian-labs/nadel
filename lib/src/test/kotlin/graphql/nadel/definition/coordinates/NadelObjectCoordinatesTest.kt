package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLObjectType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelObjectCoordinatesTest {
    @Test
    fun `can resolve object coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                type User {
                    id: ID!
                    name: String
                    email: String
                }
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("User")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "User")
        assertTrue(result === schema.getTypeAs<GraphQLObjectType>("User"))
    }

    @Test
    fun `resolving non existent object returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                type User {
                    id: ID!
                    name: String
                    email: String
                }
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("Account")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
