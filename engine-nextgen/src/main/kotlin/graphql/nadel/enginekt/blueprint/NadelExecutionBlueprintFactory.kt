package graphql.nadel.enginekt.blueprint

import graphql.language.TypeDefinition
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
import graphql.nadel.enginekt.util.mapFrom
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.schema.NadelDirectives
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil
import graphql.schema.FieldCoordinates.coordinates as createFieldCoordinates

internal typealias AnyTypeDefinition = TypeDefinition<*>

internal object NadelExecutionBlueprintFactory {
    fun create(overallSchema: GraphQLSchema, services: List<Service>): NadelExecutionBlueprint {
        val typeRenameInstructions = createTypeRenameInstructions(overallSchema).strictAssociateBy {
            it.overallName
        }
        val fieldInstructions = createFieldInstructions(overallSchema, services).strictAssociateBy {
            it.location
        }

        val serviceTypeRenameInstructions = createServiceTypeRenameInstructions(services)

        return NadelExecutionBlueprint(
            fieldInstructions,
            NadelTypeRenameInstructions(
                typeRenameInstructions,
                serviceTypeRenameInstructions,
            ),
        )
    }

    private fun createServiceTypeRenameInstructions(
        services: List<Service>,
    ): Map<String, NadelServiceTypeRenameInstructions> {
        return mapFrom(
            services.map { service ->
                service.definitionRegistry.definitions
                    .asSequence()
                    .filterIsInstance<AnyTypeDefinition>()
                    .mapNotNull { def ->
                        createTypeRenameInstruction(def)
                    }
                    .strictAssociateBy {
                        it.underlyingName
                    }
                    .let {
                        service.name to NadelServiceTypeRenameInstructions(it)
                    }
            },
        )
    }

    private fun createFieldInstructions(
        overallSchema: GraphQLSchema,
        services: List<Service>,
    ): List<NadelFieldInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLObjectType>()
            .flatMap { type ->
                type.fields
                    .asSequence()
                    // Get the field mapping def
                    .mapNotNull { field ->
                        when (val mappingDefinition = getFieldMappingDefinition(field)) {
                            null -> when (val hydration = getUnderlyingServiceHydration(field)) {
                                null -> null
                                else -> createHydrationFieldInstruction(services, type, field, hydration)
                            }
                            else -> when (mappingDefinition.inputPath.size) {
                                1 -> createRenameInstruction(type, field, mappingDefinition)
                                else -> createDeepRenameFieldInstruction(type, field, mappingDefinition)
                            }
                        }
                    }
            }
            .toList()
    }

    private fun createDeepRenameFieldInstruction(
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelFieldInstruction {
        val location = createFieldCoordinates(parentType, field)

        return NadelDeepRenameFieldInstruction(
            location,
            mappingDefinition.inputPath,
        )
    }

    private fun createHydrationFieldInstruction(
        services: List<Service>,
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelFieldInstruction {
        val hydrationService = services.single { it.name == hydration.serviceName }
        val underlyingSchema = hydrationService.underlyingSchema

        val pathToSourceField = listOfNotNull(hydration.syntheticField, hydration.topLevelField)
        val sourceField = underlyingSchema.queryType.getFieldAt(pathToSourceField)!!

        if (GraphQLTypeUtil.isList(sourceField.type)) {
            return createBatchHydrationFieldInstruction(parentType, field, hydration)
        }

        return NadelHydrationFieldInstruction(
            location = createFieldCoordinates(parentType, field),
            sourceService = hydration.serviceName,
            pathToSourceField = pathToSourceField,
            arguments = getHydrationArguments(hydration),
        )
    }

    private fun createBatchHydrationFieldInstruction(
        type: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelFieldInstruction {
        val location = createFieldCoordinates(type, field)

        return NadelBatchHydrationFieldInstruction(
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

    private fun createRenameInstruction(
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelRenameFieldInstruction {
        return NadelRenameFieldInstruction(
            location = createFieldCoordinates(parentType, field),
            underlyingName = mappingDefinition.inputPath.single(),
        )
    }

    private fun createTypeRenameInstructions(
        overallSchema: GraphQLSchema,
    ): Sequence<NadelTypeRenameInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLDirectiveContainer>()
            .filter { it.definition is AnyTypeDefinition }
            .mapNotNull(this::createTypeRenameInstruction)
    }

    private fun createTypeRenameInstruction(type: GraphQLDirectiveContainer): NadelTypeRenameInstruction? {
        // Create from the extended type definition or try lookup instruction metadata in the directives
        return createTypeRenameInstruction(type.definition as AnyTypeDefinition)
            ?: NadelDirectives.createTypeMapping(type)?.let(::createTypeRenameInstruction)
    }

    private fun createTypeRenameInstruction(definition: AnyTypeDefinition): NadelTypeRenameInstruction? {
        return when (definition) {
            is ObjectTypeDefinitionWithTransformation -> createTypeRenameInstruction(definition.typeMappingDefinition)
            is InterfaceTypeDefinitionWithTransformation -> createTypeRenameInstruction(definition.typeMappingDefinition)
            is InputObjectTypeDefinitionWithTransformation -> createTypeRenameInstruction(definition.typeMappingDefinition)
            is EnumTypeDefinitionWithTransformation -> createTypeRenameInstruction(definition.typeMappingDefinition)
            else -> null
        }
    }

    private fun createTypeRenameInstruction(typeMappingDefinition: TypeMappingDefinition): NadelTypeRenameInstruction {
        return NadelTypeRenameInstruction(
            overallName = typeMappingDefinition.overallName,
            underlyingName = typeMappingDefinition.underlyingName,
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
}
