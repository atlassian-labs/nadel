package graphql.nadel.enginekt.transform.hydration

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal object NadelHydrationUtil {
    fun getSourceFieldDefinition(
        instruction: NadelGenericHydrationInstruction,
    ): GraphQLFieldDefinition {
        return getSourceFieldDefinition(
            service = instruction.sourceService,
            pathToSourceField = instruction.pathToSourceField,
        )
    }

    private fun getSourceFieldDefinition(
        service: Service,
        pathToSourceField: List<String>,
    ): GraphQLFieldDefinition {
        val parentType = pathToSourceField
            .asSequence()
            .take(pathToSourceField.size - 1) // All but last element
            .fold(service.underlyingSchema.queryType) { prevType, fieldName ->
                prevType.getField(fieldName)!!.type as GraphQLObjectType
            }

        return parentType.getField(pathToSourceField.last())
    }

    fun makeTypeNameField(
        alias: String,
        objectTypeNames: List<String>,
    ): NormalizedField {
        return newNormalizedField()
            .alias(alias)
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(objectTypeNames)
            .build()
    }
}
