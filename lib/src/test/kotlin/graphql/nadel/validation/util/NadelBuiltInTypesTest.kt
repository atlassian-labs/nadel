package graphql.nadel.validation.util

import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.AnySDLNamedDefinition
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertTrue

class NadelBuiltInTypesTest {
    @Test
    fun `built in types include all directives`() {
        // Given
        val namesFromNadelDirectives = NadelDirectives::class
            .declaredMemberProperties
            .map {
                it.get(NadelDirectives) as AnySDLNamedDefinition
            }
            .mapTo(LinkedHashSet()) { definition ->
                definition.name
            }

        // Sanity check
        assertTrue(namesFromNadelDirectives.isNotEmpty())
        assertTrue(namesFromNadelDirectives.contains("hydrated"))

        // Then
        assertTrue(NadelBuiltInTypes.builtInDirectiveSyntaxTypeNames.containsAll(namesFromNadelDirectives))
    }
}
