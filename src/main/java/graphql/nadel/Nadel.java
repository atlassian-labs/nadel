package graphql.nadel;

import com.atlassian.braid.ArgumentValueProvider;
import com.atlassian.braid.BatchLoaderEnvironment;
import com.atlassian.braid.Braid;
import com.atlassian.braid.DefaultArgumentValueProvider;
import com.atlassian.braid.Link;
import com.atlassian.braid.LinkArgument;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.TypeRename;
import com.atlassian.braid.TypeUtils;
import com.atlassian.braid.document.TypeMapper;
import com.atlassian.braid.document.TypeMappers;
import com.atlassian.braid.transformation.SchemaTransformation;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.language.Definition;
import graphql.language.FieldDefinition;
import graphql.language.SDLDefinition;
import graphql.nadel.TransformationUtils.TransformationWithParentType;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.nadel.TransformationUtils.collectFieldTransformations;
import static graphql.nadel.TransformationUtils.collectObjectTypeDefinitionWithTransformations;
import static graphql.schema.idl.TypeInfo.typeInfo;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@PublicApi
public class Nadel {
    private final StitchingDsl stitchingDsl;
    private final Parser parser = new Parser();
    private final Braid braid;

    private final Map<String, TypeDefinitionRegistry> typesByService = new LinkedHashMap<>();
    private final Map<String, SchemaNamespace> namespaceByService = new LinkedHashMap<>();
    private final ArgumentValueProvider argumentValueProvider;
    private final PrivateSchemaProvider privateSchemaProvider;

    /**
     * Parses provided DSL and creates a stitched schema based on it.
     *
     * @param dsl                    string containing Nadel DSL.
     * @param schemaSourceFactory    schema source factory that provide {@link SchemaSource} for each service defined in
     *                               DSL.
     * @param transformationsFactory provides additional type definitions that will be added to the stitched schema. If no
     *                               additional types are needed {@link SchemaTransformationsFactory#DEFAULT} can be used.
     * @param batchLoaderEnvironment provides functions that will be used by braid batch loader.
     * @param instrumentations       the graphql instrumentations to use
     * @param argumentValueProvider  for providing  argument values to links.
     * @throws InvalidDslException in case there is an issue with DSL.
     */
    private Nadel(String dsl,
                  SchemaSourceFactory schemaSourceFactory,
                  SchemaTransformationsFactory transformationsFactory,
                  BatchLoaderEnvironment batchLoaderEnvironment,
                  List<Instrumentation> instrumentations,
                  ArgumentValueProvider argumentValueProvider,
                  PrivateSchemaProvider privateSchemaProvider) {
        requireNonNull(dsl, "dsl");
        requireNonNull(schemaSourceFactory, "schemaSourceFactory");
        requireNonNull(transformationsFactory, "transformationsFactory");
        requireNonNull(transformationsFactory, "transformationsFactory");
        requireNonNull(privateSchemaProvider, "privateSchemaProvider");
        this.argumentValueProvider = requireNonNull(argumentValueProvider, "argumentValueProvider");
        this.stitchingDsl = this.parser.parseDSL(dsl);
        this.privateSchemaProvider = privateSchemaProvider;

        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.typesByService.put(serviceDefinition.getName(), typeDefinitionRegistry);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            namespaceByService.put(serviceDefinition.getName(), schemaNamespace);

        }
        List<SchemaSource> schemaSources = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            SchemaNamespace namespace = namespaceByService.get(serviceDefinition.getName());
            TypeDefinitionRegistry typeDefinitionRegistry = typesByService.get(serviceDefinition.getName());

            final List<TransformationWithParentType<FieldDefinitionWithTransformation>> defs = collectFieldTransformations(serviceDefinition);

