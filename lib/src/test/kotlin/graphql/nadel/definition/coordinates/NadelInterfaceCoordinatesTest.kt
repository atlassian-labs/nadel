package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInterfaceType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelInterfaceCoordinatesTest {
    @Test
    fun `can resolve interface coordinates`() {
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

        val coords = NadelInterfaceCoordinates("Node")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "Node")
        assertTrue(result === schema.getTypeAs<GraphQLInterfaceType>("Node"))
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

        val coords = NadelInterfaceCoordinates("Saucer")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
