package graphql.nadel.schema

import graphql.nadel.Service
import graphql.nadel.util.AnySDLDefinition

fun interface NadelSchemaDefinitionTransformationHook {
    operator fun invoke(
        services: List<Service>,
        definitions: List<AnySDLDefinition>,
    ): List<AnySDLDefinition>

    companion object {
        val Identity = NadelSchemaDefinitionTransformationHook { _, definitions -> definitions }
    }
}