            List<TypeMapper> mappers = createMappers(defs);
            List<TypeRename> typeRenames = buildTypeRenames(collectObjectTypeDefinitionWithTransformations(serviceDefinition));
            List<Link> links = createLinks(namespace, defs);
            SchemaSource schemaSource = schemaSourceFactory.createSchemaSource(serviceDefinition, namespace,
                    typeDefinitionRegistry, links, mappers, typeRenames);
            schemaSources.add(schemaSource);
        }

        AsyncExecutionStrategy asyncExecutionStrategy = new AsyncExecutionStrategy();
        final List<SchemaTransformation> schemaTransformations = transformationsFactory.create(this.typesByService);
        Braid.BraidBuilder braidBuilder = Braid.builder()
                .executionStrategy(asyncExecutionStrategy)
                .customSchemaTransformations(schemaTransformations)
                .schemaSources(schemaSources)
                .batchLoaderEnvironment(batchLoaderEnvironment);
        instrumentations.forEach(braidBuilder::instrumentation);
        this.braid = braidBuilder
                .build();
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

    private List<Link> createLinks(SchemaNamespace schemaNamespace,
                                   List<TransformationWithParentType<FieldDefinitionWithTransformation>> defs) {
        List<Link> links = new ArrayList<>();

        for (TransformationWithParentType<FieldDefinitionWithTransformation> definition : defs) {
            final FieldTransformation transformation = definition.field().getFieldTransformation();
            if (transformation.getInnerServiceHydration() != null) {
                final InnerServiceHydration hydration = transformation.getInnerServiceHydration();
                final Link link = createHydrationLink(schemaNamespace, definition, hydration);
                links.add(link);
            }
        }
        return links;
    }

    private List<TypeRename> buildTypeRenames(
            List<TransformationWithParentType<ObjectTypeDefinitionWithTransformation>> defs
    ) {
        return defs.stream().filter(t -> t.field() != null && t.field().getTypeTransformation() != null)
                .map(d -> TypeRename.from(d.field().getTypeTransformation().getOriginalName(), d.field().getName()))
                .collect(toList());
    }

    private List<TypeMapper> createMappers(
            List<TransformationWithParentType<FieldDefinitionWithTransformation>> defs) {
        Map<String, TypeMapper> typeMapperMap = new LinkedHashMap<>();
        for (TransformationWithParentType<FieldDefinitionWithTransformation> definition : defs) {
            final FieldTransformation transformation = definition.field().getFieldTransformation();
            if (transformation.getFieldMappingDefinition() != null) {
                typeMapperMap.compute(definition.parentType(), (k, v) ->
                        ((v == null) ? TypeMappers.typeNamed(definition.parentType()) : v)
                                .copy(definition.field().getName(),
                                        transformation.getFieldMappingDefinition().getInputName()));
            }
        }
        return typeMapperMap.values().stream().map(TypeMapper::copyRemaining).collect(toList());
    }


    private Link createHydrationLink(SchemaNamespace schemaNamespace,
                                     TransformationWithParentType<FieldDefinitionWithTransformation> definition,
                                     InnerServiceHydration hydration) {
        SchemaNamespace targetService = assertNotNull(this.namespaceByService.get(hydration.getServiceName()));
        final FieldDefinition targetField = findTargetFieldForHydration(hydration, targetService);

        List<LinkArgument> linkArguments = hydration.getArguments().stream()
                .map(argDef -> LinkArgument.newLinkArgument()
                        .sourceName(argDef.getRemoteArgumentSource().getName())
                        .argumentSource(LinkArgument.ArgumentSource.valueOf(argDef.getRemoteArgumentSource().getSourceType().toString()))
                        .queryArgumentName(argDef.getName())
                        .removeInputField(false)
                        .nullable(true)
                        .build())
                .collect(Collectors.toList());
        String sourceType = definition.parentType();
        String targetType = typeInfo(definition.field().getType()).getName();
        return Link.newComplexLink()
                .sourceType(sourceType)
                .newFieldName(definition.field().getName())
                .sourceNamespace(schemaNamespace)
                .targetNamespace(targetService)
                .targetType(targetType)
                .topLevelQueryField(targetField.getName())
                .noSchemaChangeNeeded(true)
                .linkArguments(linkArguments)
                .argumentValueProvider(argumentValueProvider)
                .build();
    }


    private FieldDefinition findTargetFieldForHydration(InnerServiceHydration hydration, SchemaNamespace targetService) {
        final TypeDefinitionRegistry types = privateSchemaProvider.schemaFor(targetService)
                .orElseGet(() -> typesByService.get(targetService.getValue()));
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
        return new InvalidDslException(String.format("Error in field hydration definition: " + format, args),
                hydration.getSourceLocation());
    }

    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Builder newNadel() {
        return new Builder();
    }

    public static class Builder {
        private String dsl;
        private SchemaSourceFactory schemaSourceFactory;
        private SchemaTransformationsFactory transformationsFactory = SchemaTransformationsFactory.DEFAULT;
        private BatchLoaderEnvironment batchLoaderEnvironment;
        private List<Instrumentation> instrumentations = new ArrayList<>();
        private ArgumentValueProvider argumentValueProvider = DefaultArgumentValueProvider.INSTANCE;
        private PrivateSchemaProvider privateSchemaProvider = PrivateSchemaProvider.DEFAULT;

        public Builder dsl(String dsl) {
            this.dsl = requireNonNull(dsl);
            return this;
        }

        public Builder remoteRetrieverFactory(GraphQLRemoteRetrieverFactory<?> graphQLRemoteRetrieverFactory) {
            this.schemaSourceFactory = new GraphQLRemoteSchemaSourceFactory<>(requireNonNull(graphQLRemoteRetrieverFactory));
            return this;
        }

        public Builder schemaSourceFactory(SchemaSourceFactory schemaSourceFactory) {
            this.schemaSourceFactory = requireNonNull(schemaSourceFactory);
            return this;
        }

        public Builder transformationsFactory(SchemaTransformationsFactory transformationsFactory) {
            this.transformationsFactory = requireNonNull(transformationsFactory);
            return this;

        }

        public Builder batchLoaderEnvironment(BatchLoaderEnvironment batchLoaderEnvironment) {
            this.batchLoaderEnvironment = requireNonNull(batchLoaderEnvironment);
            return this;
        }

        public Builder useInstrumentation(Instrumentation instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder argumentValueProvider(ArgumentValueProvider argumentValueProvider) {
            this.argumentValueProvider = requireNonNull(argumentValueProvider);
            return this;
        }

        public Builder privateSchemaProvider(PrivateSchemaProvider privateSchemaProvider) {
            this.privateSchemaProvider = requireNonNull(privateSchemaProvider);
            return this;
        }

        public Nadel build() {
            return new Nadel(dsl, schemaSourceFactory, transformationsFactory, batchLoaderEnvironment, instrumentations,
                    argumentValueProvider, privateSchemaProvider);
        }
    }
}
