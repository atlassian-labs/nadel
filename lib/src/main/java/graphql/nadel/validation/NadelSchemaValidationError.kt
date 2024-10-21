package graphql.nadel.validation

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.language.InputValueDefinition
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil
import graphql.schema.GraphQLUnionType

private class NadelSchemaValidationErrorClassification(
    private val type: NadelSchemaValidationError,
) : ErrorClassification {
    override fun toSpecification(error: GraphQLError?): Any {
        return type.javaClass.simpleName
    }
}

sealed interface NadelSchemaValidationError {
    /**
     * Human-readable message associated with the error
     * e.g. could not find underlying type for overall type Foo in service Bar
     */
    val message: String

    /**
     * What schema element the error is associated with.
     *
     * e.g. if the error message is could not find underlying type for overall type Foo in service Bar
     * then the [subject] is the `Foo` type.
     *
     * e.g. if the error message is could not find overall field Foo.id on the underlying type Foo in service Bar
     * then the [subject] is the `id` field.
     *
     * You can also think of this as where the error message should show up in a text editor.
     */
    val subject: GraphQLNamedSchemaElement

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This is only here for backwards compatability purposes")
    fun toGraphQLError(): GraphQLError {
        return GraphqlErrorBuilder.newError()
            .message(message)
            .errorType(NadelSchemaValidationErrorClassification(this))
            .build()
    }

    data class UnionHasExtraType(
        val service: Service,
        val unionType: GraphQLUnionType,
        val extraType: GraphQLObjectType,
    ) : NadelSchemaValidationError {
        override val message = run {
            val ut = unionType.name
            val s = service.name
            val et = extraType.name
            "Union $ut in service $s has possible type $et but underlying schema does not"
        }

        override val subject = unionType
    }

    data class MissingUnderlyingType(
        val service: Service,
        val overallType: GraphQLNamedType,
    ) : NadelSchemaValidationError {
        override val message = run {
            val t = overallType.name
            val s = service.name
            "Could not find underlying type for overall type $t in service $s"
        }

        override val subject = overallType
    }

    data class MissingConcreteTypes(
        val interfaceType: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        override val message = run {
            val t = interfaceType.overall.name
            val s = interfaceType.service.name
            "Service $s does not define any concrete implementations for interface $t"
        }

        override val subject = interfaceType.overall
    }

    data class DuplicatedUnderlyingType(
        val duplicates: List<NadelServiceSchemaElement>,
    ) : NadelSchemaValidationError {
        val service: Service get() = duplicates.first().service

        override val message = run {
            val ot = duplicates.map { it.overall.name }
            val ut = duplicates.first().underlying.name
            val s = service.name
            "Underlying type $ut was duplicated by types $ot in the service $s"
        }

        override val subject = duplicates.first().overall
    }

    data class IncompatibleFieldOutputType(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val underlyingField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val s = service.name
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = makeFieldCoordinates(parentType.underlying.name, underlyingField.name)
            val ot = GraphQLTypeUtil.simplePrint(overallField.type)
            val ut = GraphQLTypeUtil.simplePrint(underlyingField.type)
            "Overall field $of has output type $ot but underlying field $uf in service $s has output type $ut"
        }

