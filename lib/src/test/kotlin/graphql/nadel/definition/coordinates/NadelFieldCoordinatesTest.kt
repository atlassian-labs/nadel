package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelFieldCoordinatesTest {
    @Test
    fun `can resolve interface field coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                interface Node {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInterfaceCoordinates("Node").field("id")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "id")
        assertTrue(result === schema.getTypeAs<GraphQLInterfaceType>("Node").getField("id"))
    }

    @Test
    fun `can resolve object field coordinates`() {
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

        val coords = NadelObjectCoordinates("User").field("name")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "name")
        assertTrue(result === schema.getTypeAs<GraphQLObjectType>("User").getField("name"))
    }

    @Test
    fun `resolving non existent interface returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                interface Node {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInterfaceCoordinates("Saucer").field("id")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
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

        val coords = NadelObjectCoordinates("Account").field("name")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `resolving non existent interface field returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                }
                interface Node {
                    id: ID!
                }
            """.trimIndent()
        )

        val coords = NadelInterfaceCoordinates("Node").field("name")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `resolving non existent object field returns null`() {
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

        val coords = NadelObjectCoordinates("User").field("address")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
