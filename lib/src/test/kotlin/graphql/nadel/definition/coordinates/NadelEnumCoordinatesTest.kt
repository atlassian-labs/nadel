package graphql.nadel.definition.coordinates

import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelEnumCoordinatesTest {
    @Test
    fun `can resolve coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @cats("Woof" meow: String) on OBJECT
                type Query {
                    echo("Wow" message: String): String
                }
                enum Sounds {
                    WOOF
                    MEOW
                }
            """.trimIndent()
        )

        val coords = NadelEnumCoordinates("Sounds")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "Sounds")
        assertTrue(result === schema.getType("Sounds"))
    }

    @Test
    fun `resolving non existent type returns null`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @cats("Woof" meow: String) on OBJECT
                type Query {
                    echo("Wow" message: String): String
                }
                enum Sounds {
                    WOOF
                    MEOW
                }
            """.trimIndent()
        )

        val coords = NadelEnumCoordinates("Rain")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
