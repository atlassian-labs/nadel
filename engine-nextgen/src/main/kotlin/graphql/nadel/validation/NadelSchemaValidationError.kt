package graphql.nadel.validation

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.pathToActorField
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType

private class NadelSchemaValidationErrorClassification(
    private val type: NadelSchemaValidationError,
) : ErrorClassification {
    override fun toSpecification(error: GraphQLError?): Any {
        return type.javaClass.simpleName
    }
}

sealed interface NadelSchemaValidationError {
    val message: String

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This is only here for backwards compatability purposes")
    fun toGraphQLError(): GraphQLError {
        return GraphqlErrorBuilder.newError()
            .message(message)
            .errorType(NadelSchemaValidationErrorClassification(this))
            .build()
    }

    data class MissingUnderlyingType(
        val service: Service,
        val overallType: GraphQLNamedType,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val t = overallType.name
            val s = service.name
            "Could not find underlying type for overall type '$t' in $s"
        }
    }

    data class MissingUnderlyingField(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall field $of on the underlying type $ut on service $s"
        }
    }

    data class MissingUnderlyingInputField(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLInputObjectField,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall input field $of on the underlying input type $ut on service $s"
        }
    }

    data class MissingUnderlyingEnum(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallValue: GraphQLEnumValueDefinition,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallValue.name)
            val s = service.name
            val ut = parentType.underlying.name
            "Could not find overall enum value $of on the underlying enum type $ut on service $s"
        }
    }

    data class MissingHydrationActorService(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            "Field $of tried to hydrate from non-existent service $s"
        }
    }

    data class MissingHydrationActorField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
        val actorServiceQueryType: GraphQLObjectType,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"
            "Field $of tried to hydrate from non-existent field $af on service $s"
        }
    }

    data class HydrationFieldMustBeNullable(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $of declares a hydration so its output type MUST be nullable"
        }
    }

    data class MissingHydrationFieldValueSource(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: RemoteArgumentSource,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${remoteArgSource.path.joinToString(separator = ".")}"
            val s = service.name
            "Field $of tried to hydrate using value of non-existent underlying field $uf from service $s as an argument"
        }
    }

    data class MissingHydrationArgumentValueSource(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: RemoteArgumentSource,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val a = remoteArgSource.name
            "Field $of tried to hydrate using value of non-existent field $a as an argument"
        }
    }

    data class MissingHydrationActorFieldArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
        val actorServiceQueryType: GraphQLObjectType,
        val argument: String,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"
            "Hydration on field $of references non-existent argument $argument on hydration actor $s.$af"
        }
    }

    data class MissingRename(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val rename: FieldMappingDefinition,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${rename.inputPath.joinToString(separator = ".")}"
            val s = service.name
            "Overall field $of defines rename but underlying field $uf on service $s doesn't exist"
        }
    }

    data class MissingArgumentOnUnderlying(
        val service: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val underlyingField: GraphQLFieldDefinition,
        val argument: GraphQLArgument,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val a = argument.name
            val s = service.name
            val uf = makeFieldCoordinates(parentType.underlying.name, underlyingField.name)
            "The overall field $of defines argument $a which does not exist in service $s field $uf"
        }
    }

    data class IncompatibleType(
        val schemaElement: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val s = schemaElement.service.name
            val o = toString(schemaElement.overall)
            val u = toString(schemaElement.underlying)
            "Overall type kind of $o in service $s does not match underlying type kind $u"
        }
    }

    data class IncompatibleTypeName(
        val schemaElement: NadelServiceSchemaElement,
    ) : NadelSchemaValidationError {
        override val message: String = run {
            val s = schemaElement.service.name
            val o = toString(schemaElement.overall)
            val u = toString(schemaElement.underlying)
            "Type name of overall type $o in service $s does not match underlying type name $u"
        }
    }
}

private fun toString(element: GraphQLNamedSchemaElement): String {
    return "${element.javaClass.simpleName}(name=${element.name})"
}
