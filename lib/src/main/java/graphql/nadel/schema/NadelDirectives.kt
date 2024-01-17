package graphql.nadel.schema

import graphql.GraphQLContext
import graphql.execution.ValuesResolver
import graphql.language.ArrayValue
import graphql.language.DirectiveDefinition
import graphql.language.EnumTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectValue
import graphql.language.SDLDefinition
import graphql.language.StringValue
import graphql.language.Value
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.RemoteArgumentSource.SourceType
import graphql.nadel.dsl.TypeMappingDefinition
import graphql.nadel.dsl.NadelHydrationDefinition
import graphql.nadel.dsl.NadelHydrationConditionDefinition
import graphql.nadel.dsl.NadelHydrationConditionPredicateDefinition
import graphql.nadel.dsl.NadelHydrationResultConditionDefinition
import graphql.nadel.engine.util.singleOfType
import graphql.parser.Parser
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import java.util.Locale

/**
 * If you update this file please add to NadelBuiltInTypes
 */
object NadelDirectives {
    val renamedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to rename a type or field in the overall schema"
            directive @renamed(
                "The type to be renamed"
                from: String!
            ) on FIELD_DEFINITION | OBJECT | INTERFACE | UNION | INPUT_OBJECT | SCALAR | ENUM
        """.trimIndent(),
    )

    val nadelHydrationComplexIdentifiedBy = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This is required by batch hydration to understand how to pull out objects from the batched result"
            input NadelBatchObjectIdentifiedBy {
                sourceId: String!
                resultId: String!
            }
        """.trimIndent(),
    )

    val nadelHydrationArgumentDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields"
            input NadelHydrationArgument {
                name: String!
                value: JSON!
            }
        """.trimIndent(),
    )

    val nadelWhenConditionPredicateDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            input NadelHydrationResultFieldPredicate @oneOf {
                startsWith: String
                equals: JSON
                matches: String
            }
        """.trimIndent(),
    )

    val nadelWhenConditionResultDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "Specify a condition for the hydration to activate based on the result"
            input NadelHydrationResultCondition {
                sourceField: String!
                predicate: NadelHydrationResultFieldPredicate!
            }
        """.trimIndent(),
    )

    val nadelWhenConditionDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "Specify a condition for the hydration to activate"
            input NadelHydrationCondition {
                result: NadelHydrationResultCondition!
            }
        """.trimIndent(),
    )

    val hydratedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields"
            directive @hydrated(
                "The target service"
                service: String!
                "The target top level field"
                field: String!
                "How to identify matching results"
                identifiedBy: String! = "id"
                "How to identify matching results"
                inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
                "Are results indexed"
                indexed: Boolean! = false
                "Is querying batched"
                batched: Boolean! = false
                "The batch size"
                batchSize: Int! = 200
                "The timeout to use when completing hydration"
                timeout: Int! = -1
                "The arguments to the hydrated field"
                arguments: [NadelHydrationArgument!]!
                "Specify a condition for the hydration to activate"
                when: NadelHydrationCondition
            ) repeatable on FIELD_DEFINITION
        """.trimIndent(),
    )

    val dynamicServiceDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field uses dynamic service resolution. This directive should only be used in commons fields, i.e. fields that are not part of a particular service."
            directive @dynamicServiceResolution on FIELD_DEFINITION
        """.trimIndent(),
    )

    val namespacedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field is a namespaced field."
            directive @namespaced on FIELD_DEFINITION
        """.trimIndent(),
    )

    val hiddenDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field is not available for queries or introspection"
            directive @hidden on FIELD_DEFINITION
        """.trimIndent(),
    )

    val nadelHydrationFromArgumentDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields with the @hydratedFrom directive"
            input NadelHydrationFromArgument {
                name: String!
                valueFromField: String
                valueFromArg: String
            }
        """.trimIndent(),
    )

    val nadelHydrationTemplateEnumDefinition = parseDefinition<EnumTypeDefinition>(
        // language=GraphQL
        """
            enum NadelHydrationTemplate {
                NADEL_PLACEHOLDER
            }
        """.trimIndent(),
    )

    val hydratedFromDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields"
            directive @hydratedFrom(
                "The arguments to the hydrated field"
                arguments: [NadelHydrationFromArgument!]! = []
                "The hydration template to use"
                template: NadelHydrationTemplate!
            ) repeatable on FIELD_DEFINITION
        """.trimIndent(),
    )

    val hydratedTemplateDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This template directive provides common values to hydrated fields"
            directive @hydratedTemplate(
                "The target service"
                service: String!
                "The target top level field"
                field: String!
                "How to identify matching results"
                identifiedBy: String! = "id"
                "How to identify matching results"
                inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
                "Are results indexed"
                indexed: Boolean = false
                "Is querying batched"
                batched: Boolean = false
                "The batch size"
                batchSize: Int = 200
                "The timeout in milliseconds"
                timeout: Int = -1
            ) on ENUM_VALUE
        """.trimIndent(),
    )

    internal fun createUnderlyingServiceHydration(
        fieldDefinition: GraphQLFieldDefinition,
        overallSchema: GraphQLSchema,
    ): List<NadelHydrationDefinition> {
        val hydrations = fieldDefinition.getAppliedDirectives(hydratedDirectiveDefinition.name)
            .map { directive ->
                val arguments = createRemoteArgs(directive.getArgument("arguments").argumentValue.value as ArrayValue)

                val inputIdentifiedBy = directive.getArgument("inputIdentifiedBy")
                val identifiedByValues = resolveArgumentValue<List<Any>>(inputIdentifiedBy)
                val identifiedBy = createObjectIdentifiers(identifiedByValues)

                val conditionalHydration = directive.getArgument("when")
                    ?.let {
                        buildConditionalHydrationObject(it)?.result
                    }

                buildHydrationParameters(directive, arguments, identifiedBy, conditionalHydration)
            }

        val templatedHydrations = fieldDefinition.getAppliedDirectives(hydratedFromDirectiveDefinition.name)
            .map { directive ->
                createTemplatedUnderlyingServiceHydration(directive, overallSchema)
            }

        return hydrations + templatedHydrations
    }

    private fun buildHydrationParameters(
        directive: GraphQLAppliedDirective,
        arguments: List<RemoteArgumentDefinition>,
        identifiedBy: List<NadelHydrationDefinition.ObjectIdentifier>,
        conditionalHydration: NadelHydrationResultConditionDefinition? = null,
    ): NadelHydrationDefinition {
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
        return NadelHydrationDefinition(
            service,
            fieldNames,
            arguments,
            objectIdentifier,
            identifiedBy,
            objectIndexed,
            batched,
            batchSize,
            timeout,
            conditionalHydration
        )
    }

    private fun createTemplatedUnderlyingServiceHydration(
        hydratedFromDirective: GraphQLAppliedDirective,
        overallSchema: GraphQLSchema,
    ): NadelHydrationDefinition {
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

    private fun createRemoteArgs(arguments: ArrayValue): List<RemoteArgumentDefinition> {
        return arguments.values
            .map { arg ->
                @Suppress("UNCHECKED_CAST") // trust GraphQL type system and caller
                val argMap = arg as ObjectValue
                val remoteArgName = (argMap.objectFields.single { it.name == "name" }.value as StringValue).value
                val remoteArgValue = argMap.objectFields.single { it.name == "value" }.value
                val remoteArgumentSource = createRemoteArgumentSource(remoteArgValue)
                RemoteArgumentDefinition(remoteArgName, remoteArgumentSource)
            }
    }

    private fun createObjectIdentifiers(arguments: List<Any>): List<NadelHydrationDefinition.ObjectIdentifier> {
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
            NadelHydrationDefinition.ObjectIdentifier(sourceId, resultId)
        }
    }

    private fun createRemoteArgumentSource(value: Value<*>): RemoteArgumentSource {
        if (value is StringValue) {
            val values = listFromDottedString(value.value)
            return when (values.first()) {
                "\$source" -> RemoteArgumentSource(
                    argumentName = null,
                    pathToField = values.subList(1, values.size),
                    staticValue = null,
                    sourceType = SourceType.ObjectField,
                )

                "\$argument" -> RemoteArgumentSource(
                    argumentName = values.subList(1, values.size).single(),
                    pathToField = null,
                    staticValue = null,
                    sourceType = SourceType.FieldArgument,
                )

                else -> RemoteArgumentSource(
                    argumentName = null,
                    pathToField = null,
                    staticValue = value,
                    sourceType = SourceType.StaticArgument,
                )
            }
        } else {
            return RemoteArgumentSource(
                argumentName = null,
                pathToField = null,
                staticValue = value,
                sourceType = SourceType.StaticArgument,
            )
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
        var staticValue: Value<*>? = null
        when (argumentType) {
            SourceType.ObjectField -> path = values
            SourceType.FieldArgument -> argumentName = values.single()
            SourceType.StaticArgument -> staticValue = StringValue(value)
        }

        return RemoteArgumentSource(argumentName, path, staticValue, argumentType)
    }

    internal fun createFieldMapping(fieldDefinition: GraphQLFieldDefinition): FieldMappingDefinition? {
        val directive = fieldDefinition.getAppliedDirective(renamedDirectiveDefinition.name)
            ?: return null
        val fromValue = getDirectiveValue<String>(directive, "from")

        return FieldMappingDefinition(inputPath = fromValue.split('.'))
    }

    internal fun createTypeMapping(directivesContainer: GraphQLDirectiveContainer): TypeMappingDefinition? {
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
        return ValuesResolver.valueToInternalValue(
            graphQLArgument.argumentValue,
            graphQLArgument.type,
            GraphQLContext.getDefault(),
            Locale.getDefault()
        ) as T
    }

    private fun buildConditionalHydrationObject(whenConditionArgument: GraphQLAppliedDirectiveArgument): NadelHydrationConditionDefinition? {
        val result = whenConditionArgument.getValue<Map<String, Map<String, Any>>>()?.get("result")
            ?: return null

        val sourceField = result["sourceField"]!! as String
        val predicate: Map<String, Any> = result["predicate"]!! as Map<String, Any>

        return NadelHydrationConditionDefinition(
            result = NadelHydrationResultConditionDefinition(
                sourceField = sourceField,
                predicate = NadelHydrationConditionPredicateDefinition(
                    equals = predicate["equals"],
                    startsWith = predicate["startsWith"] as String?,
                    matches = (predicate["matches"] as String?)?.toRegex(),
                )
            )
        )
    }

    private inline fun <reified T : SDLDefinition<*>> parseDefinition(sdl: String): T {
        return Parser.parse(sdl).definitions.singleOfType()
    }
}
