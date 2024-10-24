package graphql.nadel.engine.blueprint

import graphql.introspection.Introspection
import graphql.language.AstPrinter
import graphql.language.OperationDefinition.Operation
import graphql.nadel.Service
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.engine.util.unwrapAll
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.idl.ScalarInfo

private fun NadelQueryPath.toNiceString() = segments.joinToString(separator = ".")

internal class NadelOverallExecutionBlueprintSerializer {
    fun toJsonMap(blueprint: NadelOverallExecutionBlueprintImpl): JsonMap {
        fun Set<String>.removeBuiltInTypes() = filterTo(HashSet()) {
            !Introspection.isIntrospectionTypes(it) &&
                !ScalarInfo.isGraphqlSpecifiedScalar(it) &&
                !it.equals(Operation.QUERY.name, ignoreCase = true) &&
                !it.equals(Operation.MUTATION.name, ignoreCase = true) &&
                !it.equals(Operation.SUBSCRIPTION.name, ignoreCase = true)
        }

        fun Set<String>.toTypeSignatures(schema: GraphQLSchema) = mapTo(HashSet()) {
            val type = schema.getType(it) ?: return@mapTo "?? $it"
            AstPrinter.printAstCompact(type.unwrapAll().definition).substringBefore("{").trim()
        }

        return mapOf(
            "fieldInstructions" to toJsonMap(blueprint.fieldInstructions),
            "underlyingTypeNamesByService" to toJsonMap(
                blueprint.underlyingTypeNamesByService
                    .mapValues { (service, types) ->
                        types.removeBuiltInTypes().toTypeSignatures(service.underlyingSchema)
                    },
            ),
            "overallTypeNamesByService" to toJsonMap(
                blueprint.overallTypeNamesByService
                    .mapValues { (_, types) ->
                        types.removeBuiltInTypes().toTypeSignatures(blueprint.engineSchema)
                    }
            ),
            "underlyingBlueprints" to toJsonMap(blueprint.underlyingBlueprints),
            "coordinatesToService" to toJsonMap(blueprint.coordinatesToService),
            "typeRenamesByOverallTypeName" to toJsonMap(blueprint.typeRenamesByOverallTypeName),
        )
    }

    @JvmName("FieldCoordinates_NadelFieldInstruction_toJsonMap")
    private fun toJsonMap(element: Map<FieldCoordinates, List<NadelFieldInstruction>>): JsonMap {
        return element
            .asSequence()
            .map { (coords, instructions) ->
                coords.toString() to instructions.map(::toJsonMap)
            }
            .toMapStrictly()
            .toSortedMap()
    }

    private fun toJsonMap(instruction: NadelFieldInstruction): JsonMap {
        return when (instruction) {
            is NadelBatchHydrationFieldInstruction -> mapOf(
                "location" to instruction.location.toString(),
                "virtualFieldDef" to instruction.virtualFieldDef.name,
                "backingService" to instruction.backingService.name,
                "queryPathToBackingField" to instruction.queryPathToBackingField.toNiceString(),
                "backingFieldArguments" to instruction.backingFieldArguments.sortedBy { it.name }.map(::toJsonMap),
                "timeout" to instruction.timeout,
                "sourceFields" to instruction.sourceFields.map(NadelQueryPath::toNiceString).sorted(),
                "backingFieldDef" to instruction.backingFieldDef.name,
                "backingFieldContainer" to instruction.backingFieldContainer.name,
                "condition" to toJsonMap(instruction.condition),
                "virtualTypeContext" to toJsonMap(instruction.virtualTypeContext),
                "batchSize" to instruction.batchSize,
                "batchHydrationMatchStrategy" to toJsonMap(instruction.batchHydrationMatchStrategy),
            )
            is NadelDeepRenameFieldInstruction -> mapOf(
                "location" to instruction.location.toString(),
                "queryPathToField" to instruction.queryPathToField.toNiceString(),
            )
            is NadelHydrationFieldInstruction -> mapOf(
                "location" to instruction.location.toString(),
                "virtualFieldDef" to instruction.virtualFieldDef.name,
                "backingService" to instruction.backingService.name,
                "queryPathToBackingField" to instruction.queryPathToBackingField.toNiceString(),
                "backingFieldArguments" to instruction.backingFieldArguments.sortedBy { it.name }.map(::toJsonMap),
                "timeout" to instruction.timeout,
                "sourceFields" to instruction.sourceFields.map(NadelQueryPath::toNiceString).sorted(),
                "backingFieldDef" to instruction.backingFieldDef.name,
                "backingFieldContainer" to instruction.backingFieldContainer.name,
                "condition" to toJsonMap(instruction.condition),
                "virtualTypeContext" to toJsonMap(instruction.virtualTypeContext),
                "hydrationStrategy" to toJsonMap(instruction.hydrationStrategy),
            )
            is NadelRenameFieldInstruction -> mapOf(
                "location" to instruction.location.toString(),
                "underlyingName" to instruction.underlyingName,
            )
        }
    }

    private fun toJsonMap(hydrationStrategy: NadelHydrationStrategy): JsonMap {
        return when (hydrationStrategy) {
            is NadelHydrationStrategy.ManyToOne -> mapOf(
                "type" to "OneToOne",
                "inputDefToSplit" to hydrationStrategy.inputDefToSplit.name,
            )
            is NadelHydrationStrategy.OneToOne -> mapOf(
                "type" to "OneToOne",
            )
        }
    }

