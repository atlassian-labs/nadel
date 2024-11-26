package graphql.nadel.validation

import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenamePartitionedField
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement

class NadelFieldValidation internal constructor(
    private val hydrationValidation: NadelHydrationValidation,
    private val partitionValidation: NadelPartitionValidation,
    private val assignableTypeValidation: NadelAssignableTypeValidation,
) {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.FieldsContainer,
    ): NadelSchemaValidationResult {
        return validate(
            schemaElement,
            overallFields = schemaElement.overall.fields,
        )
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallFields: List<GraphQLFieldDefinition>,
    ): NadelSchemaValidationResult {
        return overallFields
            .asSequence()
            .let { fieldSequence ->
                // Apply filter if necessary
                if (isCombinedType(type = parent.overall)) {
                    val fieldsThatServiceContributed = getFieldsThatServiceContributed(parent)
                    fieldSequence.filter { it.name in fieldsThatServiceContributed }
                } else {
                    fieldSequence
                }
            }
            .map { overallField ->
                validate(parent, overallField)
            }
            .toResult()
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        return if (isRenamed(parent, overallField)) {
            validateRename(parent, overallField)
        } else if (isHydrated(parent, overallField)) {
            hydrationValidation.validate(parent, overallField)
        } else {
            val underlyingField = parent.underlying.getField(overallField.name)
            if (underlyingField == null) {
                MissingUnderlyingField(parent, overallField = overallField)
            } else {
                validate(
                    parent,
                    overallField = overallField,
                    underlyingField = underlyingField,
                )
            }
        }
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val argumentIssues = overallField.arguments
            .map { overallArg ->
                val underlyingArg = underlyingField.getArgument(overallArg.name)
                if (underlyingArg == null) {
                    MissingArgumentOnUnderlying(parent, overallField, underlyingField, overallArg)
                } else {
                    // Note: the value comes from the user (overall schema)
                    // So we are supplying the overall argument to the underlying argument
                    val isArgumentTypeAssignable = assignableTypeValidation.isTypeAssignable(
                        suppliedType = overallArg.type,
                        requiredType = underlyingArg.type
                    )
                    if (isArgumentTypeAssignable) {
                        ok()
                    } else {
                        IncompatibleArgumentInputType(
                            parentType = parent,
                            overallField = overallField,
                            overallInputArg = overallArg,
                            underlyingInputArg = underlyingArg,
                        )
                    }
                }
            }
            .toResult()

        val outputTypeIssues = validateOutputType(parent, overallField, underlyingField)
        val partitionDirectiveIssues = partitionValidation.validate(parent, overallField)

        return results(argumentIssues, outputTypeIssues, partitionDirectiveIssues)
    }

    context(NadelValidationContext)
    private fun validateRename(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (isHydrated(parent, overallField)) {
            return CannotRenameHydratedField(parent, overallField)
        }

        if (isPartitioned(parent, overallField)) {
            return CannotRenamePartitionedField(parent, overallField)
        }

        val rename = getRenamedOrNull(parent, overallField)
            ?: return ok()

        val underlyingField = parent.underlying.getFieldAt(rename.from)
            ?: return MissingRename(parent, overallField, rename)

        // In theory, this should have .onError { return it } but the schema has too many violations…
        val result = validate(parent, overallField, underlyingField)

        return if (parent is NadelServiceSchemaElement.Object) {
            results(
                result,
                NadelValidatedFieldResult(
                    service = parent.service,
                    fieldInstruction = makeRenameFieldInstruction(parent, overallField, rename)
                ),
            )
        } else {
            result
        }
    }

    context(NadelValidationContext)
    private fun makeRenameFieldInstruction(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        rename: NadelRenamedDefinition.Field,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(
            parentType = parent.overall,
            field = overallField,
        )

        val underlyingName = rename.from.singleOrNull()
            ?: return NadelDeepRenameFieldInstruction(
                location = location,
                queryPathToField = NadelQueryPath(rename.from),
            )

        return NadelRenameFieldInstruction(
            location = location,
            underlyingName = underlyingName,
        )
    }

    context(NadelValidationContext)
    private fun validateOutputType(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        // Note: the value comes from the underlying schema, so we are supplying the underlying field to the overall field
        val isUnderlyingTypeAssignable = assignableTypeValidation.isTypeAssignable(
            suppliedType = underlyingField.type,
            requiredType = overallField.type,
        )

        return if (isUnderlyingTypeAssignable) {
            ok()
        } else {
            results(
                IncompatibleFieldOutputType(parent, overallField, underlyingField),
            )
        }
    }

    context(NadelValidationContext)
    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        return type.name in combinedTypeNames
    }
}
