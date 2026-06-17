package graphql.nadel.schema

import graphql.nadel.util.AnySDLDefinition

fun interface NadelSchemaDefinitionTransformationHook {
    operator fun invoke(definitions: List<AnySDLDefinition>): List<AnySDLDefinition>

    companion object {
        val Identity = NadelSchemaDefinitionTransformationHook { originalSchema -> originalSchema }
    }
}
