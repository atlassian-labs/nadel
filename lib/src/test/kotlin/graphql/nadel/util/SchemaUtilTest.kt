package graphql.nadel.util

import graphql.language.ObjectTypeDefinition
import graphql.nadel.engine.util.sequenceOfNulls
import graphql.parser.ParserOptions
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SchemaUtilTest {
    @Test
    fun `can parse large schemas into TypeDefinitionRegistry`() {
        val fields = sequenceOfNulls<String>(100_000)
            .mapIndexed { index, _ ->
                "echo_${index}: String"
            }
            .joinToString(separator = "\n")
        val schema = """
            type Query {
                $fields
            }
        """.trimIndent()

        // Greater than the default max characters
        assertTrue(schema.length > ParserOptions.MAX_QUERY_CHARACTERS)

        // When
        val typeDefs = SchemaUtil.parseTypeDefinitionRegistry(schema.reader())

        // Then
        val query = typeDefs.types().values.single()

        assertTrue(query is ObjectTypeDefinition)
        assertTrue(query.fieldDefinitions.size == 100_000)
    }
}
