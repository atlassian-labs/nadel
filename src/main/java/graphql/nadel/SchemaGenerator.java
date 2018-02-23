package graphql.nadel;

import graphql.Scalars;
import graphql.introspection.Introspection;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValue;
import graphql.language.FieldDefinition;
import graphql.language.FloatValue;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.IntValue;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.Node;
import graphql.language.NullValue;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.language.ObjectValue;
import graphql.language.ScalarTypeDefinition;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.language.Value;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.Coercing;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetcherFactoryEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.EnumValuesProvider;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.errors.NotAnInputTypeError;
import graphql.schema.idl.errors.NotAnOutputTypeError;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.introspection.Introspection.DirectiveLocation.ENUM;
import static graphql.introspection.Introspection.DirectiveLocation.ENUM_VALUE;
import static graphql.introspection.Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.INPUT_OBJECT;
import static graphql.introspection.Introspection.DirectiveLocation.INTERFACE;
import static graphql.introspection.Introspection.DirectiveLocation.OBJECT;
import static graphql.introspection.Introspection.DirectiveLocation.UNION;
import static graphql.schema.GraphQLEnumValueDefinition.newEnumValueDefinition;
import static graphql.schema.GraphQLTypeReference.typeRef;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class SchemaGenerator {

    /**
     * We pass this around so we know what we have defined in a stack like manner plus
     * it gives us helper functions
     */
    class BuildContext {
        final NadelTypeDefinitionRegistry typeRegistry;
        GraphqlCallerFactory graphqlCallerFactory;
        final Stack<String> definitionStack = new Stack<>();

        final Map<String, GraphQLOutputType> outputGTypes = new HashMap<>();
        final Map<String, GraphQLInputType> inputGTypes = new HashMap<>();

        BuildContext(NadelTypeDefinitionRegistry typeRegistry, GraphqlCallerFactory graphqlCallerFactory) {
            this.typeRegistry = typeRegistry;
            this.graphqlCallerFactory = graphqlCallerFactory;
        }

        @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
        TypeDefinition getTypeDefinition(Type type) {
            return typeRegistry.getTypeDefinitions(type).get();
        }

        public boolean stackContains(TypeInfo typeInfo) {
            return definitionStack.contains(typeInfo.getName());
        }

        public void push(TypeInfo typeInfo) {
            definitionStack.push(typeInfo.getName());
        }

        void pop() {
            definitionStack.pop();
        }

        GraphQLOutputType hasOutputType(TypeDefinition typeDefinition) {
            return outputGTypes.get(typeDefinition.getName());
        }

        GraphQLInputType hasInputType(TypeDefinition typeDefinition) {
            return inputGTypes.get(typeDefinition.getName());
        }

        void put(GraphQLOutputType outputType) {
            outputGTypes.put(outputType.getName(), outputType);
            // certain types can be both input and output types, for example enums
            if (outputType instanceof GraphQLInputType) {
                inputGTypes.put(outputType.getName(), (GraphQLInputType) outputType);
            }
        }

        void put(GraphQLInputType inputType) {
            inputGTypes.put(inputType.getName(), inputType);
            // certain types can be both input and output types, for example enums
            if (inputType instanceof GraphQLOutputType) {
                outputGTypes.put(inputType.getName(), (GraphQLOutputType) inputType);
            }
        }

        public List<TypeDefinition> getAllTypeDefinitions() {
            return typeRegistry.getAllTypeDefinitions();
        }

        public List<TypeDefinition> getTypeDefinitions(String name) {
            return typeRegistry.getTypeDefinitions(name);
        }

        public ServiceDefinition getServiceForField(FieldDefinition fieldDefinition) {
            return typeRegistry.getStitchingDsl().getServiceByField().get(fieldDefinition);
        }

        public GraphqlCaller createCaller(ServiceDefinition serviceDefinition) {
            return graphqlCallerFactory.createGraphqlCaller(serviceDefinition);
        }

        public FieldTransformation getFieldTransformation(FieldDefinition fieldDefinition) {
            return typeRegistry.getStitchingDsl().getTransformationsByFieldDefinition().get(fieldDefinition);
        }
    }

//    private final NadelSchemaTypeChecker typeChecker = new NadelSchemaTypeChecker();

    public SchemaGenerator() {
    }

    public GraphQLSchema makeExecutableSchema(NadelTypeDefinitionRegistry typeRegistry, GraphqlCallerFactory graphqlCallerFactory) throws SchemaProblem {
//        List<GraphQLError> errors = typeChecker.checkTypeRegistry(typeRegistry);

        BuildContext buildCtx = new BuildContext(typeRegistry, graphqlCallerFactory);

        return makeExecutableSchemaImpl(buildCtx);
    }

    private GraphQLSchema makeExecutableSchemaImpl(BuildContext buildCtx) {
        GraphQLObjectType query = buildQueryType(buildCtx);

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
        schemaBuilder.query(query);

        Set<GraphQLType> additionalTypes = buildAdditionalTypes(buildCtx);
        return schemaBuilder.build(additionalTypes);
    }

    private GraphQLObjectType buildQueryType(BuildContext buildContext) {
        List<TypeDefinition> queryDefinitions = buildContext.getTypeDefinitions("Query");
        assertTrue(queryDefinitions.size() > 0, "At least of Query definition is required");
        ObjectTypeDefinition firstQueryDefinition = (ObjectTypeDefinition) queryDefinitions.get(0);

        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();
        builder.definition((ObjectTypeDefinition) queryDefinitions.get(0));
        builder.name("Query");
        //TODO: merge all query object infos, not just the first
        builder.description(buildDescription(firstQueryDefinition, firstQueryDefinition.getDescription()));

        Map<String, GraphQLFieldDefinition> fieldDefinitions = new LinkedHashMap<>();
        for (TypeDefinition typeDefinition : queryDefinitions) {
            ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) typeDefinition;
            objectTypeDefinition.getFieldDefinitions().forEach(fieldDef -> {

                ServiceDefinition serviceDefinition = buildContext.getServiceForField(fieldDef);
                GraphqlCaller caller = buildContext.createCaller(serviceDefinition);
                RemoteRootQueryDataFetcher remoteRootQueryDataFetcher = new RemoteRootQueryDataFetcher(serviceDefinition, caller, buildContext.typeRegistry.getStitchingDsl());
                GraphQLFieldDefinition newFieldDefinition = buildField(buildContext, typeDefinition, fieldDef, remoteRootQueryDataFetcher);
                fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);

            });
        }
        fieldDefinitions.values().forEach(builder::field);


        GraphQLObjectType queryType = builder.build();
        buildContext.put(queryType);
        return queryType;
    }

    private Set<GraphQLType> buildAdditionalTypes(BuildContext buildCtx) {
        Set<GraphQLType> additionalTypes = new HashSet<>();
        List<TypeDefinition> allTypes = buildCtx.getAllTypeDefinitions();
        allTypes.forEach(typeDefinition -> {
            TypeName typeName = new TypeName(typeDefinition.getName());
            if (typeDefinition instanceof InputObjectTypeDefinition) {
                if (buildCtx.hasInputType(typeDefinition) == null) {
                    additionalTypes.add(buildInputType(buildCtx, typeName));
                }
            } else {
                if (buildCtx.hasOutputType(typeDefinition) == null) {
                    additionalTypes.add(buildOutputType(buildCtx, typeName));
                }
            }
        });
        return additionalTypes;
    }

    @SuppressWarnings("unchecked")
    private <T extends GraphQLOutputType> T buildOutputType(BuildContext buildCtx, Type rawType) {

        TypeDefinition typeDefinition = buildCtx.getTypeDefinition(rawType);
        TypeInfo typeInfo = TypeInfo.typeInfo(rawType);

        GraphQLOutputType outputType = buildCtx.hasOutputType(typeDefinition);
        if (outputType != null) {
            return typeInfo.decorate(outputType);
        }

        if (buildCtx.stackContains(typeInfo)) {
            // we have circled around so put in a type reference and fix it up later
            // otherwise we will go into an infinite loop
            return typeInfo.decorate(typeRef(typeInfo.getName()));
        }

        buildCtx.push(typeInfo);

        if (typeDefinition instanceof ObjectTypeDefinition) {
            outputType = buildObjectType(buildCtx, (ObjectTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof InterfaceTypeDefinition) {
            outputType = buildInterfaceType(buildCtx, (InterfaceTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof UnionTypeDefinition) {
            outputType = buildUnionType(buildCtx, (UnionTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof EnumTypeDefinition) {
            outputType = buildEnumType(buildCtx, (EnumTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof ScalarTypeDefinition) {
            outputType = buildScalar(buildCtx, (ScalarTypeDefinition) typeDefinition);
        } else {
            // typeDefinition is not a valid output type
            throw new NotAnOutputTypeError(typeDefinition);
        }

        buildCtx.put(outputType);
        buildCtx.pop();
        return (T) typeInfo.decorate(outputType);
    }

    private GraphQLInputType buildInputType(BuildContext buildCtx, Type rawType) {

        TypeDefinition typeDefinition = buildCtx.getTypeDefinition(rawType);
        TypeInfo typeInfo = TypeInfo.typeInfo(rawType);

        GraphQLInputType inputType = buildCtx.hasInputType(typeDefinition);
        if (inputType != null) {
            return typeInfo.decorate(inputType);
        }

        if (buildCtx.stackContains(typeInfo)) {
            // we have circled around so put in a type reference and fix it later
            return typeInfo.decorate(typeRef(typeInfo.getName()));
        }

        buildCtx.push(typeInfo);

        if (typeDefinition instanceof InputObjectTypeDefinition) {
            inputType = buildInputObjectType(buildCtx, (InputObjectTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof EnumTypeDefinition) {
            inputType = buildEnumType(buildCtx, (EnumTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof ScalarTypeDefinition) {
            inputType = buildScalar(buildCtx, (ScalarTypeDefinition) typeDefinition);
        } else {
            // typeDefinition is not a valid InputType
            throw new NotAnInputTypeError(typeDefinition);
        }

        buildCtx.put(inputType);
        buildCtx.pop();
        return typeInfo.decorate(inputType);
    }

    private GraphQLObjectType buildObjectType(BuildContext buildCtx, ObjectTypeDefinition typeDefinition) {

        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();
        builder.definition(typeDefinition);
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition, typeDefinition.getDescription()));

        List<ObjectTypeExtensionDefinition> typeExtensions = getTypeExtensionsOf(typeDefinition, buildCtx);

        List<Directive> extensionDirectives = typeExtensions.stream()
                .map(ObjectTypeDefinition::getDirectives).filter(Objects::nonNull)
                .flatMap(Collection::stream).collect(Collectors.toList());

        // combine the type and its extension directives together as one
        builder.withDirectives(
                buildDirectives(buildCtx, typeDefinition.getDirectives(),
                        extensionDirectives, OBJECT)
        );

        buildObjectTypeFields(buildCtx, typeDefinition, builder, typeExtensions);
        buildObjectTypeInterfaces(buildCtx, typeDefinition, builder, typeExtensions);

        return builder.build();
    }

    private void buildObjectTypeFields(BuildContext buildCtx, ObjectTypeDefinition typeDefinition, GraphQLObjectType.Builder builder, List<ObjectTypeExtensionDefinition> typeExtensions) {
        Map<String, GraphQLFieldDefinition> fieldDefinitions = new LinkedHashMap<>();

        typeDefinition.getFieldDefinitions().forEach(fieldDef -> {
            GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
            fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);
        });

        // an object consists of the fields it gets from its definition AND its type extensions
        typeExtensions.forEach(typeExt -> typeExt.getFieldDefinitions().forEach(fieldDef -> {
            GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
            //
            // de-dupe here - pre-flight checks ensure all dupes are of the same type
            if (!fieldDefinitions.containsKey(newFieldDefinition.getName())) {
                fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);
            }
        }));

        fieldDefinitions.values().forEach(builder::field);
    }

    private void buildObjectTypeInterfaces(BuildContext buildCtx, ObjectTypeDefinition typeDefinition, GraphQLObjectType.Builder builder, List<ObjectTypeExtensionDefinition> typeExtensions) {
        Map<String, GraphQLInterfaceType> interfaces = new LinkedHashMap<>();
        typeDefinition.getImplements().forEach(type -> {
            GraphQLInterfaceType newInterfaceType = buildOutputType(buildCtx, type);
            interfaces.put(newInterfaceType.getName(), newInterfaceType);
        });

        // an object consists of the interfaces it gets from its definition AND its type extensions
        typeExtensions.forEach(typeExt -> typeExt.getImplements().forEach(type -> {
            GraphQLInterfaceType interfaceType = buildOutputType(buildCtx, type);
            //
            // de-dupe here - pre-flight checks ensure all dupes are of the same type
            if (!interfaces.containsKey(interfaceType.getName())) {
                interfaces.put(interfaceType.getName(), interfaceType);
            }
        }));

        interfaces.values().forEach(builder::withInterface);
    }

    private List<ObjectTypeExtensionDefinition> getTypeExtensionsOf(ObjectTypeDefinition objectTypeDefinition, BuildContext buildCtx) {
        List<ObjectTypeExtensionDefinition> ObjectTypeExtensionDefinitions = buildCtx.typeRegistry.typeExtensions().get(objectTypeDefinition.getName());
        return ObjectTypeExtensionDefinitions == null ? emptyList() : ObjectTypeExtensionDefinitions;
    }

    private GraphQLInterfaceType buildInterfaceType(BuildContext buildCtx, InterfaceTypeDefinition typeDefinition) {
        GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface();
        builder.definition(typeDefinition);
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition, typeDefinition.getDescription()));

        builder.typeResolver(getTypeResolverForInterface(buildCtx, typeDefinition));

        typeDefinition.getFieldDefinitions().forEach(fieldDef ->
                builder.field(buildField(buildCtx, typeDefinition, fieldDef)));

        builder.withDirectives(
                buildDirectives(buildCtx, typeDefinition.getDirectives(),
                        emptyList(), INTERFACE)
        );

        return builder.build();
    }

    private GraphQLUnionType buildUnionType(BuildContext buildCtx, UnionTypeDefinition typeDefinition) {
        GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType();
        builder.definition(typeDefinition);
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition, typeDefinition.getDescription()));
        builder.typeResolver(getTypeResolverForUnion(buildCtx, typeDefinition));

        typeDefinition.getMemberTypes().forEach(mt -> {
            GraphQLOutputType outputType = buildOutputType(buildCtx, mt);
            if (outputType instanceof GraphQLTypeReference) {
                builder.possibleType((GraphQLTypeReference) outputType);
            } else {
                builder.possibleType((GraphQLObjectType) outputType);
            }
        });

        builder.withDirectives(
                buildDirectives(buildCtx, typeDefinition.getDirectives(),
                        emptyList(), UNION)
        );

        return builder.build();
    }

    private GraphQLEnumType buildEnumType(BuildContext buildCtx, EnumTypeDefinition typeDefinition) {
        GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum();
        builder.definition(typeDefinition);
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition, typeDefinition.getDescription()));

        EnumValuesProvider enumValuesProvider = null;
        typeDefinition.getEnumValueDefinitions().forEach(evd -> {
            String description = buildDescription(evd, evd.getDescription());
            String deprecation = buildDeprecationReason(evd.getDirectives());

            Object value;
            if (enumValuesProvider != null) {
                value = enumValuesProvider.getValue(evd.getName());
                assertNotNull(value, "EnumValuesProvider for %s returned null for %s", typeDefinition.getName(), evd.getName());
            } else {
                value = evd.getName();
            }
            builder.value(newEnumValueDefinition()
                    .name(evd.getName())
                    .value(value)
                    .description(description)
                    .deprecationReason(deprecation)
                    .withDirectives(
                            buildDirectives(buildCtx, evd.getDirectives(),
                                    emptyList(), ENUM_VALUE)
                    )
                    .build());
        });

        builder.withDirectives(
                buildDirectives(buildCtx, typeDefinition.getDirectives(),
                        emptyList(), ENUM)
        );

        return builder.build();
    }

    private GraphQLScalarType buildScalar(BuildContext buildCtx, ScalarTypeDefinition typeDefinition) {
        Optional<GraphQLScalarType> standardScalar = ScalarInfo.STANDARD_SCALARS.stream().filter(graphQLScalarType -> graphQLScalarType.getName().equals(typeDefinition.getName())).findFirst();
        return standardScalar.orElseGet(() -> new GraphQLScalarType(typeDefinition.getName(), "", new Coercing() {
            @Override
            public Object serialize(Object dataFetcherResult) {
                return null;
            }

            @Override
            public Object parseValue(Object input) {
                return null;
            }

            @Override
            public Object parseLiteral(Object input) {
                return null;
            }
        }));
    }

    private GraphQLFieldDefinition buildField(BuildContext buildCtx, TypeDefinition parentType, FieldDefinition fieldDef) {
        return buildField(buildCtx, parentType, fieldDef, null);
    }

    private GraphQLFieldDefinition buildField(BuildContext buildCtx, TypeDefinition parentType, FieldDefinition fieldDef, DataFetcher dataFetcher) {
        FieldTransformation fieldTransformation = buildCtx.getFieldTransformation(fieldDef);
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();

        builder.name(fieldTransformation != null ? fieldTransformation.getTargetName() : fieldDef.getName());
        builder.definition(fieldDef);
        builder.description(buildDescription(fieldDef, fieldDef.getDescription()));
        builder.deprecate(buildDeprecationReason(fieldDef.getDirectives()));

        GraphQLDirective[] directives = buildDirectives(buildCtx, fieldDef.getDirectives(),
                Collections.emptyList(), Introspection.DirectiveLocation.FIELD);
        builder.withDirectives(
                directives
        );

        fieldDef.getInputValueDefinitions().forEach(inputValueDefinition ->
                builder.argument(buildArgument(buildCtx, inputValueDefinition)));

        GraphQLOutputType fieldType;
        if (fieldTransformation != null) {
            fieldType = buildOutputType(buildCtx, fieldTransformation.getTargetType());
        } else {
            fieldType = buildOutputType(buildCtx, fieldDef.getType());
        }
        builder.type(fieldType);
        if (dataFetcher != null) {
            builder.dataFetcher(dataFetcher);
        } else {
            builder.dataFetcherFactory(buildDataFetcherFactory(buildCtx, parentType, fieldDef, fieldType, Arrays.asList(directives)));
        }


        return builder.build();
    }

    private DataFetcherFactory buildDataFetcherFactory(BuildContext buildCtx, TypeDefinition parentType, FieldDefinition fieldDef, GraphQLOutputType fieldType, List<GraphQLDirective> directives) {
        return new DataFetcherFactory() {
            @Override
            public DataFetcher get(DataFetcherFactoryEnvironment environment) {
                return null;
            }
        };
//        String fieldName = fieldDef.getName();
//        String parentTypeName = parentType.getName();
//        TypeDefinitionRegistry typeRegistry = buildCtx.getTypeRegistry();
//        RuntimeWiring runtimeWiring = buildCtx.getWiring();
//        WiringFactory wiringFactory = runtimeWiring.getWiringFactory();
//
//        FieldWiringEnvironment wiringEnvironment = new FieldWiringEnvironment(typeRegistry, parentType, fieldDef, fieldType, directives);
//
//        DataFetcherFactory<?> dataFetcherFactory;
//        if (wiringFactory.providesDataFetcherFactory(wiringEnvironment)) {
//            dataFetcherFactory = wiringFactory.getDataFetcherFactory(wiringEnvironment);
//            assertNotNull(dataFetcherFactory, "The WiringFactory indicated it provides a data fetcher factory but then returned null");
//        } else {
//            //
//            // ok they provide a data fetcher directly
//            DataFetcher<?> dataFetcher;
//            if (wiringFactory.providesDataFetcher(wiringEnvironment)) {
//                dataFetcher = wiringFactory.getDataFetcher(wiringEnvironment);
//                assertNotNull(dataFetcher, "The WiringFactory indicated it provides a data fetcher but then returned null");
//            } else {
//                dataFetcher = runtimeWiring.getDataFetcherForType(parentTypeName).get(fieldName);
//                if (dataFetcher == null) {
//                    dataFetcher = runtimeWiring.getDefaultDataFetcherForType(parentTypeName);
//                    if (dataFetcher == null) {
//                        dataFetcher = wiringFactory.getDefaultDataFetcher(wiringEnvironment);
//                        assertNotNull(dataFetcher, "The WiringFactory indicated MUST provide a default data fetcher as part of its contract");
//                    }
//                }
//            }
//            dataFetcherFactory = DataFetcherFactories.useDataFetcher(dataFetcher);
//        }
//
//        return dataFetcherFactory;
    }

    private GraphQLInputObjectType buildInputObjectType(BuildContext buildCtx, InputObjectTypeDefinition typeDefinition) {
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
        builder.definition(typeDefinition);
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition, typeDefinition.getDescription()));

        builder.withDirectives(
                buildDirectives(buildCtx, typeDefinition.getDirectives(),
                        emptyList(), INPUT_OBJECT)
        );

        typeDefinition.getInputValueDefinitions().forEach(fieldDef ->
                builder.field(buildInputField(buildCtx, fieldDef)));
        return builder.build();
    }

    private GraphQLInputObjectField buildInputField(BuildContext buildCtx, InputValueDefinition fieldDef) {
        GraphQLInputObjectField.Builder fieldBuilder = GraphQLInputObjectField.newInputObjectField();
        fieldBuilder.definition(fieldDef);
        fieldBuilder.name(fieldDef.getName());
        fieldBuilder.description(buildDescription(fieldDef, fieldDef.getDescription()));

        // currently the spec doesnt allow deprecations on InputValueDefinitions but it should!
        //fieldBuilder.deprecate(buildDeprecationReason(fieldDef.getDirectives()));
        GraphQLInputType inputType = buildInputType(buildCtx, fieldDef.getType());
        fieldBuilder.type(inputType);
        Value defaultValue = fieldDef.getDefaultValue();
        if (defaultValue != null) {
            fieldBuilder.defaultValue(buildValue(defaultValue, inputType));
        }

        fieldBuilder.withDirectives(
                buildDirectives(buildCtx, fieldDef.getDirectives(),
                        emptyList(), INPUT_FIELD_DEFINITION)
        );

        return fieldBuilder.build();
    }

    private GraphQLArgument buildArgument(BuildContext buildCtx, InputValueDefinition valueDefinition) {
        GraphQLArgument.Builder builder = GraphQLArgument.newArgument();
        builder.definition(valueDefinition);
        builder.name(valueDefinition.getName());
        builder.description(buildDescription(valueDefinition, valueDefinition.getDescription()));
        GraphQLInputType inputType = buildInputType(buildCtx, valueDefinition.getType());
        builder.type(inputType);
        Value defaultValue = valueDefinition.getDefaultValue();
        if (defaultValue != null) {
            builder.defaultValue(buildValue(defaultValue, inputType));
        }

        return builder.build();
    }


    private Object buildValue(Value value, GraphQLType requiredType) {
        Object result = null;
        if (requiredType instanceof GraphQLNonNull) {
            requiredType = ((GraphQLNonNull) requiredType).getWrappedType();
        }
        if (requiredType instanceof GraphQLScalarType) {
            result = ((GraphQLScalarType) requiredType).getCoercing().parseLiteral(value);
        } else if (value instanceof EnumValue && requiredType instanceof GraphQLEnumType) {
            result = ((EnumValue) value).getName();
        } else if (value instanceof ArrayValue && requiredType instanceof GraphQLList) {
            ArrayValue arrayValue = (ArrayValue) value;
            GraphQLType wrappedType = ((GraphQLList) requiredType).getWrappedType();
            result = arrayValue.getValues().stream()
                    .map(item -> this.buildValue(item, wrappedType)).collect(Collectors.toList());
        } else if (value instanceof ObjectValue && requiredType instanceof GraphQLInputObjectType) {
            result = buildObjectValue((ObjectValue) value, (GraphQLInputObjectType) requiredType);
        } else if (value != null && !(value instanceof NullValue)) {
            assertShouldNeverHappen(
                    "cannot build value of %s from %s", requiredType.getName(), String.valueOf(value));
        }
        return result;
    }

    private Object buildObjectValue(ObjectValue defaultValue, GraphQLInputObjectType objectType) {
        HashMap<String, Object> map = new LinkedHashMap<>();
        defaultValue.getObjectFields().forEach(of -> map.put(of.getName(),
                buildValue(of.getValue(), objectType.getField(of.getName()).getType())));
        return map;
    }

    @SuppressWarnings("Duplicates")
    private TypeResolver getTypeResolverForUnion(BuildContext buildCtx, UnionTypeDefinition unionType) {
        return env -> assertShouldNeverHappen();
    }

    @SuppressWarnings("Duplicates")
    private TypeResolver getTypeResolverForInterface(BuildContext buildCtx, InterfaceTypeDefinition interfaceType) {
        return env -> assertShouldNeverHappen();
    }

    protected String buildDescription(Node<?> node, Description description) {
        if (description != null) {
            return description.getContent();
        }
        List<Comment> comments = node.getComments();
        List<String> lines = new ArrayList<>();
        for (Comment comment : comments) {
            String commentLine = comment.getContent();
            if (commentLine.trim().isEmpty()) {
                lines.clear();
            } else {
                lines.add(commentLine);
            }
        }
        if (lines.size() == 0) return null;
        return lines.stream().collect(joining("\n"));
    }


    protected String buildDeprecationReason(List<Directive> directives) {
        directives = directives == null ? emptyList() : directives;
        Optional<Directive> directive = directives.stream().filter(d -> "deprecated".equals(d.getName())).findFirst();
        if (directive.isPresent()) {
            Map<String, String> args = directive.get().getArguments().stream().collect(toMap(
                    Argument::getName, arg -> ((StringValue) arg.getValue()).getValue()
            ));
            if (args.isEmpty()) {
                return "No longer supported"; // default value from spec
            } else {
                // pre flight checks have ensured its valid
                return args.get("reason");
            }
        }
        return null;
    }

    private GraphQLDirective[] buildDirectives(BuildContext buildCtx, List<Directive> directives, List<Directive> extensionDirectives, DirectiveLocation directiveLocation) {
        directives = directives == null ? emptyList() : directives;
        extensionDirectives = extensionDirectives == null ? emptyList() : extensionDirectives;
        Set<String> names = new HashSet<>();

        List<GraphQLDirective> output = new ArrayList<>();
        for (Directive directive : directives) {
            if (!names.contains(directive.getName())) {
                names.add(directive.getName());
                output.add(buildDirective(buildCtx, directive, directiveLocation));
            }
        }
        for (Directive directive : extensionDirectives) {
            if (!names.contains(directive.getName())) {
                names.add(directive.getName());
                output.add(buildDirective(buildCtx, directive, directiveLocation));
            }
        }
        return output.toArray(new GraphQLDirective[0]);
    }

    // builds directives from a type and its extensions
    private GraphQLDirective buildDirective(BuildContext buildCtx, Directive directive, DirectiveLocation directiveLocation) {
        List<GraphQLArgument> arguments = directive.getArguments().stream()
                .map(arg -> buildDirectiveArgument(buildCtx, arg))
                .collect(Collectors.toList());

        GraphQLDirective.Builder builder = GraphQLDirective.newDirective()
                .name(directive.getName())
                .description(buildDescription(directive, null))
                .validLocations(directiveLocation);

        for (GraphQLArgument argument : arguments) {
            builder.argument(argument);
        }
        return builder.build();
    }

    private GraphQLArgument buildDirectiveArgument(BuildContext buildCtx, Argument arg) {
        GraphQLArgument.Builder builder = GraphQLArgument.newArgument();
        builder.name(arg.getName());
        GraphQLInputType inputType = buildDirectiveInputType(buildCtx, arg.getValue());
        builder.type(inputType);
        builder.defaultValue(buildValue(arg.getValue(), inputType));
        return builder.build();
    }

    /**
     * We support the basic types as directive types
     *
     * @param buildCtx build ctx
     * @param value    the value to use
     *
     * @return a graphql input type
     */
    private GraphQLInputType buildDirectiveInputType(BuildContext buildCtx, Value value) {
        if (value instanceof NullValue) {
            return Scalars.GraphQLString;
        }
        if (value instanceof FloatValue) {
            return Scalars.GraphQLFloat;
        }
        if (value instanceof StringValue) {
            return Scalars.GraphQLString;
        }
        if (value instanceof IntValue) {
            return Scalars.GraphQLInt;
        }
        if (value instanceof BooleanValue) {
            return Scalars.GraphQLBoolean;
        }
        return assertShouldNeverHappen("Directive values of type '%s' are not supported yet", value.getClass().getName());
    }


}