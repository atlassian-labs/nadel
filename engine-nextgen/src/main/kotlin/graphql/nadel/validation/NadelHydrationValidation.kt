package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.pathToActorField
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaUtil.getHydration
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingHydrationActorService
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingHydrationFieldValueSource
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType

class NadelHydrationValidation(
    private val services: Map<String, Service>,
    private val service: Service,
    private val typeValidation: NadelTypeValidation,
) {
    fun getIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val hydration = getHydration(overallField)
            ?: error("Don't invoke hydration validation if there is no hydration silly")

        val actorService = services[hydration.serviceName]
            ?: return listOf(
                missingHydrationActorService(parent, overallField, hydration),
            )

        val actorServiceQueryType = actorService.underlyingSchema.queryType
        val actorField = actorServiceQueryType.getFieldAt(hydration.pathToActorField)
            ?: return listOf(
                missingHydrationActorField(parent, overallField, hydration, actorServiceQueryType)
            )

        val argumentIssues = getArgumentIssues(parent, overallField, hydration, actorServiceQueryType, actorField)

        val outputTypeIssues = typeValidation.getIssues(
            NadelServiceSchemaElement(
                overall = overallField.type.unwrapAll(),
                underlying = actorField.type.unwrapAll(),
                service = actorService,
            )
        )

        return argumentIssues + outputTypeIssues
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
                missingHydrationActorFieldArgument(
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
                    missingHydrationFieldValueSource(service, parent, overallField, remoteArgSource)
                } else {
                    // TODO: check argument type is correct
                    null
                }
            }
            FIELD_ARGUMENT -> {
                val argument = overallField.getArgument(remoteArgSource.name)
                if (argument == null) {
                    missingHydrationArgumentValueSource(parent, overallField, remoteArgSource)
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
