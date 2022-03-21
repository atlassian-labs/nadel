package graphql.nadel.schema

import graphql.Scalars
import graphql.Scalars.GraphQLString
import graphql.execution.ValuesResolver
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.DirectiveDefinition.newDirectiveDefinition
import graphql.language.EnumTypeDefinition.newEnumTypeDefinition
import graphql.language.EnumValueDefinition.newEnumValueDefinition
import graphql.language.InputObjectTypeDefinition.newInputObjectDefinition
import graphql.language.StringValue
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.RemoteArgumentSource.SourceType
import graphql.nadel.dsl.TypeMappingDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.util.IntValue
import graphql.nadel.util.description
import graphql.nadel.util.emptyArrayValue
import graphql.nadel.util.inputValueDefinition
import graphql.nadel.util.list
import graphql.nadel.util.nonNull
import graphql.nadel.util.onEnum
import graphql.nadel.util.onEnumValue
import graphql.nadel.util.onFieldDefinition
import graphql.nadel.util.onInputObject
import graphql.nadel.util.onInterface
import graphql.nadel.util.onObject
import graphql.nadel.util.onScalar
import graphql.nadel.util.onUnion
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

/**
 * If you update this file please add to NadelBuiltInTypes
 */
object NadelDirectives {
    val renamedDirectiveDefinition = newDirectiveDefinition()
        .name("renamed")
        .onFieldDefinition().onObject().onInterface().onUnion().onInputObject().onScalar().onEnum()
        .description("This allows you to rename a type or field in the overall schema")
        .inputValueDefinition(name = "from", type = nonNull(GraphQLString), description = "The type to be renamed")
        .build()

    val nadelHydrationComplexIdentifiedBy = newInputObjectDefinition()
        .name("NadelBatchObjectIdentifiedBy")
        .description("This is required by batch hydration to understand how to pull out objects from the batched result")
        .inputValueDefinition(
            name = "sourceId",
            type = nonNull(GraphQLString),
        )
        .inputValueDefinition(
            name = "resultId",
            type = nonNull(GraphQLString),
        )
        .build()

    val nadelHydrationArgumentDefinition = newInputObjectDefinition()
        .name("NadelHydrationArgument")
        .description("This allows you to hydrate new values into fields")
        .inputValueDefinition(
            name = "name",
            type = nonNull(GraphQLString),
        )
        .inputValueDefinition(
            name = "value",
            type = nonNull(GraphQLString),
        )
        .build()

    val hydratedDirectiveDefinition = newDirectiveDefinition()
        .name("hydrated")
        .onFieldDefinition()
        .description("This allows you to hydrate new values into fields")
        .repeatable(true)
        .inputValueDefinition(
            name = "service",
            type = nonNull(GraphQLString),
            description = "The target service",
        )
        .inputValueDefinition(
            name = "field",
            type = nonNull(GraphQLString),
            description = "The target top level field",
        )
        .inputValueDefinition(
            name = "identifiedBy",
            type = nonNull(GraphQLString),
            description = "How to identify matching results",
            defaultValue = StringValue("id"),
        )
        .inputValueDefinition(
            name = "inputIdentifiedBy",
            type = nonNull(list(nonNull(nadelHydrationComplexIdentifiedBy))),
            description = "How to identify matching results",
            defaultValue = emptyArrayValue(),
        )
        .inputValueDefinition(
            name = "indexed",
            description = "Are results indexed",
            type = nonNull(Scalars.GraphQLBoolean),
            defaultValue = BooleanValue(false),
        )
        .inputValueDefinition(
            name = "batched",
            description = "Is querying batched",
            type = nonNull(Scalars.GraphQLBoolean),
            defaultValue = BooleanValue(false),
        )
        .inputValueDefinition(
            name = "batchSize",
            description = "The batch size",
            type = nonNull(Scalars.GraphQLInt),
            defaultValue = IntValue(200),
        )
        .inputValueDefinition(
            name = "timeout",
            description = "The timeout to use when completing hydration",
            type = nonNull(Scalars.GraphQLInt),
            defaultValue = IntValue(-1),
        )
        .inputValueDefinition(
            name = "arguments",
            description = "The arguments to the hydrated field",
            type = nonNull(list(nonNull(nadelHydrationArgumentDefinition))),
        )
        .build()

