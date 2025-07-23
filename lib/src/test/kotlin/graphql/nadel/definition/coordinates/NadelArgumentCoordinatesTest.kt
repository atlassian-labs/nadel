package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLObjectType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelArgumentCoordinatesTest {
    @Test
    fun `can resolve field argument coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("Query").field("echo").argument("message")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.description == "Wow")
        assertTrue(result == schema.getTypeAs<GraphQLObjectType>("Query").getField("echo").getArgument("message"))
    }

    @Test
    fun `returns null for non existent field`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    helloWorld("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("Query").field("echo").argument("message")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `returns null for non existent type`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("Mutation").field("echo").argument("message")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `can resolve directive argument coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @cats("Woof" meow: String) on OBJECT
                type Query {
                    echo("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelDirectiveCoordinates("cats").argument("meow")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.description == "Woof")
        assertTrue(result === schema.getDirective("cats").getArgument("meow"))
    }

    @Test
    fun `returns null for non existent directive`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @cats("Woof" meow: String) on OBJECT
                type Query {
                    echo("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelDirectiveCoordinates("dogs").argument("meow")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
