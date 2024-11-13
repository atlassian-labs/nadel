package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil

private fun getHydrationErrorMessage(
    parentType: NadelServiceSchemaElement,
    virtualField: GraphQLFieldDefinition,
    hydration: NadelHydrationDefinition,
    reason: String,
): String {
    val parentTypeName = parentType.overall.name
    val fieldName = virtualField.name
    val backingField = hydration.backingField.joinToString(separator = ".")
    return "Field $parentTypeName.$fieldName tried to hydrate from Query.$backingField but $reason"
}

data class NadelBatchHydrationArgumentInvalidSourceInputError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val hydrationArgument: NadelHydrationArgumentDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val argName = hydrationArgument.backingFieldArgumentName
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "batch argument $argName must be a 1 dimensional list",
        )
    }

    override val subject = virtualField
}

data class NadelBatchHydrationArgumentMissingSourceFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = getHydrationErrorMessage(
        parentType,
        virtualField,
        hydration,
        reason = "is missing one \$source argument",
    )

    override val subject = virtualField
}

data class NadelBatchHydrationArgumentMultipleSourceFieldsError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = getHydrationErrorMessage(
        parentType,
        virtualField,
        hydration,
        reason = "must not declare multiple \$source arguments",
    )

    override val subject = virtualField
}

data class NadelBatchHydrationMatchingStrategyInvalidSourceIdError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val offendingObjectIdentifier: NadelBatchObjectIdentifiedByDefinition,
) : NadelSchemaValidationError {
    override val message = run {
        val inputIdentifiedBy = NadelHydrationDefinition.Keyword.inputIdentifiedBy
        val sourceId = NadelBatchObjectIdentifiedByDefinition.Keyword.sourceId

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "$inputIdentifiedBy must specify a $sourceId that is a child of the \$source field",
        )
    }

    override val subject = virtualField
}

data class NadelBatchHydrationMatchingStrategyReferencesNonExistentSourceFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToNonExistentSourceField: List<String>,
) : NadelSchemaValidationError {
    override val message = run {
        val inputIdentifiedBy = NadelHydrationDefinition.Keyword.inputIdentifiedBy
        val sourceId = NadelBatchObjectIdentifiedByDefinition.Keyword.sourceId
        val parentTypeName = parentType.underlying.name
        val field = pathToNonExistentSourceField.joinToString(separator = ".")

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "$inputIdentifiedBy references non existent $sourceId in underlying schema $parentTypeName.${field}",
        )
    }

    override val subject = virtualField
}

data class NadelBatchHydrationMissingIdentifiedByError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
) : NadelSchemaValidationError {
    override val message = getHydrationErrorMessage(
        parentType,
        virtualField,
        hydration,
        reason = "did not specify identifiedBy",
    )

    override val subject = virtualField
}