    val dynamicServiceDirectiveDefinition = newDirectiveDefinition()
        .name("dynamicServiceResolution")
        .onFieldDefinition()
        .description("Indicates that the field uses dynamic service resolution. This directive should only be used in commons fields, i.e. fields that are not part of a particular service.")
        .build()

    val namespacedDirectiveDefinition = newDirectiveDefinition()
        .name("namespaced")
        .onFieldDefinition()
        .description("Indicates that the field is a namespaced field.")
        .build()

    val hiddenDirectiveDefinition = newDirectiveDefinition()
        .name("hidden")
        .description("Indicates that the field is not available for queries or introspection")
        .onFieldDefinition()
        .build()

    val nadelHydrationFromArgumentDefinition = newInputObjectDefinition()
        .name("NadelHydrationFromArgument")
        .description("This allows you to hydrate new values into fields with the @hydratedFrom directive")
        .inputValueDefinition(
            name = "name",
            type = nonNull(GraphQLString),
        )
        .inputValueDefinition(
            name = "valueFromField",
            type = GraphQLString,
        )
        .inputValueDefinition(
            name = "valueFromArg",
            type = GraphQLString,
        )
        .build()

    val nadelHydrationTemplateEnumDefinition = newEnumTypeDefinition()
        .name("NadelHydrationTemplate")
        .enumValueDefinition(newEnumValueDefinition().name("NADEL_PLACEHOLDER").build())
        .build()

    val hydratedFromDirectiveDefinition = newDirectiveDefinition()
        .name("hydratedFrom")
        .onFieldDefinition()
        .description("This allows you to hydrate new values into fields")
        .repeatable(true)
        .inputValueDefinition(
            name = "arguments",
            description = "The arguments to the hydrated field",
            type = nonNull(list(nonNull(nadelHydrationFromArgumentDefinition))),
            defaultValue = ArrayValue.newArrayValue().build(),
        )
        .inputValueDefinition(
            name = "template",
            description = "The hydration template to use",
            type = nonNull(nadelHydrationTemplateEnumDefinition),
        )
        .build()

    val hydratedTemplateDirectiveDefinition = newDirectiveDefinition()
        .name("hydratedTemplate")
        .onEnumValue()
        .description("This template directive provides common values to hydrated fields")
        .inputValueDefinition(
            name = "service",
            type = nonNull(GraphQLString),
            description = "The target service"
        )
        .inputValueDefinition(
            name = "field",
            type = nonNull(GraphQLString),
            description = "The target top level field"
        )
        .inputValueDefinition(
            name = "identifiedBy",
            description = "How to identify matching results",
            type = nonNull(GraphQLString),
            defaultValue = StringValue.newStringValue("id").build(),
        )
        .inputValueDefinition(
            name = "inputIdentifiedBy",
            description = "How to identify matching results",
            type = nonNull(list(nonNull(nadelHydrationComplexIdentifiedBy))),
            defaultValue = ArrayValue.newArrayValue().build(),
        )
        .inputValueDefinition(
            name = "indexed",
            description = "Are results indexed",
            type = Scalars.GraphQLBoolean,
            defaultValue = BooleanValue(false),
        )
        .inputValueDefinition(
            name = "batched",
            description = "Is querying batched",
            type = Scalars.GraphQLBoolean,
            defaultValue = BooleanValue(false),
        )
        .inputValueDefinition(
            name = "batchSize",
            description = "The batch size",
            type = Scalars.GraphQLInt,
            defaultValue = IntValue(200),
        )
        .inputValueDefinition(
            name = "timeout",
            description = "The timeout in milliseconds",
            type = Scalars.GraphQLInt,
            defaultValue = IntValue(-1),
        )
        .build()

    fun createUnderlyingServiceHydration(
        fieldDefinition: GraphQLFieldDefinition,
        overallSchema: GraphQLSchema,
    ): List<UnderlyingServiceHydration> {
        val hydrations = fieldDefinition.getAppliedDirectives(hydratedDirectiveDefinition.name)
            .asSequence()
            .map { directive ->
                val argumentValues = resolveArgumentValue<List<Any>>(directive.getArgument("arguments"))
                val arguments = createRemoteArgs(argumentValues)

                val inputIdentifiedBy = directive.getArgument("inputIdentifiedBy")
                val identifiedByValues = resolveArgumentValue<List<Any>>(inputIdentifiedBy)
                val identifiedBy = createObjectIdentifiers(identifiedByValues)

                buildHydrationParameters(directive, arguments, identifiedBy)
            }

        val templatedHydrations = fieldDefinition.getAppliedDirectives(hydratedFromDirectiveDefinition.name)
            .asSequence()
            .map { directive ->
                createTemplatedUnderlyingServiceHydration(directive, overallSchema)
            }

        return (hydrations + templatedHydrations).toList()
    }

