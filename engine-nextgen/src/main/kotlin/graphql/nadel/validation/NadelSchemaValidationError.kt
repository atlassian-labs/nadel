package graphql.nadel.validation

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.language.InputValueDefinition
import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.pathToActorField
import graphql.nadel.enginekt.util.unwrapAll
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil

private class NadelSchemaValidationErrorClassification(
    private val type: NadelSchemaValidationError,
) : ErrorClassification {
    override fun toSpecification(error: GraphQLError?): Any {
        return type.javaClass.simpleName
    }
}

sealed interface NadelSchemaValidationError {
    /**
     * Human readable message associated with the error
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

    data class MissingHydrationActorService(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            "Field $of tried to hydrate from non-existent service $s"
        }

        override val subject = overallField
    }

    data class MissingHydrationActorField(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
        val actorServiceQueryType: GraphQLObjectType,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"
            "Field $of tried to hydrate from non-existent field in overall schema $af on service $s"
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

    data class PolymorphicHydrationReturnTypeMismatch(
        val actorField: GraphQLFieldDefinition,
        val actorService: Service,
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Field $of declares a polymorphic hydration with incorrect return type. One of the hydrations' actor fields" +
                " ${actorField.name} in the service ${actorService.name} returns the type " +
                "${(actorField.type.unwrapAll() as GraphQLNamedType).name} which is not present in the polymorphic hydration return " +
                "type ${(overallField.type.unwrapAll() as GraphQLNamedType).name}"
        }

        override val subject = overallField
    }

    data class MissingHydrationFieldValueSource(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: RemoteArgumentSource,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${remoteArgSource.path.joinToString(separator = ".")}"
            val s = service.name
            "Field $of tried to hydrate using value of non-existent underlying field $uf from service $s as an argument"
        }

        override val subject = overallField
    }

    data class MissingHydrationArgumentValueSource(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArgSource: RemoteArgumentSource,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val a = remoteArgSource.name
            "Field $of tried to hydrate using value of non-existent field $a as an argument"
        }

        override val subject = overallField
    }

    data class MissingHydrationActorFieldArgument(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val hydration: UnderlyingServiceHydration,
        val actorServiceQueryType: GraphQLObjectType,
        val argument: String,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"
            "Hydration on field $of references non-existent argument $argument on hydration actor $s.$af"
        }

        override val subject = overallField
    }

    data class MissingRename(
        val parentType: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val rename: FieldMappingDefinition,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            val uf = "${parentType.underlying.name}.${rename.inputPath.joinToString(separator = ".")}"
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
        val duplicates: List<RemoteArgumentDefinition>,
    ) : NadelSchemaValidationError {
        val service: Service get() = parentType.service

        override val message = run {
            val an = duplicates.map { it.name }.toSet().single()
            val of = makeFieldCoordinates(parentType.overall.name, overallField.name)
            "Hydration at $of provides multiple possible values for argument $an"
        }

        override val subject = overallField
    }
}

private fun toString(element: GraphQLNamedSchemaElement): String {
    return "${element.javaClass.simpleName}(name=${element.name})"
}
