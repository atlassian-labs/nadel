package graphql.nadel.definition.stubbed

import graphql.introspection.Introspection.DirectiveLocation
import graphql.introspection.Introspection.DirectiveLocation.ENUM
import graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION
import graphql.introspection.Introspection.DirectiveLocation.INPUT_OBJECT
import graphql.introspection.Introspection.DirectiveLocation.OBJECT
import graphql.introspection.Introspection.DirectiveLocation.UNION
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelStubbedDefinitionTest {
    @Test
    fun `directive name are stable`() {
        assertTrue(NadelStubbedDefinition.directiveDefinition.name == "stubbed")
    }

    @Test
    fun `directive locations are stable`() {
        // Given
        val expectedLocations = setOf(
            FIELD_DEFINITION,
            OBJECT,
            INPUT_OBJECT,
            UNION,
            ENUM,
        )
        val actualLocations = NadelStubbedDefinition.directiveDefinition.directiveLocations.mapTo(mutableSetOf()) {
            DirectiveLocation.valueOf(it.name)
        }

        // Then
        assertTrue(actualLocations == expectedLocations)
    }
}
