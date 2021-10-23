package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.NadelSchemaUtil.hasRename
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

internal class NadelFieldValidation(
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val typeValidation: NadelTypeValidation,
) {
    private val renameValidation = NadelRenameValidation(this)
    private val hydrationValidation = NadelHydrationValidation(services, typeValidation)

    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLFieldsContainer && schemaElement.underlying is GraphQLFieldsContainer) {
            validate(
                schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    fun validate(
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
                    val fieldsThatServiceContributed = getFieldsThatServiceContributed(parent)
                    fieldSequence.filter { it.name in fieldsThatServiceContributed }
                } else {
                    fieldSequence
                }
            }
            .flatMap { overallField ->
                validate(parent, overallField, underlyingFieldsByName)
            }
            .toList()
    }

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingFieldsByName: Map<String, GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationError> {
        return if (hasRename(overallField)) {
            renameValidation.validate(parent, overallField)
        } else if (hasHydration(overallField)) {
            hydrationValidation.validate(parent, overallField)
        } else {
            val underlyingField = underlyingFieldsByName[overallField.name]
            if (underlyingField == null) {
                listOf(
                    MissingUnderlyingField(parent, overallField = overallField),
                )
            } else {
                validate(
                    parent,
                    overallField = overallField,
                    underlyingField = underlyingField,
                )
            }
        }
    }

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val argumentIssues = overallField.arguments.flatMap { overallArg ->
            val underlyingArg = underlyingField.getArgument(overallArg.name)
            if (underlyingArg == null) {
                listOf(
                    MissingArgumentOnUnderlying(parent, overallField, underlyingField, overallArg),
                )
            } else {
                // TODO check the type wrappings are equal
                typeValidation.validate(
                    NadelServiceSchemaElement(
                        service = parent.service,
                        overall = overallArg.type.unwrapAll(),
                        underlying = underlyingArg.type.unwrapAll(),
                    )
                )
            }
        }

        val outputTypeIssues = typeValidation.validateOutputType(parent, overallField, underlyingField)

        return argumentIssues + outputTypeIssues
    }

    private fun getFieldsThatServiceContributed(schemaElement: NadelServiceSchemaElement): Set<String> {
        val service = schemaElement.service
        val overallType = schemaElement.overall

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

        // IF YOU CHANGE THIS then check NadelTypeValidation.getTypeNamesUsed to ensure they type is actually visited
        return type.name == overallSchema.queryType.name
            || type.name == overallSchema.mutationType?.name
            || type.name == overallSchema.subscriptionType?.name
            || overallSchema.queryType.fields.any(usesTypeAsNamespaced)
            || overallSchema.mutationType?.fields?.any(usesTypeAsNamespaced) == true
            || overallSchema.subscriptionType?.fields?.any(usesTypeAsNamespaced) == true
    }
}
