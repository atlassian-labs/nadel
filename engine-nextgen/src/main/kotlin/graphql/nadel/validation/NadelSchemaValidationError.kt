package graphql.nadel.validation

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.language.FieldDefinition
import graphql.language.SDLExtensionDefinition
import graphql.language.SourceLocation
import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.pathToActorField
import graphql.nadel.validation.NadelSchemaValidationErrorType.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationErrorType.IllegalExtensionField
import graphql.nadel.validation.NadelSchemaValidationErrorType.IncompatibleTypes
import graphql.nadel.validation.NadelSchemaValidationErrorType.MissingArgument
import graphql.nadel.validation.NadelSchemaValidationErrorType.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationErrorType.MissingUnderlyingField
import graphql.nadel.validation.NadelSchemaValidationErrorType.MissingUnderlyingInputField
import graphql.nadel.validation.NadelSchemaValidationErrorType.MissingUnderlyingType
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLObjectType

data class NadelSchemaValidationError(
    private val message: String,
    private val errorType: NadelSchemaValidationErrorType,
) : GraphQLError {
    override fun getMessage(): String = message

    override fun getLocations(): MutableList<SourceLocation> = mutableListOf()

    override fun getErrorType(): NadelSchemaValidationErrorType = errorType

    companion object {
        fun missingUnderlyingType(
            service: Service,
            overallTypeName: String,
        ): NadelSchemaValidationError {
            return NadelSchemaValidationError(
                message = "Could not find underlying type for overall type '${overallTypeName}' in ${service.name}",
                errorType = MissingUnderlyingType,
            )
        }

        fun missingUnderlyingField(
            service: Service,
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val s = service.name
            val ut = parent.underlying.name

            return NadelSchemaValidationError(
                message = "Could not find overall field $of on the underlying type $ut on service $s",
                errorType = MissingUnderlyingField,
            )
        }

        fun missingUnderlyingInputField(
            service: Service,
            parent: NadelServiceSchemaElement,
            overallField: GraphQLInputObjectField,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val s = service.name
            val ut = parent.underlying.name

            return NadelSchemaValidationError(
                message = "Could not find overall input field $of on the underlying input type $ut on service $s",
                errorType = MissingUnderlyingInputField,
            )
        }

        fun missingUnderlyingEnum(
            service: Service,
            parent: NadelServiceSchemaElement,
            overallValue: GraphQLEnumValueDefinition,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallValue.name)
            val s = service.name
            val ut = parent.underlying.name

            return NadelSchemaValidationError(
                message = "Could not find overall enum value $of on the underlying enum type $ut on service $s",
                errorType = MissingUnderlyingInputField,
            )
        }

        fun missingHydrationActorService(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            hydration: UnderlyingServiceHydration,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val s = hydration.serviceName

            return NadelSchemaValidationError(
                message = "Field $of tried to hydrate from non-existent service $s",
                errorType = MissingUnderlyingField,
            )
        }

        fun missingHydrationActorField(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            hydration: UnderlyingServiceHydration,
            actorServiceQueryType: GraphQLObjectType,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"

            return NadelSchemaValidationError(
                message = "Field $of tried to hydrate from non-existent field $af on service $s",
                errorType = MissingUnderlyingField,
            )
        }

        fun hydrationFieldMustBeNullable(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)

            return NadelSchemaValidationError(
                message = "Field $of declares a hydration so its output type MUST be nullable",
                errorType = HydrationFieldMustBeNullable,
            )
        }

        fun missingHydrationFieldValueSource(
            service: Service,
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            remoteArgSource: RemoteArgumentSource,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val uf = "${parent.overall.name}.${remoteArgSource.path.joinToString(separator = ".")}"
            val s = service.name

            return NadelSchemaValidationError(
                message = "Field $of tried to hydrate using value of non-existent underlying field $uf from service $s as an argument",
                errorType = MissingHydrationArgumentValueSource,
            )
        }

        fun missingHydrationArgumentValueSource(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            remoteArgSource: RemoteArgumentSource,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val a = remoteArgSource.name

            return NadelSchemaValidationError(
                message = "Field $of tried to hydrate using value of non-existent argument $a as an argument",
                errorType = MissingHydrationArgumentValueSource,
            )
        }

        fun missingHydrationActorFieldArgument(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            hydration: UnderlyingServiceHydration,
            actorServiceQueryType: GraphQLObjectType,
            argument: String,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val s = hydration.serviceName
            val af = "${actorServiceQueryType.name}.${hydration.pathToActorField.joinToString(separator = ".")}"

            return NadelSchemaValidationError(
                message = "Hydration on field $of references non-existent argument $argument on hydration actor $s.$af",
                errorType = MissingUnderlyingField,
            )
        }

        fun missingRename(
            service: Service,
            parent: NadelServiceSchemaElement,
            field: GraphQLFieldDefinition,
            rename: FieldMappingDefinition,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, field.name)
            val uf = "${parent.underlying.name}.${rename.inputPath.joinToString(separator = ".")}"
            val s = service.name

            return NadelSchemaValidationError(
                message = "Overall field $of defines rename but underlying field $uf on service $s doesn't exist",
                errorType = MissingUnderlyingField,
            )
        }

        fun missingArgumentOnUnderlying(
            service: Service,
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            underlyingField: GraphQLFieldDefinition,
            argument: GraphQLArgument,
        ): NadelSchemaValidationError {
            val of = makeFieldCoordinates(parent.overall.name, overallField.name)
            val a = argument.name
            val s = service.name
            val uf = makeFieldCoordinates(parent.underlying.name, underlyingField.name)

            return NadelSchemaValidationError(
                message = "The overall field $of defines argument $a which does not exist in service $s field $uf",
                errorType = MissingArgument,
            )
        }

        fun incompatibleType(
            schemaElement: NadelServiceSchemaElement,
        ): NadelSchemaValidationError {
            val s = schemaElement.service.name
            val o = toString(schemaElement.overall)
            val u = toString(schemaElement.underlying)

            return NadelSchemaValidationError(
                message = "Overall type $o kind does not match underlying type kind $u in service $s",
                errorType = IncompatibleTypes,
            )
        }

        private fun toString(element: GraphQLNamedSchemaElement): String {
            return "${element.javaClass.name}(name=${element.name})"
        }
    }
}

enum class NadelSchemaValidationErrorType : ErrorClassification {
    MissingUnderlyingType,
    MissingUnderlyingField,
    MissingUnderlyingInputField,
    MissingArgument,
    MissingHydrationArgumentValueSource,
    IncompatibleTypes,
    HydrationFieldMustBeNullable,
    IllegalExtensionField,
}

