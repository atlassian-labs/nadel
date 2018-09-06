package graphql.nadel;

import com.atlassian.braid.Braid;
import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.TypeUtils;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.Definition;
import graphql.language.FieldDefinition;
import graphql.language.SDLDefinition;
import graphql.nadel.TransformationUtils.FieldDefinitionWithParentType;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;
import static graphql.nadel.TransformationUtils.collectFieldTransformations;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@PublicApi
public class Nadel {
    private static final String BASE_SERVICE_NAME = "base";

    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private Braid braid;

    private final Map<String, TypeDefinitionRegistry> typesByService = new LinkedHashMap<>();
    private final Map<String, SchemaNamespace> namespaceByService = new LinkedHashMap<>();

    private Nadel(String dsl, SchemaSourceFactory schemaSourceFactory, DataFetcherFactory fetcherFactory) {
        assertNotNull(dsl, "dsl");
        assertNotNull(schemaSourceFactory, "schemaSourceFactory");
        assertNotNull(fetcherFactory, "fetcherFactory");
        this.stitchingDsl = this.parser.parseDSL(dsl);

        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.typesByService.put(serviceDefinition.getName(), typeDefinitionRegistry);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            namespaceByService.put(serviceDefinition.getName(), schemaNamespace);

        }
        Braid.BraidBuilder braidBuilder = Braid.builder();


        List<SchemaSource> schemaSources = new ArrayList<>();
        List<TypeRuntimeWiring> wirings = new ArrayList<>();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            SchemaNamespace namespace = namespaceByService.get(serviceDefinition.getName());
            TypeDefinitionRegistry typeDefinitionRegistry = typesByService.get(serviceDefinition.getName());
            boolean isBaseService = serviceDefinition.getName().equals(BASE_SERVICE_NAME);

            List<Link> links = new ArrayList<>();
            processTransformations(serviceDefinition, fetcherFactory, links, wirings);

