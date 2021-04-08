package graphql.nadel;

import graphql.ExecutionResult;
import graphql.PublicApi;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.nadel.dsl.CommonDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.introspection.DefaultIntrospectionRunner;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.nadel.schema.NeverWiringFactory;
import graphql.nadel.schema.OverallSchemaGenerator;
import graphql.nadel.schema.SchemaTransformationHook;
import graphql.nadel.schema.UnderlyingSchemaGenerator;
import graphql.nadel.util.Util;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import static graphql.nadel.Nadel.Builder.defaultEngineFactory;
import static graphql.nadel.util.Util.buildServiceRegistry;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@PublicApi
public class Nadel {
    private final NadelExecutionEngine engine;
    final Map<String, StitchingDsl> stitchingDsls;
    final ServiceExecutionFactory serviceExecutionFactory;
    final List<Service> services;
    final GraphQLSchema overallSchema;
    final NadelInstrumentation instrumentation;
    final ServiceExecutionHooks serviceExecutionHooks;
    final PreparsedDocumentProvider preparsedDocumentProvider;
    final ExecutionIdProvider executionIdProvider;
    final IntrospectionRunner introspectionRunner;
    final DefinitionRegistry commonTypes;
    final WiringFactory overallWiringFactory;
    final WiringFactory underlyingWiringFactory;
    final SchemaTransformationHook schemaTransformationHook;
    final OverallSchemaGenerator overallSchemaGenerator = new OverallSchemaGenerator();

    private Nadel(
            NadelExecutionEngineFactory engineFactory,
            Map<String, Reader> serviceNDSLs,
            ServiceExecutionFactory serviceExecutionFactory,
            NadelInstrumentation instrumentation,
            PreparsedDocumentProvider preparsedDocumentProvider,
            ExecutionIdProvider executionIdProvider,
            IntrospectionRunner introspectionRunner,
            ServiceExecutionHooks serviceExecutionHooks,
            WiringFactory overallWiringFactory,
            WiringFactory underlyingWiringFactory,
            SchemaTransformationHook schemaTransformationHook
    ) {
        this.serviceExecutionFactory = serviceExecutionFactory;
        this.instrumentation = instrumentation;
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.preparsedDocumentProvider = preparsedDocumentProvider;
        this.executionIdProvider = executionIdProvider;
        this.schemaTransformationHook = schemaTransformationHook;

        this.introspectionRunner = introspectionRunner;
        this.overallWiringFactory = overallWiringFactory;
        this.underlyingWiringFactory = underlyingWiringFactory;
        this.stitchingDsls = createDSLs(serviceNDSLs);
        this.services = createServices();
        this.commonTypes = createCommonTypes();
        this.overallSchema = createOverallSchema();

        this.engine = engineFactory.create(this);
    }

    private Map<String, StitchingDsl> createDSLs(Map<String, Reader> serviceNDSLs) {
        NSDLParser nadelParser = new NSDLParser();
        Map<String, StitchingDsl> mapOfDSLs = new LinkedHashMap<>();
        for (Map.Entry<String, Reader> e : serviceNDSLs.entrySet()) {
            StitchingDsl stitchingDsl = nadelParser.parseDSL(e.getValue());
            mapOfDSLs.put(e.getKey(), stitchingDsl);
        }
        return mapOfDSLs;
    }

    private DefinitionRegistry createCommonTypes() {
        List<CommonDefinition> commonDefinitions = stitchingDsls.values().stream()
                .filter(dsl -> dsl.getCommonDefinition() != null)
                .map(StitchingDsl::getCommonDefinition)
                .collect(toList());
        return buildServiceRegistry(commonDefinitions);
    }

    private List<Service> createServices() {
        List<Service> serviceList = new ArrayList<>();
        UnderlyingSchemaGenerator underlyingSchemaGenerator = new UnderlyingSchemaGenerator();

        for (Map.Entry<String, StitchingDsl> e : stitchingDsls.entrySet()) {
            String serviceName = e.getKey();
            StitchingDsl stitchingDsl = e.getValue();
            if (stitchingDsl.getCommonDefinition() != null) {
                continue;
            }
            ServiceDefinition serviceDefinition = Util.buildServiceDefinition(serviceName, stitchingDsl);
            ServiceExecution serviceExecution = this.serviceExecutionFactory.getServiceExecution(serviceName);
            TypeDefinitionRegistry underlyingTypeDefinitions = this.serviceExecutionFactory.getUnderlyingTypeDefinitions(serviceName);

            GraphQLSchema underlyingSchema = underlyingSchemaGenerator
                    .buildUnderlyingSchema(serviceName, underlyingTypeDefinitions, underlyingWiringFactory);
            DefinitionRegistry definitionRegistry = buildServiceRegistry(serviceDefinition);

            Service service = new Service(serviceName, underlyingSchema, serviceExecution, serviceDefinition, definitionRegistry);
            serviceList.add(service);
        }
        return serviceList;
    }

    private GraphQLSchema createOverallSchema() {
        List<DefinitionRegistry> registries = this.services.stream()
                .map(Service::getDefinitionRegistry)
                .collect(toList());
        GraphQLSchema schema = overallSchemaGenerator.buildOverallSchema(registries, commonTypes, overallWiringFactory);

        GraphQLSchema newSchema = schemaTransformationHook.apply(schema);

        //
        // make sure that the overall schema has the standard scalars in it since he underlying may use them EVEN if the overall does not
        // make direct use of them, we still have to map between them
        return newSchema.transform(builder -> ScalarInfo.GRAPHQL_SPECIFICATION_SCALARS.forEach(builder::additionalType));
    }

