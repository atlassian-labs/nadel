package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLEnumType
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelEnumValueCoordinatesTest {
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

        val coords = NadelEnumCoordinates("Sounds").enumValue("MEOW")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result != null)
        assertTrue(result.name == "MEOW")
        assertTrue(result === schema.getTypeAs<GraphQLEnumType>("Sounds").getValue("MEOW"))
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

        val coords = NadelEnumCoordinates("Rain").enumValue("WOOF")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }

    @Test
    fun `resolving non existent value returns null`() {
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

        val coords = NadelEnumCoordinates("Sounds").enumValue("TIP")

        // When
        val result = coords.resolve(schema)

        // Then
        assertTrue(result == null)
    }
}