    private fun toJsonMap(batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy): JsonMap {
        return when (batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> mapOf(
                "type" to "MatchIndex",
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> mapOf(
                "type" to "MatchObjectIdentifier",
                "sourceId" to batchHydrationMatchStrategy.sourceId,
                "resultId" to batchHydrationMatchStrategy.resultId,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> mapOf(
                "type" to "MatchObjectIdentifiers",
                "objectIds" to batchHydrationMatchStrategy.objectIds
                    .sortedWith(
                        compareBy(
                            { it.sourceId.toNiceString() },
                            { it.resultId },
                        ),
                    )
                    .map { objectId ->
                        mapOf(
                            "sourceId" to objectId.sourceId,
                            "resultId" to objectId.resultId,
                        )
                    },
            )
        }
    }

    private fun toJsonMap(virtualTypeContext: NadelVirtualTypeContext?): JsonMap? {
        virtualTypeContext ?: return null

        return mapOf(
            "virtualFieldContainer" to virtualTypeContext.virtualFieldContainer.name,
            "virtualField" to virtualTypeContext.virtualField.name,
            "virtualTypeToBackingType" to virtualTypeContext.virtualTypeToBackingType,
            "backingTypeToVirtualType" to virtualTypeContext.backingTypeToVirtualType,
        )
    }

    private fun toJsonMap(condition: NadelHydrationCondition?): JsonMap? {
        return when (condition) {
            is NadelHydrationCondition.StringResultEquals -> mapOf(
                "type" to "StringResultEquals",
                "fieldPath" to condition.fieldPath.toNiceString(),
                "value" to condition.value,
            )
            is NadelHydrationCondition.LongResultEquals -> mapOf(
                "type" to "LongResultEquals",
                "fieldPath" to condition.fieldPath.toNiceString(),
                "value" to condition.value,
            )
            is NadelHydrationCondition.StringResultMatches -> mapOf(
                "type" to "StringResultMatches",
                "fieldPath" to condition.fieldPath.toNiceString(),
                "regex" to condition.regex,
            )
            is NadelHydrationCondition.StringResultStartsWith -> mapOf(
                "type" to "StringResultStartsWith",
                "fieldPath" to condition.fieldPath.toNiceString(),
                "prefix" to condition.prefix,
            )
            null -> null
        }
    }

    private fun toJsonMap(element: NadelHydrationArgument): JsonMap {
        return mapOf(
            "name" to element.name,
            "backingArgumentDef" to element.backingArgumentDef.name,
            "valueSource" to toJsonMap(element.valueSource),
        )
    }

    private fun toJsonMap(valueSource: NadelHydrationArgument.ValueSource): JsonMap {
        return when (valueSource) {
            is NadelHydrationArgument.ValueSource.ArgumentValue -> mapOf(
                "type" to "ArgumentValue",
                "argumentName" to valueSource.argumentName,
                // todo: maybe include source location? that might help with overall / underlying in case it's checking the wrong one
                "argumentDefinition" to valueSource.argumentDefinition.name,
                "defaultValue" to valueSource.defaultValue,
            )
            is NadelHydrationArgument.ValueSource.FieldResultValue -> mapOf(
                "type" to "FieldResultValue",
                "queryPathToField" to valueSource.queryPathToField.toNiceString(),
                "fieldDefinition" to valueSource.fieldDefinition.name,
            )
            is NadelHydrationArgument.ValueSource.StaticValue -> mapOf(
                "type" to "StaticValue",
            )
            is NadelHydrationArgument.ValueSource.RemainingArguments -> mapOf(
                "type" to "RemainingArguments",
                "remainingArgumentNames" to valueSource.remainingArgumentNames,
            )
        }
    }

    @JvmName("Service_Set_String_toJsonMap")
    private fun toJsonMap(element: Map<Service, Set<String>>): JsonMap {
        return element
            .asSequence()
            .map { (service, set) ->
                service.name to set.toList().sorted()
            }
            .toMapStrictly()
            .toSortedMap()
    }

    @JvmName("String_NadelUnderlyingExecutionBlueprint_toJsonMap")
    private fun toJsonMap(element: Map<String, NadelUnderlyingExecutionBlueprint>): JsonMap {
        return mapOf(

        )
    }

    @JvmName("FieldCoordinates_Service_toJsonMap")
    private fun toJsonMap(element: Map<FieldCoordinates, Service>): JsonMap {
        return element
            .asSequence()
            .map { (coords, service) ->
                coords.toString() to service.name
            }
            .toMapStrictly()
            .toSortedMap()
    }

    @JvmName("String_NadelTypeRenameInstruction_toJsonMap")
    private fun toJsonMap(element: Map<String, NadelTypeRenameInstruction>): JsonMap {
        return element
            .asSequence()
            .map { (string, instruction) ->
                string to toJsonMap(instruction)
            }
            .toMapStrictly()
            .toSortedMap()
    }

    private fun toJsonMap(instruction: NadelTypeRenameInstruction): JsonMap {
        return mapOf(
            "service" to instruction.service.name,
            "overallName" to instruction.overallName,
            "underlyingName" to instruction.underlyingName,
        )
    }
}