    private fun buildHydrationParameters(
        directive: GraphQLAppliedDirective,
        arguments: List<RemoteArgumentDefinition>,
        identifiedBy: List<UnderlyingServiceHydration.ObjectIdentifier>,
    ): UnderlyingServiceHydration {
        val service = getDirectiveValue<String>(directive, "service")
        val fieldNames = getDirectiveValue<String>(directive, "field").split('.')
        val objectIdentifier = getDirectiveValue<String>(directive, "identifiedBy")
        val objectIndexed = getDirectiveValue<Boolean>(directive, "indexed")

        // Note: this is not properly implemented yet, so the value does not matter
        val batched = false // getDirectiveValue(directive, "batched", Boolean.class, false);

        val batchSize = getDirectiveValue<Int>(directive, "batchSize")
        val timeout = getDirectiveValue<Int>(directive, "timeout")

        require(fieldNames.isNotEmpty())

        // nominally this should be some other data class that's not an AST element
        // but history is what it is, and it's an AST element that's' really a data class
        return UnderlyingServiceHydration(
            service,
            fieldNames,
            arguments,
            objectIdentifier,
            identifiedBy,
            objectIndexed,
            batched,
            batchSize,
            timeout
        )
    }

    private fun createTemplatedUnderlyingServiceHydration(
        hydratedFromDirective: GraphQLAppliedDirective,
        overallSchema: GraphQLSchema,
    ): UnderlyingServiceHydration {
        val template = hydratedFromDirective.getArgument("template")
        val enumTargetName = resolveArgumentValue<String?>(template)
        val templateEnumType = overallSchema.getTypeAs<GraphQLEnumType?>("NadelHydrationTemplate")
        requireNotNull(templateEnumType) { "There MUST be a enum called NadelHydrationTemplate" }
        val enumValue = templateEnumType.getValue(enumTargetName)
        requireNotNull(enumValue) {
            "There MUST be a enum value in NadelHydrationTemplate called '${enumTargetName}'"
        }
        val templateDirective = enumValue.getAppliedDirective(hydratedTemplateDirectiveDefinition.name)
        requireNotNull(templateDirective) {
            "The enum value '${enumTargetName}' in NadelHydrationTemplate must have a directive called '${hydratedTemplateDirectiveDefinition.name}'"
        }

        val graphQLArgument = hydratedFromDirective.getArgument("arguments")
        val argumentValues = resolveArgumentValue<List<Any>>(graphQLArgument)
        val arguments = createTemplatedHydratedArgs(argumentValues)
        return buildHydrationParameters(
            templateDirective,
            arguments,
            emptyList()
        )
    }

    private fun createRemoteArgs(arguments: List<Any>): List<RemoteArgumentDefinition> {
        fun Map<String, String>.requireArgument(key: String): String {
            return requireNotNull(this[key]) {
                "${nadelHydrationArgumentDefinition.name} definition requires '$key' to be not-null"
            }
        }

        return arguments
            .map { arg ->
                @Suppress("UNCHECKED_CAST") // trust GraphQL type system and caller
                val argMap = arg as Map<String, String>
                val remoteArgName = argMap.requireArgument("name")
                val remoteArgValue = argMap.requireArgument("value")
                val remoteArgumentSource = createRemoteArgumentSource(remoteArgValue)
                RemoteArgumentDefinition(remoteArgName, remoteArgumentSource)
            }
    }

    private fun createObjectIdentifiers(arguments: List<Any>): List<UnderlyingServiceHydration.ObjectIdentifier> {
        fun Map<String, String>.requireArgument(key: String): String {
            return requireNotNull(this[key]) {
                "${nadelHydrationComplexIdentifiedBy.name} definition requires '$key' to be not-null"
            }
        }
        return arguments.map { arg ->
            @Suppress("UNCHECKED_CAST") // trust GraphQL type system and caller
            val argMap = arg as MutableMap<String, String>
            val sourceId = argMap.requireArgument("sourceId")
            val resultId = argMap.requireArgument("resultId")
            UnderlyingServiceHydration.ObjectIdentifier(sourceId, resultId)
        }
    }

