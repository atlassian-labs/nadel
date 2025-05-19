package graphql.nadel.definition.coordinates

import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NadelAppliedDirectiveArgumentCoordinatesTest {
    @Test
    fun `can resolve coordinates`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {echo: String}
            """.trimIndent()
        )

        val coords = NadelObjectCoordinates("Query").appliedDirective("test").argument("message")

        // When
        val ex = assertFails {
            coords.resolve(schema)
        }

        // Then
        assertTrue(ex is UnsupportedOperationException)
    }
}
