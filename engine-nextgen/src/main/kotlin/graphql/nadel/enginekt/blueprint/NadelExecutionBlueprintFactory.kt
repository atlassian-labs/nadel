package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.dsl.EnumTypeDefinitionWithTransformation
import graphql.nadel.dsl.ExtendedFieldDefinition
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.InputObjectTypeDefinitionWithTransformation
import graphql.nadel.dsl.InterfaceTypeDefinitionWithTransformation
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD
import graphql.nadel.dsl.TypeMappingDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.toMap
import graphql.nadel.schema.NadelDirectives
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil
import graphql.schema.FieldCoordinates.coordinates as createFieldCoordinates

internal object NadelExecutionBlueprintFactory {
    fun create(overallSchema: GraphQLSchema, services: List<Service>): NadelExecutionBlueprint {
        val typeRenameInstructions = getTypeRenameInstructions(overallSchema).toMap {
            it.overallName
        }
        val fieldRenameInstructions = getFieldRenameInstructions(overallSchema).toMap {
            createFieldCoordinates(it.parentTypeName, it.overallName)
        }
        val otherInstructions = getInstructions(overallSchema, services).toMap {
            it.location
        }

        return NadelExecutionBlueprint(
            fieldRenameInstructions,
            typeRenameInstructions,
            otherInstructions,
        )
    }

    private fun getInstructions(overallSchema: GraphQLSchema, services: List<Service>): List<NadelInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLFieldsContainer>()
            .flatMap { type ->
                type.fields
                    .asSequence()
                    // Get the field mapping def
                    .mapNotNull { field ->
                        when (val mappingDefinition = getFieldMappingDefinition(field)) {
                            null -> when (val hydration = getUnderlyingServiceHydration(field)) {
                                null -> null
                                else -> getHydrationField(services, type, field, hydration)
                            }
                            else -> when (mappingDefinition.inputPath.size) {
                                1 -> null
                                else -> getDeepRenameInstruction(type, field, mappingDefinition)
                            }
                        }
                    }
            }
            .toList()
    }

    private fun getDeepRenameInstruction(
        parentType: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition
    ): NadelInstruction {
        val location = createFieldCoordinates(parentType, field)

        return NadelDeepRenameInstruction(
            location,
            mappingDefinition.inputPath,
        )
    }

    private fun getHydrationField(
        services: List<Service>,
        parentType: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelInstruction {
        val hydrationService = services.single { it.name == hydration.serviceName }
        val underlyingSchema = hydrationService.underlyingSchema

        val pathToSourceField = listOfNotNull(hydration.syntheticField, hydration.topLevelField)
        val sourceField = underlyingSchema.queryType.getFieldAt(pathToSourceField)!!

        if (GraphQLTypeUtil.isList(sourceField.type)) {
            return getBatchHydrationField(parentType, field, hydration)
        }

        return NadelHydrationInstruction(
            location = createFieldCoordinates(parentType, field),
            sourceService = hydration.serviceName,
            pathToSourceField = pathToSourceField,
            arguments = getHydrationArguments(hydration),
        )
    }

    private fun getBatchHydrationField(
        type: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelInstruction {
        val location = createFieldCoordinates(type, field)

        return NadelBatchHydrationInstruction(
            location,
            sourceService = hydration.serviceName,
            pathToSourceField = listOfNotNull(hydration.syntheticField, hydration.topLevelField),
            arguments = getHydrationArguments(hydration),
            // TODO: figure out what to do for default batch sizes, nobody uses them in central schema
            batchSize = hydration.batchSize!!,
            batchHydrationMatchStrategy = if (hydration.isObjectMatchByIndex) {
                NadelBatchHydrationMatchStrategy.MatchIndex
            } else {
                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(objectId = hydration.objectIdentifier)
            },
        )
    }

    private fun getHydrationArguments(hydration: UnderlyingServiceHydration): List<NadelHydrationArgument> {
        return hydration.arguments.map { argumentDef ->
            val valueSource = when (val argSourceType = argumentDef.remoteArgumentSource.sourceType) {
                FIELD_ARGUMENT -> NadelHydrationArgumentValueSource.ArgumentValue(argumentDef.remoteArgumentSource.name)
                OBJECT_FIELD -> NadelHydrationArgumentValueSource.FieldValue(argumentDef.remoteArgumentSource.path)
                else -> error("Unsupported remote argument source type: '$argSourceType'")
            }
            NadelHydrationArgument(argumentDef.name, valueSource)
        }
    }

    private fun getFieldMappingDefinition(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        val extendedDef = field.definition as? ExtendedFieldDefinition
        return extendedDef?.fieldTransformation?.fieldMappingDefinition
            ?: NadelDirectives.createFieldMapping(field)
    }

    private fun getUnderlyingServiceHydration(field: GraphQLFieldDefinition): UnderlyingServiceHydration? {
        val extendedDef = field.definition as? ExtendedFieldDefinition
        return extendedDef?.fieldTransformation?.underlyingServiceHydration
            ?: NadelDirectives.createUnderlyingServiceHydration(field)
    }

    private fun getFieldRenameInstructions(overallSchema: GraphQLSchema): Sequence<NadelFieldRenameInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLFieldsContainer>()
            .flatMap { type ->
                type.fields
                    .asSequence()
                    .mapNotNull { field ->
                        val mappingDefinition = getFieldMappingDefinition(field)
                        // Only handle basic renames
                        when (mappingDefinition?.inputPath?.size) {
                            1 -> getUnderlyingField(type, field, mappingDefinition)
                            else -> null
                        }
                    }
            }
    }

    private fun getUnderlyingField(
        type: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelFieldRenameInstruction {
        return NadelFieldRenameInstruction(
            parentTypeName = type.name,
            overallName = field.name,
            underlyingName = mappingDefinition.inputPath.single(),
        )
    }

    private fun getTypeRenameInstructions(overallSchema: GraphQLSchema): Sequence<NadelTypeRenameInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLDirectiveContainer>()
            .mapNotNull(::getTypeRenameInstruction)
    }

    private fun getTypeRenameInstruction(type: GraphQLDirectiveContainer): NadelTypeRenameInstruction? {
        return when (val def = type.definition) {
            is ObjectTypeDefinitionWithTransformation -> getUnderlyingType(def.typeMappingDefinition)
            is InterfaceTypeDefinitionWithTransformation -> getUnderlyingType(def.typeMappingDefinition)
            is InputObjectTypeDefinitionWithTransformation -> getUnderlyingType(def.typeMappingDefinition)
            is EnumTypeDefinitionWithTransformation -> getUnderlyingType(def.typeMappingDefinition)
            else -> when (val typeMappingDef = NadelDirectives.createTypeMapping(type)) {
                null -> null
                else -> getUnderlyingType(typeMappingDef)
            }
        }
    }

    private fun getUnderlyingType(typeMappingDefinition: TypeMappingDefinition): NadelTypeRenameInstruction {
        return NadelTypeRenameInstruction(
            overallName = typeMappingDefinition.overallName,
            underlyingName = typeMappingDefinition.underlyingName,
        )
    }
}