    private fun createRemoteArgumentSource(value: String): RemoteArgumentSource {
        val values = listFromDottedString(value)

        return when (values.first()) {
            "\$source" -> RemoteArgumentSource(
                argumentName = null,
                pathToField = values.subList(1, values.size),
                sourceType = SourceType.ObjectField,
            )
            "\$argument" -> RemoteArgumentSource(
                argumentName = values.subList(1, values.size).single(),
                pathToField = null,
                sourceType = SourceType.FieldArgument,
            )
            else -> throw IllegalArgumentException("$value must begin with \$source. or \$argument.")
        }
    }

    private fun createTemplatedHydratedArgs(arguments: List<Any>): List<RemoteArgumentDefinition> {
        val inputObjectTypeName = nadelHydrationFromArgumentDefinition.name
        val valueFromFieldKey = "valueFromField"
        val valueFromArgKey = "valueFromArg"

        return arguments.map { arg ->
            @Suppress("UNCHECKED_CAST") // trust graphQL type system and caller
            val argMap = arg as Map<String, String>

            val remoteArgName = requireNotNull(argMap["name"]) {
                "$inputObjectTypeName requires 'name' to be not-null"
            }

            val remoteArgFieldValue = argMap[valueFromFieldKey]
            val remoteArgArgValue = argMap[valueFromArgKey]

            val remoteArgumentSource = if (remoteArgFieldValue != null && remoteArgArgValue != null) {
                throw IllegalArgumentException("$inputObjectTypeName can not have both $valueFromFieldKey and $valueFromArgKey set")
            } else if (remoteArgFieldValue != null) {
                createTemplatedRemoteArgumentSource(remoteArgFieldValue, SourceType.ObjectField)
            } else if (remoteArgArgValue != null) {
                createTemplatedRemoteArgumentSource(remoteArgArgValue, SourceType.FieldArgument)
            } else {
                throw IllegalArgumentException("$inputObjectTypeName requires one of $valueFromFieldKey or $valueFromArgKey to be set")
            }

            RemoteArgumentDefinition(remoteArgName, remoteArgumentSource)
        }
    }

    private fun createTemplatedRemoteArgumentSource(value: String, argumentType: SourceType): RemoteArgumentSource {
        // for backwards compat reasons - we will allow them to specify "$source.field.name" and treat it as just "field.name"
        val values = value
            .removePrefix("\$source.")
            .removePrefix("\$argument.")
            .split('.')

        var argumentName: String? = null
        var path: List<String>? = null
        when (argumentType) {
            SourceType.ObjectField -> path = values
            SourceType.FieldArgument -> argumentName = values.single()
        }

        return RemoteArgumentSource(argumentName, path, argumentType)
    }

    fun createFieldMapping(fieldDefinition: GraphQLFieldDefinition): FieldMappingDefinition? {
        val directive = fieldDefinition.getAppliedDirective(renamedDirectiveDefinition.name)
            ?: return null
        val fromValue = getDirectiveValue<String>(directive, "from")

        return FieldMappingDefinition(inputPath = fromValue.split('.'))
    }

    fun createTypeMapping(directivesContainer: GraphQLDirectiveContainer): TypeMappingDefinition? {
        val directive = directivesContainer.getAppliedDirective(renamedDirectiveDefinition.name)
            ?: return null
        val from = getDirectiveValue<String>(directive, "from")

        return TypeMappingDefinition(underlyingName = from, overallName = directivesContainer.name)
    }

    private fun listFromDottedString(from: String): List<String> {
        return from.split('.').toList()
    }

    private inline fun <reified T : Any> getDirectiveValue(
        directive: GraphQLAppliedDirective,
        name: String,
    ): T {
        val argument = directive.getArgument(name)
            ?: throw IllegalStateException("The @${directive.name} directive argument '$name' argument MUST be present")

        val value = resolveArgumentValue<Any?>(argument)
        return T::class.java.cast(value)
    }

    private fun <T> resolveArgumentValue(graphQLArgument: GraphQLAppliedDirectiveArgument): T {
        @Suppress("UNCHECKED_CAST") // Trust caller. Can't do much
        return ValuesResolver.valueToInternalValue(graphQLArgument.argumentValue, graphQLArgument.type) as T
    }
}
