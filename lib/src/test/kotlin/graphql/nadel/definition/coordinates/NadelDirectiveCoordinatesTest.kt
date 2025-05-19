package graphql.nadel.definition.coordinates

import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelDirectiveCoordinatesTest {
    @Test
    fun `can resolve coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @cats("Woof" meow: String) on OBJECT
                type Query {
                    echo("Wow" message: String): String
                }
            """.trimIndent()
        )

        val coords = NadelDirectiveCoordinates("cats")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "cats")
        assertTrue(result === schema.getDirective("cats"))
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

        val coords = NadelDirectiveCoordinates("dogs")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
