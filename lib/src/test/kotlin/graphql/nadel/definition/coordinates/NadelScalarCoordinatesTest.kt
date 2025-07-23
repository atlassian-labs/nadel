package graphql.nadel.definition.coordinates

import graphql.Scalars
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelScalarCoordinatesTest {
    @Test
    fun `can resolve built in scalars coordinates`() {
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

        val coords = NadelScalarCoordinates("ID")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "ID")
        assertTrue(result === Scalars.GraphQLID)
    }

    @Test
    fun `can resolve user defined scalar`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                scalar URL
            """.trimIndent()
        )

        val coords = NadelScalarCoordinates("URL")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "URL")
        assertTrue(result === schema.getTypeAs<GraphQLScalarType>("URL"))
    }

    @Test
    fun `resolving non existent scalar returns null`() {
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

        val coords = NadelScalarCoordinates("URL")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
