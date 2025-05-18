package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLUnionType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelUnionCoordinatesTest {
    @Test
    fun `can resolve union coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                union User = BotUser | AtlassianAccountUser
                type BotUser {
                    id: ID!
                }
                type AtlassianAccountUser {
                    name: String
                }
            """.trimIndent()
        )

        val coords = NadelUnionCoordinates("User")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "User")
        assertTrue(result === schema.getTypeAs<GraphQLUnionType>("User"))
    }

    @Test
    fun `resolving non existent union returns null`() {
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

        val coords = NadelUnionCoordinates("Account")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
