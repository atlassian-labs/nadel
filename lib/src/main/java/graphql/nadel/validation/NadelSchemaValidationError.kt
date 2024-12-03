package graphql.nadel.validation

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.schema.NadelDirectives
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
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

sealed interface NadelSchemaValidationError : NadelSchemaValidationResult {
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

    /**
     * Do not change. Just an indicator to [NadelSchemaValidationResult] that this is an error.
     */
    override val isError: Boolean
        get() = true

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This is only here for backwards compatibility purposes")
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

    data class StaticArgIsNotAssignable(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: NadelHydrationDefinition,
        val hydrationArgument: NadelHydrationArgumentDefinition.StaticArgument,
        val requiredArgumentType: GraphQLType,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val vf = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val bf = hydration.backingField.joinToString(separator = ".")
            val an = hydrationArgument.name
            "Field $vf tried to hydrate Query.$bf but gave invalid static value for argument $an"
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

    data class CannotRenamePartitionedField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Overall field $of tried to partition a renamed field"
        }

        override val subject = overallField
    }

    data class PartitionAppliedToUnsupportedField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Directive '${NadelDirectives.partitionDirectiveDefinition.name}' is declared on a field " +
                "'${of}' inside a type that is not an operation or namespace type"
        }

        override val subject = overallField
    }

    data class PartitionAppliedToSubscriptionField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Directive '${NadelDirectives.partitionDirectiveDefinition.name}' is declared on a field " +
                "'${of}' inside the Subscription type"
        }

        override val subject = overallField
    }

    data class CannotPartitionHydratedField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Overall field $of tried to partition a hydrated field"
        }

        override val subject = overallField
    }

    data class PartitionAppliedToFieldWithUnsupportedOutputType(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Directive '${NadelDirectives.partitionDirectiveDefinition.name}' is declared on a field " +
                "'${of}' with an unsupported output type. Only lists and mutation payloads are supported"
        }

        override val subject = overallField
    }

    data class InvalidPartitionArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Directive '${NadelDirectives.partitionDirectiveDefinition.name}' applied to field " +
                "'${of}' is using wrong type for the partition argument. Only lists are supported"
        }

        override val subject = overallField
    }

    data class AllFieldsUsingHiddenDirective(
        val parentType: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find any fields without @hidden directive on the underlying type $ut on service $s"
        }

        override val subject = parentType.overall
    }
}

private fun toString(element: GraphQLNamedSchemaElement): String {
    return "${element.javaClass.simpleName}(name=${element.name})"
}
