package graphql.nadel.schema;

import graphql.Assert;
import graphql.execution.ValuesResolver;
import graphql.language.BooleanValue;
import graphql.language.Description;
import graphql.language.DirectiveDefinition;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.IntValue;
import graphql.language.NonNullType;
import graphql.language.SourceLocation;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.dsl.UnderlyingServiceHydration;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLDirectiveContainer;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.introspection.Introspection.DirectiveLocation.ENUM;
import static graphql.introspection.Introspection.DirectiveLocation.ENUM_VALUE;
import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.INPUT_OBJECT;
import static graphql.introspection.Introspection.DirectiveLocation.INTERFACE;
import static graphql.introspection.Introspection.DirectiveLocation.OBJECT;
import static graphql.introspection.Introspection.DirectiveLocation.SCALAR;
import static graphql.introspection.Introspection.DirectiveLocation.UNION;
import static graphql.language.DirectiveLocation.newDirectiveLocation;
import static graphql.language.InputValueDefinition.newInputValueDefinition;
import static graphql.language.ListType.newListType;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.CONTEXT;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class NadelDirectives {

    //
    // If you add to this, please update NadelBuiltInTypes
    //

    public static final DirectiveDefinition RENAMED_DIRECTIVE_DEFINITION;
    public static final DirectiveDefinition HYDRATED_DIRECTIVE_DEFINITION;
    public static final InputObjectTypeDefinition NADEL_HYDRATION_ARGUMENT_DEFINITION;
    public static final DirectiveDefinition DYNAMIC_SERVICE_DIRECTIVE_DEFINITION;
    public static final DirectiveDefinition NAMESPACED_DIRECTIVE_DEFINITION;
    public static final DirectiveDefinition HIDDEN_DIRECTIVE_DEFINITION;

    public static final InputObjectTypeDefinition NADEL_HYDRATION_FROM_ARGUMENT_DEFINITION;
    public static final DirectiveDefinition HYDRATED_FROM_DIRECTIVE_DEFINITION;
    public static final EnumTypeDefinition NADEL_HYDRATION_TEMPLATE_ENUM_DEFINITION;
    public static final DirectiveDefinition HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION;

    static {
        DYNAMIC_SERVICE_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("dynamicServiceResolution")
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .description(createDescription("Indicates that the field uses dynamic service resolution. This directive should only be used in commons fields, i.e. fields that are not part of a particular service."))
                .build();

        NAMESPACED_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("namespaced")
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .description(createDescription("Indicates that the field is a namespaced field."))
                .build();

        RENAMED_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("renamed")
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .directiveLocation(newDirectiveLocation().name(OBJECT.name()).build())
                .directiveLocation(newDirectiveLocation().name(INTERFACE.name()).build())
                .directiveLocation(newDirectiveLocation().name(UNION.name()).build())
                .directiveLocation(newDirectiveLocation().name(INPUT_OBJECT.name()).build())
                .directiveLocation(newDirectiveLocation().name(SCALAR.name()).build())
                .directiveLocation(newDirectiveLocation().name(ENUM.name()).build())
                .description(createDescription("This allows you to rename a type or field in the overall schema"))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("from")
                                .description(createDescription("The type to be renamed"))
                                .type(nonNull("String"))
                                .build())
                .build();

        NADEL_HYDRATION_ARGUMENT_DEFINITION = InputObjectTypeDefinition.newInputObjectDefinition()
                .name("NadelHydrationArgument")
                .description(createDescription("This allows you to hydrate new values into fields"))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("name")
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("value")
                                .type(nonNull("String"))
                                .build())
                .build();

        HYDRATED_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("hydrated")
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .description(createDescription("This allows you to hydrate new values into fields"))
                .repeatable(true)
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("service")
                                .description(createDescription("The target service"))
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("field")
                                .description(createDescription("The target top level field"))
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("identifiedBy")
                                .description(createDescription("How to identify matching results"))
                                .type(nonNull("String"))
                                .defaultValue(StringValue.newStringValue("id").build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("indexed")
                                .description(createDescription("Are results indexed"))
                                .type(typeOf("Boolean"))
                                .defaultValue(BooleanValue.newBooleanValue(false).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("batched")
                                .description(createDescription("Is querying batched"))
                                .type(typeOf("Boolean"))
                                .defaultValue(BooleanValue.newBooleanValue(false).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("batchSize")
                                .description(createDescription("The batch size"))
                                .type(typeOf("Int"))
                                .defaultValue(IntValue.newIntValue().value(BigInteger.valueOf(200)).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("timeout")
                                .description(createDescription("The timeout to use when completing hydration"))
                                .type(typeOf("Int"))
                                .defaultValue(IntValue.newIntValue().value(BigInteger.valueOf(-1)).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("arguments")
                                .description(createDescription("The arguments to the hydrated field"))
                                .type(newListType().type(nonNull("NadelHydrationArgument")).build())
                                .build())
                .build();

        NADEL_HYDRATION_FROM_ARGUMENT_DEFINITION = InputObjectTypeDefinition.newInputObjectDefinition()
                .name("NadelHydrationFromArgument")
                .description(createDescription("This allows you to hydrate new values into fields with the @hydratedFrom directive"))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("name")
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("valueFromField")
                                .type(typeOf("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("valueFromArg")
                                .type(typeOf("String"))
                                .build())
                .build();

        NADEL_HYDRATION_TEMPLATE_ENUM_DEFINITION = EnumTypeDefinition.newEnumTypeDefinition()
                .name("NadelHydrationTemplate")
                .enumValueDefinition(EnumValueDefinition.newEnumValueDefinition().name("NADEL_PLACEHOLDER").build())
                .build();

        HYDRATED_FROM_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("hydratedFrom")
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .description(createDescription("This allows you to hydrate new values into fields"))
                .repeatable(true)
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("arguments")
                                .description(createDescription("The arguments to the hydrated field"))
                                .type(newListType().type(nonNull("NadelHydrationFromArgument")).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("template")
                                .description(createDescription("The hydration template to use"))
                                .type(nonNull("NadelHydrationTemplate"))
                                .build())
                .build();

        HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("hydratedTemplate")
                .directiveLocation(newDirectiveLocation().name(ENUM_VALUE.name()).build())
                .description(createDescription("This template directive provides common values to hydrated fields"))
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("service")
                                .description(createDescription("The target service"))
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("field")
                                .description(createDescription("The target top level field"))
                                .type(nonNull("String"))
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("identifiedBy")
                                .description(createDescription("How to identify matching results"))
                                .type(nonNull("String"))
                                .defaultValue(StringValue.newStringValue("id").build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("indexed")
                                .description(createDescription("Are results indexed"))
                                .type(typeOf("Boolean"))
                                .defaultValue(BooleanValue.newBooleanValue(false).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("batched")
                                .description(createDescription("Is querying batched"))
                                .type(typeOf("Boolean"))
                                .defaultValue(BooleanValue.newBooleanValue(false).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("batchSize")
                                .description(createDescription("The batch size"))
                                .type(typeOf("Int"))
                                .defaultValue(IntValue.newIntValue().value(BigInteger.valueOf(200)).build())
                                .build())
                .inputValueDefinition(
                        newInputValueDefinition()
                                .name("timeout")
                                .description(createDescription("The timeout in milliseconds"))
                                .type(typeOf("Int"))
                                .defaultValue(IntValue.newIntValue().value(BigInteger.valueOf(-1)).build())
                                .build())
                .build();

        HIDDEN_DIRECTIVE_DEFINITION = DirectiveDefinition.newDirectiveDefinition()
                .name("hidden")
                .description(createDescription("Indicates that the field is not available for queries or introspection"))
                .directiveLocation(newDirectiveLocation().name(FIELD_DEFINITION.name()).build())
                .build();
    }

    private static TypeName typeOf(String typename) {
        return newTypeName().name(typename).build();
    }

    private static NonNullType nonNull(String typeName) {
        return NonNullType.newNonNullType(typeOf(typeName)).build();
    }

    private static Description createDescription(String s) {
        return new Description(s, null, false);
    }

    public static List<UnderlyingServiceHydration> createUnderlyingServiceHydration(GraphQLFieldDefinition fieldDefinition, GraphQLSchema overallSchema) {

        List<GraphQLDirective> hydrationDirectives = FpKit.concat(
                fieldDefinition.getDirectives(HYDRATED_DIRECTIVE_DEFINITION.getName()),
                fieldDefinition.getDirectives(HYDRATED_FROM_DIRECTIVE_DEFINITION.getName())
        );
        return FpKit.map(hydrationDirectives,
                directive -> {

                    if (directive.getName().equals(HYDRATED_FROM_DIRECTIVE_DEFINITION.getName())) {
                        return createTemplatedUnderlyingServiceHydration(directive, overallSchema);
                    }
                    GraphQLArgument graphQLArgument = directive.getArgument("arguments");
                    List<Object> argumentValues = resolveArgumentValue(graphQLArgument);
                    List<RemoteArgumentDefinition> arguments = createArgs(argumentValues);

                    return buildHydrationParameters(directive, arguments);
                });
    }

    private static UnderlyingServiceHydration buildHydrationParameters(GraphQLDirective directive, List<RemoteArgumentDefinition> arguments) {
        String service = getDirectiveValue(directive, "service", String.class);
        String field = getDirectiveValue(directive, "field", String.class);
        String objectIdentifier = getDirectiveValue(directive, "identifiedBy", String.class);
        Boolean objectIndexed = getDirectiveValue(directive, "indexed", Boolean.class, false);
        // Note: this is not properly implemented yet, so the value does not matter
        boolean batched = false; // getDirectiveValue(directive, "batched", Boolean.class, false);
        if (objectIndexed) {
            objectIdentifier = null; // we cant have both but it has a default
        }
        int batchSize = getDirectiveValue(directive, "batchSize", Integer.class, 200);
        int timeout = getDirectiveValue(directive, "timeout", Integer.class, -1);

        List<String> fieldNames = dottedString(field);
        assertTrue(fieldNames.size() >= 1);
        String topLevelFieldName = fieldNames.get(0);
        String syntheticField = null;
        if (fieldNames.size() > 1) {
            syntheticField = fieldNames.get(0);
            topLevelFieldName = fieldNames.get(1);
        }

        // nominally this should be some other data class that's not an AST element
        // but history is what it is, and it's an AST element that's' really a data class
        return new UnderlyingServiceHydration(
                emptySrc(),
                emptyList(),
                service,
                topLevelFieldName,
                syntheticField,
                arguments,
                objectIdentifier,
                objectIndexed,
                batched,
                batchSize,
                timeout,
                emptyMap()
        );
    }

    private static UnderlyingServiceHydration createTemplatedUnderlyingServiceHydration(GraphQLDirective hydratedFromDirective, GraphQLSchema overallSchema) {

        GraphQLArgument template = hydratedFromDirective.getArgument("template");
        String enumTargetName = resolveArgumentValue(template);
        GraphQLEnumType templateEnumType = overallSchema.getTypeAs("NadelHydrationTemplate");
        assertNotNull(templateEnumType, () -> "There MUST be a enum called NadelHydrationTemplate");

        GraphQLEnumValueDefinition enumValue = templateEnumType.getValue(enumTargetName);
        assertNotNull(enumValue, () -> String.format("There MUST be a enum value in NadelHydrationTemplate called '%s'", enumTargetName));

        GraphQLDirective templateDirective = enumValue.getDirective(HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION.getName());
        assertNotNull(templateDirective, () -> String.format("The enum value '%s' in NadelHydrationTemplate must have a directive called '%s'", enumTargetName, HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION.getName()));

        GraphQLArgument graphQLArgument = hydratedFromDirective.getArgument("arguments");
        List<Object> argumentValues = resolveArgumentValue(graphQLArgument);
        List<RemoteArgumentDefinition> arguments = createTemplatedHydratedArgs(argumentValues);

        return buildHydrationParameters(templateDirective, arguments);
    }

    private static <T> T resolveArgumentValue(GraphQLArgument graphQLArgument) {
        //noinspection unchecked
        return (T) ValuesResolver.valueToInternalValue(graphQLArgument.getArgumentValue(), graphQLArgument.getType());
    }

    @SuppressWarnings("unchecked")
    private static List<RemoteArgumentDefinition> createArgs(List<Object> arguments) {
        List<RemoteArgumentDefinition> remoteArgumentDefinitions = new ArrayList<>();
        for (Object arg : arguments) {
            Map<String, String> argMap = (Map<String, String>) arg;

            String remoteArgName = argMap.get("name");
            String remoteArgValue = argMap.get("value");
            RemoteArgumentSource remoteArgumentSource = createRemoteArgumentSource(remoteArgValue);
            RemoteArgumentDefinition remoteArgumentDefinition = new RemoteArgumentDefinition(remoteArgName, remoteArgumentSource, emptySrc(), emptyMap());
            remoteArgumentDefinitions.add(remoteArgumentDefinition);
        }
        return remoteArgumentDefinitions;
    }

    private static RemoteArgumentSource createRemoteArgumentSource(String value) {
        List<String> values = dottedString(String.valueOf(value));
        assertTrue(values.size() >= 2);

        String type = values.get(0);
        RemoteArgumentSource.SourceType argumentType = null;
        String argumentName = null;
        List<String> path = null;
        if ("$source".equals(type)) {
            argumentType = OBJECT_FIELD;
            path = values.subList(1, values.size());
        } else if ("$argument".equals(type)) {
            argumentType = FIELD_ARGUMENT;
            argumentName = values.get(1);
        } else if ("$context".equals(type)) {
            argumentType = CONTEXT;
            argumentName = values.get(1);
        } else {
            Assert.assertShouldNeverHappen("The hydration arguments are invalid : %s", value);
        }
        return new RemoteArgumentSource(argumentName, path, argumentType, emptySrc(), emptyMap());
    }

    @SuppressWarnings("unchecked")
    private static List<RemoteArgumentDefinition> createTemplatedHydratedArgs(List<Object> arguments) {
        List<RemoteArgumentDefinition> remoteArgumentDefinitions = new ArrayList<>();
        for (Object arg : arguments) {
            Map<String, String> argMap = (Map<String, String>) arg;

            String remoteArgName = argMap.get("name");
            String remoteArgFieldValue = argMap.get("valueFromField");
            String remoteArgArgValue = argMap.get("valueFromArg");
            boolean bothSet = remoteArgArgValue != null && remoteArgFieldValue != null;
            boolean noneSet = remoteArgArgValue == null && remoteArgFieldValue == null;
            Assert.assertTrue(!(bothSet || noneSet), () -> "You must specify only one of valueForField or valueForArg in NadelHydrationFromArgument arguments");

            RemoteArgumentSource remoteArgumentSource;
            if (remoteArgFieldValue != null) {
                remoteArgumentSource = createTemplatedRemoteArgumentSource(remoteArgFieldValue, OBJECT_FIELD);
            } else {
                remoteArgumentSource = createTemplatedRemoteArgumentSource(remoteArgArgValue, FIELD_ARGUMENT);
            }
            RemoteArgumentDefinition remoteArgumentDefinition = new RemoteArgumentDefinition(remoteArgName, remoteArgumentSource, emptySrc(), emptyMap());
            remoteArgumentDefinitions.add(remoteArgumentDefinition);
        }
        return remoteArgumentDefinitions;
    }

    private static RemoteArgumentSource createTemplatedRemoteArgumentSource(String value, RemoteArgumentSource.SourceType argumentType) {
        // for backwards compat reasons - we will allow them to specify "$source.field.name" and treat it as just "field.name"
        value = removePrefix(value, "$source.");
        value = removePrefix(value, "$argument.");
        List<String> values = dottedString(value);
        String argumentName = null;
        List<String> path = null;
        if (argumentType == OBJECT_FIELD) {
            path = values;
        }
        if (argumentType == FIELD_ARGUMENT) {
            argumentName = values.get(0);
        }
        return new RemoteArgumentSource(argumentName, path, argumentType, emptySrc(), emptyMap());
    }

    private static String removePrefix(String value, String prefix) {
        if (value.startsWith(prefix)) {
            return value.replace(prefix, "");
        }
        return value;
    }

    public static FieldMappingDefinition createFieldMapping(GraphQLFieldDefinition fieldDefinition) {
        GraphQLDirective directive = fieldDefinition.getDirective(RENAMED_DIRECTIVE_DEFINITION.getName());
        if (directive == null) {
            return null;
        }

        String fromValue = getDirectiveValue(directive, "from", String.class);
        List<String> fromPath = dottedString(String.valueOf(fromValue));
        return new FieldMappingDefinition(fromPath, emptySrc(), emptyList(), emptyMap());
    }

    public static TypeMappingDefinition createTypeMapping(GraphQLDirectiveContainer directivesContainer) {
        GraphQLDirective directive = directivesContainer.getDirective(RENAMED_DIRECTIVE_DEFINITION.getName());
        if (directive == null) {
            return null;
        }

        String from = getDirectiveValue(directive, "from", String.class);
        TypeMappingDefinition typeMappingDefinition = new TypeMappingDefinition(emptySrc(), emptyList(), emptyMap());
        typeMappingDefinition.setOverallName(directivesContainer.getName());
        typeMappingDefinition.setUnderlyingName(from);
        return typeMappingDefinition;
    }

    private static List<String> dottedString(String from) {
        String[] split = from.split("\\.");
        return Arrays.asList(split);
    }

    private static SourceLocation emptySrc() {
        return new SourceLocation(-1, -1);
    }

    private static <T> T getDirectiveValueImpl(GraphQLDirective directive, String name, Class<T> clazz, boolean allowMissingArg) {
        GraphQLArgument argument = directive.getArgument(name);
        if (allowMissingArg && argument == null) {
            return null;
        }
        assertNotNull(argument, () -> String.format("The @%s directive argument '%s argument MUST be present - how is this possible?", directive.getName(), name));
        Object value = resolveArgumentValue(argument);
        return clazz.cast(value);
    }

    private static <T> T getDirectiveValue(GraphQLDirective directive, String name, Class<T> clazz) {
        return getDirectiveValueImpl(directive, name, clazz, false);
    }

    private static <T> T getDirectiveValue(GraphQLDirective directive, String name, Class<T> clazz, T defaultValue) {
        T value = getDirectiveValueImpl(directive, name, clazz, true);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
