package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.isNonNull
import graphql.nadel.enginekt.util.pathToActorField
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaUtil.getHydration
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorService
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType

internal class NadelHydrationValidation(
    private val services: Map<String, Service>,
    private val service: Service,
    private val typeValidation: NadelTypeValidation,
) {
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val hydration = getHydration(overallField)
            ?: error("Don't invoke hydration validation if there is no hydration silly")

        val actorService = services[hydration.serviceName]
            ?: return listOf(
                MissingHydrationActorService(parent, overallField, hydration),
            )

        val actorServiceQueryType = actorService.underlyingSchema.queryType
        val actorField = actorServiceQueryType.getFieldAt(hydration.pathToActorField)
            ?: return listOf(
                MissingHydrationActorField(parent, overallField, hydration, actorServiceQueryType)
            )

        val argumentIssues = getArgumentIssues(parent, overallField, hydration, actorServiceQueryType, actorField)
        val outputTypeIssues = getOutputTypeIssues(parent, overallField, actorService, actorField)

        return argumentIssues + outputTypeIssues
    }

    private fun getOutputTypeIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        actorService: Service,
        actorField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        // Ensures that the underlying type of the actor field matches with the expected overall output type
        val typeValidation = typeValidation.validate(
            NadelServiceSchemaElement(
                overall = overallField.type.unwrapAll(),
                underlying = actorField.type.unwrapAll(),
                service = actorService,
            )
        )

        // Hydrations can error out so they MUST always be nullable
        val outputTypeMustBeNullable = if (overallField.type.isNonNull) {
            listOf(
                HydrationFieldMustBeNullable(parent, overallField)
            )
        } else {
            emptyList()
        }

        return typeValidation + outputTypeMustBeNullable
    }

    private fun getArgumentIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
        actorServiceQueryType: GraphQLObjectType,
        actorField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        return hydration.arguments.mapNotNull { remoteArg ->
            val actorFieldArgument = actorField.getArgument(remoteArg.name)
            if (actorFieldArgument == null) {
                MissingHydrationActorFieldArgument(
                    parent,
                    overallField,
                    hydration,
                    actorServiceQueryType,
                    argument = remoteArg.name,
                )
            } else {
                val remoteArgSource = remoteArg.remoteArgumentSource
                getArgumentIssues(parent, overallField, remoteArgSource)
            }
        }
    }

    private fun getArgumentIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArgSource: RemoteArgumentSource,
    ): NadelSchemaValidationError? {
        return when (remoteArgSource.sourceType) {
            OBJECT_FIELD -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.path)
                if (field == null) {
                    MissingHydrationFieldValueSource(service, parent, overallField, remoteArgSource)
                } else {
                    // TODO: check argument type is correct
                    null
                }
            }
            FIELD_ARGUMENT -> {
                val argument = overallField.getArgument(remoteArgSource.name)
                if (argument == null) {
                    MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource)
                } else {
                    // TODO: check argument type is correct
                    null
                }
            }
            else -> {
                null
            }
        }
    }
}