        override val subject = overallField
    }

    data class MissingUnderlyingField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall field $of on the underlying type $ut on service $s"
        }

        override val subject = overallField
    }

    data class MissingUnderlyingInputField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLInputObjectField,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall input field $of on the underlying input type $ut on service $s"
        }

        override val subject = overallField
    }

    data class IncompatibleFieldInputType(
        val parentType: NadelServiceSchemaElement,
        val overallInputField: GraphQLInputObjectField,
        val underlyingInputField: GraphQLInputObjectField,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val s = service.name
            val of = makeFieldCoordinates(parentType.overall.name, overallInputField.name)
            val uf = makeFieldCoordinates(parentType.underlying.name, underlyingInputField.name)
            val ot = GraphQLTypeUtil.simplePrint(overallInputField.type)
            val ut = GraphQLTypeUtil.simplePrint(underlyingInputField.type)
            "Overall field $of has input type $ot but underlying field $uf in service $s has input type $ut"
        }

        override val subject = overallInputField
    }

    data class IncompatibleArgumentInputType(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val overallInputArg: GraphQLArgument,
        val underlyingInputArg: GraphQLArgument,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val s = service.name
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val ofa = makeFieldCoordinates(parentType.overall.name, overallInputArg.name)
            val ufa = makeFieldCoordinates(parentType.underlying.name, underlyingInputArg.name)
            val ot = GraphQLTypeUtil.simplePrint(overallInputArg.type)
            val ut = GraphQLTypeUtil.simplePrint(underlyingInputArg.type)
            "Overall field $of has argument $ofa has input type $ot but underlying field argument $ufa in service $s has input type $ut"
        }

        override val subject = overallInputArg
    }

    data class MissingUnderlyingEnumValue(
        val parentType: NadelServiceSchemaElement,
        val overallValue: GraphQLEnumValueDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallValue.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall enum value $of on the underlying enum type $ut on service $s"
        }

        override val subject = overallValue
    }

    data class MissingHydrationBackingField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: NadelHydrationDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val af = hydration.backingField.joinToString(separator = ".")
            "Field $of tried to hydrate from non-existent field Query.$af"
        }

        override val subject = overallField
    }

    data class HydrationFieldMustBeNullable(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $of declares a hydration so its output type MUST be nullable"
        }

        override val subject = overallField
    }

    data class HydrationIncompatibleOutputType(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val backingField: GraphQLFieldDefinition,
        val incompatibleOutputType: GraphQLNamedOutputType,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val bf = makeFieldCoordinates(parentType.overall.name, backingField.name)
            val at = incompatibleOutputType.unwrapAll().name
            val ot = overallField.type.unwrapAll().name
            "Field $of tries to hydrate from $bf but $bf returns $at which cannot be assigned to with $ot"
        }

        override val subject = overallField
    }

    data class FieldWithPolymorphicHydrationMustReturnAUnion(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $of declares a polymorphic hydration so its output type MUST be a union"
        }

        override val subject = overallField
    }

    data class MissingHydrationFieldValueSource(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: NadelHydrationArgumentDefinition.ValueSource.ObjectField,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${remoteArgSource.pathToField.joinToString(separator = ".")}"
            val s = service.name
            "Field $of tried to hydrate using value of non-existent underlying field $uf from service $s as an argument"
        }

        override val subject = overallField
    }

    data class MissingHydrationArgumentValueSource(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: NadelHydrationArgumentDefinition.ValueSource.FieldArgument,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val a = remoteArgSource.argumentName
            "Field $of tried to hydrate using value of non-existent field $a as an argument"
        }

        override val subject = overallField
    }

    data class NonExistentHydrationBackingFieldArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: NadelHydrationDefinition,
        val argument: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val af = hydration.backingField.joinToString(separator = ".")
            "Hydration on field $of references non-existent argument $argument on backing field Query.$af"
        }

        override val subject = overallField
    }

    data class IncompatibleHydrationArgumentType(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArg: NadelHydrationArgumentDefinition,
        val hydrationType: GraphQLType,
        val backingArgInputType: GraphQLType,
        val backingFieldName: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val ht = GraphQLTypeUtil.simplePrint(hydrationType)
            val at = GraphQLTypeUtil.simplePrint(backingArgInputType)
            "Hydration cannot assign type $ht to $at on argument ${remoteArg.name}"
        }

        override val subject = overallField
    }

    data class MixedIndexHydration(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val coords = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $coords uses both indexed hydration and non-indexed hydration"
        }

        override val subject = overallField
    }

    data class MultipleHydrationSourceInputFields(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val coords = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $coords uses multiple \$source fields"
        }

        override val subject = overallField
    }

    data class StaticArgIsNotAssignable(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArg: NadelHydrationArgumentDefinition,
        val backingArgInputType: GraphQLType,
        val backingFieldName: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val hydrationArgName = remoteArg.name
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val at = GraphQLTypeUtil.simplePrint(backingArgInputType)

            "Field $of tried to hydrate using backing field \"$backingFieldName\". " +
                " However, the type of the static argument you are supplying backing field arg \"$hydrationArgName\" with " +
                "is not assignable to the expected type $at"
        }

        override val subject = overallField
    }

    data class HydrationConditionUnsupportedFieldType(
        val pathToSourceField: List<String>,
        val sourceFieldTypeName: String,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message =
            "Hydration condition field \"${pathToSourceField.joinToString(separator = ".")}\" needs to be of type String, Int or ID"
        override val subject = overallField
    }

    data class HydrationConditionSourceFieldDoesNotExist(
        val pathToSourceField: List<String>,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message =
            "Hydration condition field \"${pathToSourceField.joinToString(separator = ".")}\" does not exist "
        override val subject = overallField
    }

    data class HydrationConditionPredicateDoesNotMatchSourceFieldType(
        val pathToSourceField: List<String>,
        val sourceFieldTypeName: String,
        val predicateTypeName: String,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message =
            "Hydration condition field \"${pathToSourceField.joinToString(separator = ".")}\" of type \"${sourceFieldTypeName}\" " +
                "does not match the predicate type \"$predicateTypeName\" in the when condition"
        override val subject = overallField
    }

    data class HydrationConditionPredicateRequiresStringSourceField(
        val pathToSourceField: List<String>,
        val sourceFieldTypeName: String,
        val predicateType: String,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message =
            "Hydration condition field \"${pathToSourceField.joinToString(separator = ".")}\" " +
                "needs to be of type String or ID in order to use the \"$predicateType\" predicate."
        override val subject = overallField
    }

    data class ConditionalHydrationPredicateContainsInvalidRegex(
        val regexString: String,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message =
            "Conditional hydration is using invalid regex \"$regexString\"."
        override val subject = overallField
    }

    data class SomeHydrationsHaveMissingConditions(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Some hydrations of field $of are missing a when condition"
        }
        override val subject = overallField
    }

    data class IncompatibleFieldInHydratedInputObject(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArg: NadelHydrationArgumentDefinition,
        val backingFieldName: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val hydrationArgName = remoteArg.name
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val pathToField = (remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField).pathToField
            val remoteArgSource = "${parentType.underlying.name}.${pathToField.joinToString(separator = ".")}"

            "Field \"$of\" tried to hydrate using the backing field \"$backingFieldName\" and argument \"$hydrationArgName\"." +
                " However, you are supplying backing field argument with the value from $remoteArgSource " +
                "and the types are incompatible"
        }

        override val subject = overallField
    }

    data class MissingFieldInHydratedInputObject(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArg: NadelHydrationArgumentDefinition,
        val missingFieldName: String,
        val backingFieldName: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val hydrationArgName = remoteArg.name
            val pathToField = (remoteArg.value as NadelHydrationArgumentDefinition.ValueSource.ObjectField).pathToField
            val remoteArgSource = "${parentType.underlying.name}.${pathToField.joinToString(separator = ".")}"
            val s = service.name
            "Field $of tried to hydrate using field \"$backingFieldName\" with argument \"$hydrationArgName\" using value from $remoteArgSource in service $s" +
                " but it was missing the required field $missingFieldName"
        }

        override val subject = overallField
    }

    data class MissingRequiredHydrationBackingFieldArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: NadelHydrationDefinition,
        val argument: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val af = hydration.backingField.joinToString(separator = ".")
            "Hydration on field $of is missing the required argument $argument on hydration backing Query.$af"
        }

        override val subject = overallField
    }

    data class MissingRename(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val rename: NadelRenamedDefinition.Field,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${rename.rawFrom}"
            val s = service.name
            "Overall field $of defines rename but underlying field $uf on service $s doesn't exist"
        }

        override val subject = overallField
    }

    data class CannotRenameHydratedField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Overall field $of tried to rename a hydrated field"
        }

        override val subject = overallField
    }

    data class MultipleSourceArgsInBatchHydration(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val fieldCoordinates = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Multiple \$source.xxx arguments are not supported for batch hydration. Field: $fieldCoordinates"
        }

        override val subject = overallField
    }

    data class NoSourceArgsInBatchHydration(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val fieldCoordinates = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "No \$source.xxx arguments for batch hydration. Field: $fieldCoordinates"
        }

        override val subject = overallField
    }

    data class MissingArgumentOnUnderlying(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val underlyingField: GraphQLFieldDefinition,
        val argument: GraphQLArgument,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val a = argument.name
            val s = service.name
            val uf = makeFieldCoordinates(parentType.underlying.name, underlyingField.name)
            "The overall field $of defines argument $a which does not exist in service $s field $uf"
        }

        override val subject = overallField
    }

    data class IncompatibleType(
        val schemaElement: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        val service: Service get() = schemaElement.service

        override val message = run {
            val s = schemaElement.service.name
            val o = toString(schemaElement.overall)
            val u = toString(schemaElement.underlying)
            "Overall type kind of $o in service $s does not match underlying type kind $u"
        }

        override val subject = schemaElement.overall
    }

    data class IncompatibleTypeName(
        val schemaElement: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        val service: Service get() = schemaElement.service

        override val message = run {
            val s = schemaElement.service.name
            val o = toString(schemaElement.overall)
            val u = toString(schemaElement.underlying)
            "Type name of overall type $o in service $s does not match underlying type name $u"
        }

        override val subject = schemaElement.overall
    }

    data class MissingDirectiveArgument(
        val parent: NadelServiceSchemaElement? = null,
        val location: GraphQLNamedSchemaElement,
        val directive: GraphQLDirective,
        val missing: InputValueDefinition,
    ) : NadelSchemaValidationError {
        override val message = run {
            val l = when (location) {
                is GraphQLFieldDefinition -> when (parent) {
                    null -> "field ${location.name}"
                    else -> "field ${makeFieldCoordinates(parent.overall.name, location.name)}"
                }

                is GraphQLType -> "type ${location.name}"
                else -> "${location.javaClass.simpleName} ${location.name}"
            }
            val d = directive.name
            val a = missing.name

            "Directive on $l has directive $d which is missing argument $a"
        }

        override val subject = location
    }

    data class DuplicatedHydrationArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val duplicates: List<NadelHydrationArgumentDefinition>,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val an = duplicates.map { it.name }.toSet().single()
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Hydration at $of provides multiple possible values for argument $an"
        }

        override val subject = overallField
    }

    data class HydrationsMismatch(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Hydrations declared on field $of cannot use a mix of batched and non-batched hydrations"
        }

        override val subject = overallField
    }

    data class NamespacedTypeMustBeObject(
        val type: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        val service: Service get() = type.service

        override val message: String = run {
            val tn = type.overall.name
            "Namespaced type $tn must be an object type"
        }

        override val subject = type.overall
    }

    data class ObjectIdentifierMustFollowSourceInputField(
        val type: NadelServiceSchemaElement,
        val field: GraphQLFieldDefinition,
        val pathToSourceInputField: List<String>,
        val offendingObjectIdentifier: NadelBatchObjectIdentifiedByDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = type.service

        override val message: String = run {
            val offender = offendingObjectIdentifier.sourceId
            val prefix = pathToSourceInputField.joinToString(".")
            "Source input field $prefix must be prefix of object identifier $offender"
        }

        override val subject = field
    }
}

private fun toString(element: GraphQLNamedSchemaElement): String {
    return "${element.javaClass.simpleName}(name=${element.name})"
}
