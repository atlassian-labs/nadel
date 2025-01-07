package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDefinition
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
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
        val argName = hydrationArgument.name
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
        val duplicateNames = duplicates.joinToString(separator = ",", prefix = "[", postfix = "]") { it.name }
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
        val argName = hydrationArgument.name
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
        val a = hydrationArgument.name
        val sfc = suppliedFieldContainer.name
        val rfc = requiredFieldContainer.name
        val rf = requiredField.name
        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "argument $a tries to assign $sfc to $rfc but $sfc is missing field $rf",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationArgumentReferencesNonExistentArgumentError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val argument: NadelHydrationArgumentDefinition.FieldArgument,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val remoteArgName = argument.name
        val virtualArgName = argument.argumentName
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
    val argument: NadelHydrationArgumentDefinition.ObjectField,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val remoteArgName = argument.name
        val sf = argument.pathToField.joinToString(separator = ".")
        getHydrationErrorMessage(
            parentType = parentType,
            virtualField = virtualField,
            hydration = hydration,
            reason = "argument $remoteArgName references non-existent underlying field at \$source.$sf",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationCannotSqueezeSourceListError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val sourceField: NadelHydrationArgumentDefinition.ObjectField,
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
        val a = hydrationArgument.name

        val sfc = suppliedFieldContainer.name
        val sf = suppliedField.name
        val sft = GraphQLTypeUtil.simplePrint(suppliedField.type)

        val rfc = requiredFieldContainer.name
        val rf = requiredField.name
        val rft = GraphQLTypeUtil.simplePrint(requiredField.type)

        getHydrationErrorMessage(
            parentType,
            virtualField,
            hydration,
            reason = "argument $a has type mismatch from field $sfc.$sf to $rfc.$rf as $sft cannot be assigned to $rft",
        )
    }

    override val subject = virtualField
}

data class NadelHydrationIncompatibleOutputTypeError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val backingField: GraphQLFieldDefinition,
    val incompatibleOutputType: GraphQLNamedOutputType,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val vf = makeFieldCoordinates(parentType.overall.name, virtualField.name)
        val bf = makeFieldCoordinates(parentType.overall.name, backingField.name)
        val at = incompatibleOutputType.unwrapAll().name
        val vt = virtualField.type.unwrapAll().name
        "Field $vf tries to hydrate from $bf but $bf returns $at which cannot be assigned to with $vt"
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
        val coords = makeFieldCoordinates(parentType.overall.name, virtualField.name)
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
        val vf = makeFieldCoordinates(parentType.overall.name, virtualField.name)
        "Field $vf declares a hydration so its output type MUST be nullable"
    }

    override val subject = virtualField
}

data class NadelMissingDefaultHydrationError(
    val parentType: GraphQLFieldsContainer,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message = run {
        val backingType = virtualField.type.unwrapAll().name
        val virtualField = makeFieldCoordinates(parentType.name, virtualField.name)
        val defaultHydration = NadelDefaultHydrationDefinition.directiveDefinition.name
        val idHydrated = NadelIdHydrationDefinition.directiveDefinition.name
        "Field $virtualField tried to use @$idHydrated but type $backingType does not specify @$defaultHydration"
    }

    override val subject = virtualField
}

data class NadelPolymorphicHydrationIncompatibleSourceFieldsError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val coords = makeFieldCoordinates(parentType.overall.name, virtualField.name)
        "Field $coords has multiple @hydrated definitions that use different source arguments and at least one of them is a list"
    }

    override val subject = virtualField
}

data class NadelPolymorphicHydrationMustOutputUnionError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val vf = makeFieldCoordinates(parentType.overall.name, virtualField.name)
        "Field $vf has multiple @hydrated definitions so its output type MUST be a union"
    }

    override val subject = virtualField
}

data class NadelHydrationUnionMemberNoBackingError(
    val parentType: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val membersNoBacking: List<GraphQLNamedType>,
) : NadelSchemaValidationError {
    val service: Service get() = parentType.service

    override val message = run {
        val vf = makeFieldCoordinates(parentType.overall.name, virtualField.name)
        val memberNamesNoBacking = membersNoBacking.map { it.name }
        "Field $vf is missing hydration(s) for possible union type(s) $memberNamesNoBacking"
    }

    override val subject = virtualField
}
