package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.NadelSchemaUtil.hasRename
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingUnderlyingField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

class NadelFieldValidation(
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val service: Service,
    private val typeValidation: NadelTypeValidation,
) {
    private val renameValidation = NadelRenameValidation(service, this)
    private val hydrationValidation = NadelHydrationValidation(services, service, typeValidation)

    fun getIssues(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLFieldsContainer && schemaElement.underlying is GraphQLFieldsContainer) {
            getIssues(
                schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    fun getIssues(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLFieldDefinition>,
        underlyingFields: List<GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields
            .asSequence()
            .let { fieldSequence ->
                // Apply filter if necessary
                if (isCombinedType(parent.overall)) {
                    val fieldsThatServiceContributed = getFieldsThatServiceContributed(parent.overall)
                    fieldSequence.filter { it.name in fieldsThatServiceContributed }
                } else {
                    fieldSequence
                }
            }
            .flatMap { overallField ->
                getIssues(parent, overallField, underlyingFieldsByName)
            }
            .toList()
    }

    fun getIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingFieldsByName: Map<String, GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationError> {
        return if (hasRename(overallField)) {
            renameValidation.getIssues(parent, overallField)
        } else if (hasHydration(overallField)) {
            hydrationValidation.getIssues(parent, overallField)
        } else {
            val underlyingField = underlyingFieldsByName[overallField.name]
            if (underlyingField == null) {
                listOf(
                    missingUnderlyingField(service, parent, overallField = overallField),
                )
            } else {
                getIssues(
                    parent,
                    overallField = overallField,
                    underlyingField = underlyingField,
                )
            }
        }
    }

    fun getIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val argumentIssues = overallField.arguments.flatMap { overallArg ->
            val underlyingArg = underlyingField.getArgument(overallArg.name)
            if (underlyingArg == null) {
                listOf(
                    missingArgumentOnUnderlying(service, parent, overallField, underlyingField, overallArg),
                )
            } else {
                // TODO check the type wrappings are equal
                typeValidation.getIssues(
                    NadelServiceSchemaElement(
                        service = service,
                        overall = overallArg.type.unwrapAll(),
                        underlying = underlyingArg.type.unwrapAll(),
                    )
                )
            }
        }

        val outputTypeIssues = typeValidation.getIssues(
            NadelServiceSchemaElement(
                service = service,
                overall = overallField.type.unwrapAll(),
                underlying = underlyingField.type.unwrapAll(),
            )
        )

        return argumentIssues + outputTypeIssues
    }

    private fun getFieldsThatServiceContributed(overallType: GraphQLNamedSchemaElement): Set<String> {
        return service.definitionRegistry.definitions
            .asSequence()
            .filterIsInstance<AnyImplementingTypeDefinition>()
            .filter { it.name == overallType.name }
            .flatMap { it.fieldDefinitions }
            .map { it.name }
            .toSet()
    }

    /**
     * Determines whether a type has fields backed by different services.
     *
     * e.g.
     *
     * ```graphql
     * type Query {
     *   echo: String # Backed by testing service
     *   user(id: ID): User # Backed by identity service
     * }
     * ```
     *
     * That is, validation for these types must only occur for the fields on
     * the type must be done against multiple underlying types.
     */
    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        val usesTypeAsNamespaced = { field: GraphQLFieldDefinition ->
            field.hasDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name)
                && field.type.unwrapAll().name == type.name
        }

        return type.name == overallSchema.queryType.name
            || type.name == overallSchema.mutationType?.name
            || type.name == overallSchema.subscriptionType?.name
            || overallSchema.queryType.fields.any(usesTypeAsNamespaced)
            || overallSchema.mutationType?.fields?.any(usesTypeAsNamespaced) == true
            || overallSchema.subscriptionType?.fields?.any(usesTypeAsNamespaced) == true
    }
}