data class NadelHydrationArgumentDuplicatedError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val duplicates: List<NadelHydrationArgumentDefinition>,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val duplicateNames = duplicates.joinToString(separator = ",", prefix = "[", postfix = "]") {
            it.backingFieldArgumentName
        }
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "must not declare duplicate arguments $duplicateNames",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationArgumentIncompatibleTypeError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val hydrationArgument: NadelHydrationArgumentDefinition,
    val suppliedType: GraphQLType,
    val requiredType: GraphQLType,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val argName = hydrationArgument.backingFieldArgumentName
        val suppliedType = GraphQLTypeUtil.simplePrint(suppliedType)
        val requiredType = GraphQLTypeUtil.simplePrint(requiredType)
        getHydrationErrorMessage(
            parentType = parentType,
            virtualField = virtualField,
            hydration = hydration,
            reason = "$suppliedType cannot be assigned to $requiredType for argument $argName",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationArgumentMissingRequiredInputObjectFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val hydrationArgument: NadelHydrationArgumentDefinition,
    val suppliedFieldContainer: GraphQLFieldsContainer,
    val requiredFieldContainer: GraphQLInputFieldsContainer,
    val requiredField: GraphQLInputObjectField,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val argName = hydrationArgument.backingFieldArgumentName
        val suppliedType = suppliedFieldContainer.name
        val requiredType = requiredFieldContainer.name
        val requiredField = requiredField.name
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "argument $argName tries to assign $suppliedType to $requiredType but $suppliedType is missing field $requiredField",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationArgumentReferencesNonExistentArgumentError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val argument: NadelHydrationArgumentDefinition.VirtualFieldArgument,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val remoteArgName = argument.backingFieldArgumentName
        val virtualArgName = argument.virtualFieldArgumentName
        getHydrationErrorMessage(
            parentType = parentType,
            virtualField = virtualField,
            hydration = hydration,
            reason = "argument $remoteArgName references non-existent \$argument.$virtualArgName",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationArgumentReferencesNonExistentFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val argument: NadelHydrationArgumentDefinition.SourceField,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val remoteArgName = argument.backingFieldArgumentName
        val sourceField = argument.pathToSourceField.joinToString(separator = ".")
        getHydrationErrorMessage(
            parentType = parentType,
            virtualField = virtualField,
            hydration = hydration,
            reason = "argument $remoteArgName references non-existent underlying field at \$source.$sourceField",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationCannotSqueezeSourceListError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val sourceField: NadelHydrationArgumentDefinition.SourceField,
) : NadelSchemaValidationError {
    override val message = run {
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "the \$source field is a list and the virtual field is not",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationConditionIncompatibleValueError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToConditionField: List<String>,
    val requiredType: GraphQLType,
    val suppliedValue: Any?,
) : NadelSchemaValidationError {
    override val message = run {
        val parentTypeName = parentType.overall.name
        val conditionField = pathToConditionField.joinToString(separator = ".")
        val suppliedType = suppliedValue?.javaClass?.name
        val requiredType = GraphQLTypeUtil.simplePrint(requiredType)
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "condition cannot assign $suppliedType to $requiredType defined by $parentTypeName.$conditionField",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationConditionInvalidRegexError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val regexString: String,
) : NadelSchemaValidationError {
    override val message = getHydrationErrorMessage(
        parentType,
        virtualField,
        hydration,
        reason = "the condition regex $regexString is not valid",
    )

    override val subject = virtualField
}

data class NadelHydrationConditionMatchesPredicateRequiresStringFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToConditionField: List<String>,
) : NadelSchemaValidationError {
    override val message = run {
        val parentTypeName = parentType.overall.name
        val conditionField = pathToConditionField.joinToString(separator = ".")
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "condition uses String.matches(Regex) predicate but $parentTypeName.$conditionField is not a String",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationConditionStartsWithPredicateRequiresStringFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToConditionField: List<String>,
) : NadelSchemaValidationError {
    override val message = run {
        val parentTypeName = parentType.overall.name
        val conditionField = pathToConditionField.joinToString(separator = ".")
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "condition uses String.startsWith predicate but $parentTypeName.$conditionField is not a String",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationIncompatibleInputObjectFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val hydrationArgument: NadelHydrationArgumentDefinition,
    val suppliedFieldContainer: GraphQLFieldsContainer,
    val suppliedField: GraphQLFieldDefinition,
    val requiredFieldContainer: GraphQLInputFieldsContainer,
    val requiredField: GraphQLInputObjectField,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val argName = hydrationArgument.backingFieldArgumentName

        val suppliedType = GraphQLTypeUtil.simplePrint(suppliedField.type)
        val suppliedField = "${suppliedFieldContainer.name}.${suppliedField.name}"

        val requiredType = GraphQLTypeUtil.simplePrint(requiredField.type)
        val requiredField = "${requiredFieldContainer.name}.${requiredField.name}"

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "argument $argName cannot assign field $suppliedField to $requiredField as $suppliedType cannot be assigned to $requiredType",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationIncompatibleOutputTypeError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val backingField: GraphQLFieldDefinition,
    val incompatibleOutputType: GraphQLNamedOutputType,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val virtualFieldType = GraphQLTypeUtil.simplePrint(virtualField.type)
        val backingFieldType = GraphQLTypeUtil.simplePrint(backingField.type)

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "$backingFieldType cannot be assigned to $virtualFieldType",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationMissingRequiredBackingFieldArgumentError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val missingBackingArgument: GraphQLArgument,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val requiredArgName = missingBackingArgument.name
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "does not supply value for required argument $requiredArgName",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationMustAllHaveConditionError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message = run {
        val parentTypeName = parentType.overall.name
        val virtualFieldName = virtualField.name
        "Field $parentTypeName.$virtualFieldName must supply conditions for all hydrations"
    }

    override val subject = virtualField
}

data class NadelHydrationMustUseIndexExclusivelyError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val coords = "${parentType.overall.name}.${virtualField.name}"
        "Field $coords cannot use both indexed hydration and non-indexed hydration"
    }

    override val subject = virtualField
}

data class NadelHydrationReferencesNonExistentBackingArgumentError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val argument: String,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = getHydrationErrorMessage(
        parentType = parentType,
        virtualField = virtualField,
        hydration = hydration,
        reason = "references non existent backing argument $argument",
    )

    override val subject = virtualField
}

data class NadelHydrationReferencesNonExistentBackingFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = getHydrationErrorMessage(
        parentType = parentType,
        virtualField = virtualField,
        hydration = hydration,
        reason = "that field does not exist",
    )

    override val subject = virtualField
}

data class NadelHydrationResultConditionReferencesNonExistentFieldError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToConditionField: List<String>,
) : NadelSchemaValidationError {
    override val message = run {
        val parentTypeName = parentType.overall.name
        val conditionField = pathToConditionField.joinToString(separator = ".")
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "condition references non existent field $parentTypeName.$conditionField",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationResultConditionUnsupportedFieldTypeError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val pathToConditionField: List<String>,
    val conditionField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message = run {
        val conditionFieldType = GraphQLTypeUtil.simplePrint(conditionField.type)
        val conditionField = pathToConditionField.joinToString(separator = ".")
        val str = Scalars.GraphQLString.name
        val int = Scalars.GraphQLInt.name
        val id = Scalars.GraphQLID.name
        val parentTypeName = parentType.overall.name

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "condition field $parentTypeName.$conditionField must to be of type $str, $int or $id but is $conditionFieldType",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationTypeMismatchError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message: String = run {
        val parentTypeName = parentType.overall.name
        val virtualFieldName = virtualField.name
        "Field $parentTypeName.$virtualFieldName must not use a mix of batched and non-batched @hydrated"
    }

    override val subject = virtualField
}

data class NadelHydrationVirtualFieldMustBeNullableError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val virtualField = "${parentType.overall.name}.${virtualField.name}"
        "Field $virtualField declares a hydration so its output type MUST be nullable"
    }

    override val subject = virtualField
}

data class NadelPolymorphicHydrationIncompatibleSourceFieldsError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val virtualField = "${parentType.overall.name}.${virtualField.name}"
        "Field $virtualField has multiple @hydrated definitions and some argument \$source fields are lists and some are not"
    }

    override val subject = virtualField
}

data class NadelPolymorphicHydrationMustOutputUnionError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val virtualField = "${parentType.overall.name}.${virtualField.name}"
        "Field $virtualField has multiple @hydrated definitions so its output type MUST be a union"
    }

    override val subject = virtualField
}