    public List<Service> getServices() {
        return services;
    }

    public GraphQLSchema getOverallSchema() {
        return overallSchema;
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput.Builder nadelExecutionInput) {
        return execute(nadelExecutionInput.build());
    }

    public CompletableFuture<ExecutionResult> execute(UnaryOperator<NadelExecutionInput.Builder> builderFunction) {
        return execute(builderFunction.apply(NadelExecutionInput.newNadelExecutionInput()).build());
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        return engine.execute(nadelExecutionInput);
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Nadel.Builder newNadel() {
        return new Nadel.Builder().engineFactory(defaultEngineFactory);
    }

    public static class Builder {
        private final Map<String, Reader> serviceNDSLs = new LinkedHashMap<>();
        private ServiceExecutionFactory serviceExecutionFactory;
        private NadelInstrumentation instrumentation = new NadelInstrumentation() {
        };
        private ServiceExecutionHooks serviceExecutionHooks = new ServiceExecutionHooks() {
        };
        private PreparsedDocumentProvider preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
        private ExecutionIdProvider executionIdProvider = ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER;
        private IntrospectionRunner introspectionRunner = new DefaultIntrospectionRunner();
        private WiringFactory overallWiringFactory = new NeverWiringFactory();
        private WiringFactory underlyingWiringFactory = new NeverWiringFactory();
        private SchemaTransformationHook schemaTransformationHook = SchemaTransformationHook.IDENTITY;

        static NadelExecutionEngineFactory defaultEngineFactory;
        private NadelExecutionEngineFactory engineFactory;

        static {
            try {
                Class<?> klass = Class.forName("graphql.nadel.NadelEngine");
                Constructor<?> declaredConstructor = klass.getDeclaredConstructor(Nadel.class);
                declaredConstructor.setAccessible(true);
                defaultEngineFactory = (nadel) -> {
                    try {
                        return (NadelExecutionEngine) declaredConstructor.newInstance(nadel);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Unable to create Nadel engine", e);
                    }
                };
            } catch (ClassNotFoundException ignored) {
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unable to create default Nadel engine factory", e);
            }
        }

        public Builder dsl(String serviceName, Reader nsdl) {
            requireNonNull(nsdl);
            this.serviceNDSLs.put(serviceName, nsdl);
            return this;
        }

        public Builder dsl(String serviceName, String nsdl) {
            return dsl(serviceName, new StringReader(requireNonNull(nsdl)));
        }

        public Builder dsl(Map<String, String> serviceDSLs) {
            requireNonNull(serviceDSLs);

            Map<String, Reader> readerServiceDSLs = new LinkedHashMap<>();
            serviceDSLs.forEach((k, v) -> readerServiceDSLs.put(k, new StringReader(v)));

            return serviceDSLs(readerServiceDSLs);
        }

        public Builder serviceDSLs(Map<String, Reader> serviceDSLs) {
            requireNonNull(serviceDSLs);
            this.serviceNDSLs.clear();
            this.serviceNDSLs.putAll(serviceDSLs);
            return this;
        }

        public Builder serviceExecutionFactory(ServiceExecutionFactory serviceExecutionFactory) {
            this.serviceExecutionFactory = serviceExecutionFactory;
            return this;
        }

        public Builder instrumentation(NadelInstrumentation instrumentation) {
            this.instrumentation = requireNonNull(instrumentation);
            return this;
        }

        public Builder preparsedDocumentProvider(PreparsedDocumentProvider preparsedDocumentProvider) {
            this.preparsedDocumentProvider = requireNonNull(preparsedDocumentProvider);
            return this;
        }

        public Builder executionIdProvider(ExecutionIdProvider executionIdProvider) {
            this.executionIdProvider = requireNonNull(executionIdProvider);
            return this;
        }

        public Builder introspectionRunner(IntrospectionRunner introspectionRunner) {
            this.introspectionRunner = requireNonNull(introspectionRunner);
            return this;
        }

        public Builder serviceExecutionHooks(ServiceExecutionHooks serviceExecutionHooks) {
            this.serviceExecutionHooks = requireNonNull(serviceExecutionHooks);
            return this;
        }

        public Builder overallWiringFactory(WiringFactory wiringFactory) {
            this.overallWiringFactory = requireNonNull(wiringFactory);
            return this;
        }

        public Builder underlyingWiringFactory(WiringFactory wiringFactory) {
            this.underlyingWiringFactory = requireNonNull(wiringFactory);
            return this;
        }

        public Builder schemaTransformationHook(SchemaTransformationHook hook) {
            this.schemaTransformationHook = hook;
            return this;
        }

        public Builder engineFactory(NadelExecutionEngineFactory engineFactory) {
            this.engineFactory = engineFactory;
            return this;
        }

        public Nadel build() {
            Objects.requireNonNull(engineFactory, "Nadel execution engine must be supplied");

            return new Nadel(
                    engineFactory,
                    serviceNDSLs,
                    serviceExecutionFactory,
                    instrumentation,
                    preparsedDocumentProvider,
                    executionIdProvider,
                    introspectionRunner,
                    serviceExecutionHooks,
                    overallWiringFactory,
                    underlyingWiringFactory,
                    schemaTransformationHook);
        }
    }
}