            if (isBaseService) {
                braidBuilder.typeDefinitionRegistry(typeDefinitionRegistry);
            } else {
                SchemaSource schemaSource = schemaSourceFactory.createSchemaSource(serviceDefinition, namespace,
                        typeDefinitionRegistry, links);
                if (schemaSource == null) {
                    throw new InvalidDslException("No schema source for service " + serviceDefinition.getName(),
                            serviceDefinition.getSourceLocation());
                }
                schemaSources.add(schemaSource);
            }
        }
        braidBuilder.withRuntimeWiring(builder -> wirings.forEach(builder::type));

        AsyncExecutionStrategy asyncExecutionStrategy = new AsyncExecutionStrategy();

        this.braid = braidBuilder
                .executionStrategy(asyncExecutionStrategy)
                .schemaSources(schemaSources)
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
        List<GraphQLError> errors = new ArrayList<>();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (Definition definition : serviceDefinition.getTypeDefinitions()) {
            if (definition instanceof SDLDefinition) {
                typeRegistry.add((SDLDefinition) definition).ifPresent(errors::add);
            }
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }


    private void processTransformations(ServiceDefinition serviceDefinition,
                                        DataFetcherFactory fetcherFactory,
                                        List<Link> links,
                                        List<TypeRuntimeWiring> wirings) {

        SchemaNamespace schemaNamespace = assertNotNull(this.namespaceByService.get(serviceDefinition.getName()));

        final List<FieldDefinitionWithParentType> defs = collectFieldTransformations(serviceDefinition);

        for (FieldDefinitionWithParentType definition : defs) {
            final FieldTransformation transformation = definition.field().getFieldTransformation();
            if (transformation.getInnerServiceHydration() != null) {
                final InnerServiceHydration hydration = transformation.getInnerServiceHydration();
                final Link link = createHydrationLink(schemaNamespace, definition, hydration);
                links.add(link);
            } else if (transformation.getFieldMappingDefinition() != null) {
                throw new InvalidDslException("Field mapping not implemented yet.", transformation.getSourceLocation());
            } else if (transformation.getFieldDataFetcher() != null) {
                final String fetcherName = transformation.getFieldDataFetcher().getDataFetcherName();
                final DataFetcher<Object> dataFetcher = fetcherFactory.createDataFetcher(fetcherName);
                final String fieldName = definition.field().getName();
                if (dataFetcher == null) {
                    throw new InvalidDslException(
                            format("Data fetcher '%s' for field '%s' does not exist", fetcherName, fieldName),
                            definition.field().getSourceLocation());
                }
                wirings.add(
                        TypeRuntimeWiring.newTypeWiring(definition.parentType())
                                .dataFetcher(fieldName, dataFetcher)
                                .build()
                );
            }
        }
    }

    private Link createHydrationLink(SchemaNamespace schemaNamespace, FieldDefinitionWithParentType definition,
                                     InnerServiceHydration hydration) {
        SchemaNamespace targetService = assertNotNull(this.namespaceByService.get(hydration.getServiceName()));
        final FieldDefinition targetField = findTargetFieldForHydration(hydration);
        //TODO: will not work for lists or non nullable types. does braid support this at all?
        String targetTypeName = TypeInfo.typeInfo(targetField.getType()).getName();

        //TODO: braid does not support multiple arguments, so we just take first
        final String fromField = hydration.getArguments().entrySet().stream()
                .map(e -> e.getValue().getInputName())
                .findFirst()
                .orElse(definition.field().getName());

        return Link.from(schemaNamespace, definition.parentType(), definition.field().getName(), fromField)
                //TODO: we need to add something to DSL to support 'queryVariableArgument' parameter of .to
                // by default it is targetField name which is not always correct.
                .to(targetService, targetTypeName, targetField.getName())
                .replaceFromField()
                .build();
    }


    private FieldDefinition findTargetFieldForHydration(InnerServiceHydration hydration) {
        final TypeDefinitionRegistry types = typesByService.get(hydration.getServiceName());
        if (types == null) {
            throw hydrationError(hydration, "Service '%s' is not defined.", hydration.getServiceName());
        }

        return TypeUtils.findQueryFieldDefinitions(types)
                .flatMap(queryFields -> queryFields.stream()
                        .filter(field -> hydration.getTopLevelField().equals(field.getName()))
                        .findFirst())
                .orElseThrow(() -> hydrationError(hydration, "Service '%s' does not contain query field '%s'",
                        hydration.getServiceName(), hydration.getTopLevelField()));
    }

    private InvalidDslException hydrationError(InnerServiceHydration hydration, String format, Object... args) {
        return new InvalidDslException(format("Error in field hydration definition: " + format, args),
                hydration.getSourceLocation());
    }


    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
    }

    public static class Builder {
        private String dsl;
        private SchemaSourceFactory schemaSourceFactory = SchemaSourceFactory.DEFAULT;
        private DataFetcherFactory dataFetcherFactory = DataFetcherFactory.DEFAULT;

        public Builder withDsl(String dsl) {
            this.dsl = dsl;
            return this;
        }

        public Builder withGraphQLRemoteRetrieverFactory(GraphQLRemoteRetrieverFactory<?> remoteFactory) {
            this.schemaSourceFactory = new GraphQLRemoteSchemaSourceFactory<>(
                    requireNonNull(remoteFactory, "remoteFactory")
            );
            return this;
        }

        public Builder withSchemaSourceFactory(SchemaSourceFactory schemaSourceFactory) {
            this.schemaSourceFactory = requireNonNull(schemaSourceFactory, "schemaSourceFactory");
            return this;
        }

        public Builder withDataFetcherFactory(DataFetcherFactory dataFetcherFactory) {
            this.dataFetcherFactory = requireNonNull(dataFetcherFactory, "dataFetcherFactory");
            return this;
        }

        public Nadel build() {
            return new Nadel(dsl, schemaSourceFactory, dataFetcherFactory);

        }
    }

}
