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
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.schema.NadelDirectives
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil
import graphql.schema.FieldCoordinates.coordinates as createFieldCoordinates

internal object NadelExecutionBlueprintFactory {
    fun create(overallSchema: GraphQLSchema, services: List<Service>): NadelExecutionBlueprint {
        val typeRenameInstructions = createTypeRenameInstructions(overallSchema).strictAssociateBy {
            it.overallName
        }
        val fieldInstructions = createInstructions(overallSchema, services).strictAssociateBy {
            it.location
        }

        return NadelExecutionBlueprint(
            fieldInstructions,
            typeRenameInstructions,
        )
    }

    private fun createInstructions(overallSchema: GraphQLSchema, services: List<Service>): List<NadelFieldInstruction> {
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
            QueryPath(mappingDefinition.inputPath),
        )
    }

    private fun createHydrationFieldInstruction(
        services: List<Service>,
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelFieldInstruction {
        val hydrationSourceService = services.single { it.name == hydration.serviceName }
        val underlyingSchema = hydrationSourceService.underlyingSchema

        val queryPathToActorField = listOfNotNull(hydration.syntheticField, hydration.topLevelField)
        val actorField = underlyingSchema.queryType.getFieldAt(queryPathToActorField)!!

        if (GraphQLTypeUtil.isList(actorField.type)) {
            return createBatchHydrationFieldInstruction(parentType, field, hydration, hydrationSourceService)
        }

        return NadelHydrationFieldInstruction(
            location = createFieldCoordinates(parentType, field),
            actorService = hydrationSourceService,
            queryPathToActorField = QueryPath(queryPathToActorField),
            actorInputValues = getHydrationArguments(hydration),
        )
    }

    private fun createBatchHydrationFieldInstruction(
        type: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
        sourceService: Service,
    ): NadelFieldInstruction {
        val location = createFieldCoordinates(type, field)

        return NadelBatchHydrationFieldInstruction(
            location,
            actorService = sourceService,
            queryPathToActorField = QueryPath(listOfNotNull(hydration.syntheticField, hydration.topLevelField)),
            actorInputValues = getHydrationArguments(hydration),
            batchSize = hydration.batchSize ?: 50,
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

    private fun createTypeRenameInstructions(overallSchema: GraphQLSchema): Sequence<NadelTypeRenameInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLDirectiveContainer>()
            .mapNotNull(this::createTypeRenameInstruction)
    }

    private fun createTypeRenameInstruction(type: GraphQLDirectiveContainer): NadelTypeRenameInstruction? {
        return when (val def = type.definition) {
            is ObjectTypeDefinitionWithTransformation -> createTypeRenameInstruction(def.typeMappingDefinition)
            is InterfaceTypeDefinitionWithTransformation -> createTypeRenameInstruction(def.typeMappingDefinition)
            is InputObjectTypeDefinitionWithTransformation -> createTypeRenameInstruction(def.typeMappingDefinition)
            is EnumTypeDefinitionWithTransformation -> createTypeRenameInstruction(def.typeMappingDefinition)
            else -> when (val typeMappingDef = NadelDirectives.createTypeMapping(type)) {
                null -> null
                else -> createTypeRenameInstruction(typeMappingDef)
            }
        }
    }

    private fun createTypeRenameInstruction(typeMappingDefinition: TypeMappingDefinition): NadelTypeRenameInstruction {
        return NadelTypeRenameInstruction(
            overallName = typeMappingDefinition.overallName,
            underlyingName = typeMappingDefinition.underlyingName,
        )
    }

    private fun getHydrationArguments(hydration: UnderlyingServiceHydration): List<NadelHydrationActorInput> {
        return hydration.arguments.map { argumentDef ->
            val valueSource = when (val argSourceType = argumentDef.remoteArgumentSource.sourceType) {
                FIELD_ARGUMENT -> NadelHydrationActorInput.ValueSource.ArgumentValue(argumentDef.remoteArgumentSource.name)
                OBJECT_FIELD -> NadelHydrationActorInput.ValueSource.FieldResultValue(QueryPath(argumentDef.remoteArgumentSource.path))
                else -> error("Unsupported remote argument source type: '$argSourceType'")
            }
            NadelHydrationActorInput(argumentDef.name, valueSource)
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
