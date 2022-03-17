package graphql.nadel;

import graphql.Assert;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.ParseAndValidateResult;
import graphql.PublicApi;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.language.SDLDefinition;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters;
import graphql.nadel.schema.NeverWiringFactory;
import graphql.nadel.schema.OverallSchemaGenerator;
import graphql.nadel.schema.QuerySchemaGenerator;
import graphql.nadel.schema.SchemaTransformationHook;
import graphql.nadel.schema.UnderlyingSchemaGenerator;
import graphql.nadel.util.LogKit;
import graphql.nadel.util.SchemaUtil;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static graphql.execution.instrumentation.DocumentAndVariables.newDocumentAndVariables;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@PublicApi
public class Nadel {
    private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(Nadel.class);
    private static final Logger log = LoggerFactory.getLogger(Nadel.class);

    private final NadelExecutionEngine engine;
    final ServiceExecutionFactory serviceExecutionFactory;
    final List<Service> services;
    final GraphQLSchema engineSchema;
    final GraphQLSchema querySchema;
    @NotNull
    final NadelInstrumentation instrumentation;
    final ServiceExecutionHooks serviceExecutionHooks;
    final PreparsedDocumentProvider preparsedDocumentProvider;
    final ExecutionIdProvider executionIdProvider;
    final WiringFactory overallWiringFactory;
    final WiringFactory underlyingWiringFactory;
    final SchemaTransformationHook schemaTransformationHook;

    private Nadel(
        Map<String, Reader> serviceNDSLs,
        ServiceExecutionFactory serviceExecutionFactory,
        @NotNull NadelInstrumentation instrumentation,
        PreparsedDocumentProvider preparsedDocumentProvider,
        ExecutionIdProvider executionIdProvider,
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

        this.overallWiringFactory = overallWiringFactory;
        this.underlyingWiringFactory = underlyingWiringFactory;
        this.services = createServices(serviceNDSLs);
        this.engineSchema = createEngineSchema();
        this.querySchema = QuerySchemaGenerator.generateQuerySchema(this.engineSchema);
        this.engine = null;
    }

    private Nadel(Nadel originalNadel, NadelExecutionEngine engine) {
        this.serviceExecutionFactory = originalNadel.serviceExecutionFactory;
        this.instrumentation = originalNadel.instrumentation;
        this.serviceExecutionHooks = originalNadel.serviceExecutionHooks;
        this.preparsedDocumentProvider = originalNadel.preparsedDocumentProvider;
        this.executionIdProvider = originalNadel.executionIdProvider;
        this.schemaTransformationHook = originalNadel.schemaTransformationHook;

        this.overallWiringFactory = originalNadel.overallWiringFactory;
        this.underlyingWiringFactory = originalNadel.underlyingWiringFactory;
        this.services = originalNadel.services;
        this.engineSchema = originalNadel.engineSchema;
        this.querySchema = originalNadel.querySchema;
        this.engine = engine;
    }

    private List<Service> createServices(Map<String, Reader> serviceNDSLs) {
        List<Service> serviceList = new ArrayList<>();
        UnderlyingSchemaGenerator underlyingSchemaGenerator = new UnderlyingSchemaGenerator();

        for (Map.Entry<String, Reader> e : serviceNDSLs.entrySet()) {
            String serviceName = e.getKey();
            List<SDLDefinition> serviceSchema = SchemaUtil.parseDefinitions(e.getValue());

            ServiceExecution serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName);
            TypeDefinitionRegistry underlyingTypeDefinitions = serviceExecutionFactory.getUnderlyingTypeDefinitions(serviceName);
            GraphQLSchema underlyingSchema = underlyingSchemaGenerator.buildUnderlyingSchema(serviceName, underlyingTypeDefinitions, underlyingWiringFactory);

            NadelDefinitionRegistry nadelDefinitionRegistry = NadelDefinitionRegistry.from(serviceSchema);

            Service service = new Service(serviceName, underlyingSchema, serviceExecution, nadelDefinitionRegistry);
            serviceList.add(service);
        }
        return serviceList;
    }

    private GraphQLSchema createEngineSchema() {
        OverallSchemaGenerator overallSchemaGenerator = new OverallSchemaGenerator();

        List<NadelDefinitionRegistry> serviceRegistries = this.services.stream()
            .map(Service::getDefinitionRegistry)
            .collect(toList());

        GraphQLSchema schema = overallSchemaGenerator.buildOverallSchema(serviceRegistries, overallWiringFactory);

        GraphQLSchema newSchema = schemaTransformationHook.apply(schema, this.services);

        //
        // make sure that the overall schema has the standard scalars in it since the underlying may use them EVEN if the overall does not
        // make direct use of them, we still have to map between them
        return newSchema.transform(builder -> ScalarInfo.GRAPHQL_SPECIFICATION_SCALARS.forEach(builder::additionalType));
    }

    public List<Service> getServices() {
        return services;
    }

    public GraphQLSchema getEngineSchema() {
        return engineSchema;
    }

    public GraphQLSchema getQuerySchema() {
        return querySchema;
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput.Builder nadelExecutionInput) {
        return execute(nadelExecutionInput.build());
    }

    public CompletableFuture<ExecutionResult> execute(UnaryOperator<NadelExecutionInput.Builder> builderFunction) {
        return execute(builderFunction.apply(NadelExecutionInput.newNadelExecutionInput()).build());
    }

    @NotNull
    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query(nadelExecutionInput.getQuery())
            .operationName(nadelExecutionInput.getOperationName())
            .context(nadelExecutionInput.getContext())
            .variables(nadelExecutionInput.getVariables())
            .executionId(nadelExecutionInput.getExecutionId())
            .build();

        NadelExecutionParams nadelExecutionParams = new NadelExecutionParams(nadelExecutionInput.getNadelExecutionHints());

        InstrumentationState instrumentationState = instrumentation.createState(new NadelInstrumentationCreateStateParameters(querySchema, executionInput));
        NadelInstrumentationQueryExecutionParameters instrumentationParameters = new NadelInstrumentationQueryExecutionParameters(executionInput, querySchema, instrumentationState);
        try {
            logNotSafe.debug("Executing request. operation name: '{}'. query: '{}'. variables '{}'", executionInput.getOperationName(), executionInput.getQuery(), executionInput.getVariables());

            InstrumentationContext<ExecutionResult> executionInstrumentation = instrumentation.beginQueryExecution(instrumentationParameters);

            return parseValidateAndExecute(executionInput, querySchema, instrumentationState, nadelExecutionParams)
                //
                // finish up instrumentation
                .whenComplete(executionInstrumentation::onCompleted)
                .exceptionally(throwable -> {
                    if (throwable instanceof AbortExecutionException) {
                        AbortExecutionException abortException = (AbortExecutionException) throwable;
                        return abortException.toExecutionResult();
                    } else if (throwable instanceof CompletionException && throwable.getCause() instanceof AbortExecutionException) {
                        AbortExecutionException abortException = (AbortExecutionException) throwable.getCause();
                        return abortException.toExecutionResult();
                    } else if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw new RuntimeException(throwable);
                })
                //
                // allow instrumentation to tweak the result
                .thenCompose(result -> instrumentation.instrumentExecutionResult(result, instrumentationParameters));
        } catch (AbortExecutionException abortException) {
            return instrumentation.instrumentExecutionResult(abortException.toExecutionResult(), instrumentationParameters);
        }
    }

    public void close() {
        engine.close();
    }

    private CompletableFuture<ExecutionResult> parseValidateAndExecute(ExecutionInput executionInput,
                                                                       GraphQLSchema graphQLSchema,
                                                                       InstrumentationState instrumentationState,
                                                                       NadelExecutionParams nadelExecutionParams) {
        AtomicReference<ExecutionInput> executionInputRef = new AtomicReference<>(executionInput);
        Function<ExecutionInput, PreparsedDocumentEntry> computeFunction = transformedInput -> {
            // if they change the original query in the pre-parser, then we want to see it downstream from then on
            executionInputRef.set(transformedInput);
            return parseAndValidate(executionInputRef, graphQLSchema, instrumentationState);
        };
        PreparsedDocumentEntry preparsedDoc = preparsedDocumentProvider.getDocument(executionInput, computeFunction);
        if (preparsedDoc.hasErrors()) {
            return CompletableFuture.completedFuture(new ExecutionResultImpl(preparsedDoc.getErrors()));
        }
        return engine.execute(executionInputRef.get(), preparsedDoc.getDocument(), instrumentationState, nadelExecutionParams);
    }

    private PreparsedDocumentEntry parseAndValidate(AtomicReference<ExecutionInput> executionInputRef, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        ExecutionInput executionInput = executionInputRef.get();
        String query = executionInput.getQuery();

        logNotSafe.debug("Parsing query: '{}'...", query);
        ParseAndValidateResult parseResult = parse(executionInput, graphQLSchema, instrumentationState);
        if (parseResult.isFailure()) {
            logNotSafe.warn("Query failed to parse : '{}'", executionInput.getQuery());
            return new PreparsedDocumentEntry(parseResult.getSyntaxException().toInvalidSyntaxError());
        } else {
            final Document document = parseResult.getDocument();
            // they may have changed the document and the variables via instrumentation so update the reference to it
            executionInput = executionInput.transform(builder -> builder.variables(parseResult.getVariables()));
            executionInputRef.set(executionInput);

            logNotSafe.debug("Validating query: '{}'", query);
            final List<ValidationError> errors = validate(executionInput, document, graphQLSchema, instrumentationState);
            if (!errors.isEmpty()) {
                logNotSafe.warn("Query failed to validate : '{}' because of {} ", query, errors);
                return new PreparsedDocumentEntry(errors);
            }

            return new PreparsedDocumentEntry(document);
        }
    }

    private ParseAndValidateResult parse(ExecutionInput executionInput, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        NadelInstrumentationQueryExecutionParameters parameters = new NadelInstrumentationQueryExecutionParameters(executionInput, graphQLSchema, instrumentationState);
        InstrumentationContext<Document> parseInstrumentation = instrumentation.beginParse(parameters);

        Document document;
        DocumentAndVariables documentAndVariables;
        try {
            document = new Parser().parseDocument(executionInput.getQuery());
            documentAndVariables = newDocumentAndVariables()
                .document(document)
                .variables(executionInput.getVariables())
                .build();
        } catch (InvalidSyntaxException e) {
            parseInstrumentation.onCompleted(null, e);
            return ParseAndValidateResult.newResult().syntaxException(e).build();
        }

        parseInstrumentation.onCompleted(documentAndVariables.getDocument(), null);
        return ParseAndValidateResult.newResult().document(documentAndVariables.getDocument()).variables(documentAndVariables.getVariables()).build();
    }

    private List<ValidationError> validate(ExecutionInput executionInput, Document document, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        InstrumentationContext<List<ValidationError>> validationCtx = instrumentation.beginValidation(new NadelInstrumentationQueryValidationParameters(executionInput, document, graphQLSchema, instrumentationState));

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);

        validationCtx.onCompleted(validationErrors, null);
        return validationErrors;
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Nadel.Builder newNadel() {
        return new Nadel.Builder();
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
        private WiringFactory overallWiringFactory = new NeverWiringFactory();
        private WiringFactory underlyingWiringFactory = new NeverWiringFactory();
        private SchemaTransformationHook schemaTransformationHook = SchemaTransformationHook.IDENTITY;
        private NadelExecutionEngineFactory engineFactory;

        private NadelExecutionEngine buildDefaultEngine(Nadel nadel) {
            try {
                Class<?> klass = Class.forName("graphql.nadel.NextgenEngine");
                Constructor<?> declaredConstructor = klass.getDeclaredConstructor(Nadel.class);
                declaredConstructor.setAccessible(true);
                try {
                    return (NadelExecutionEngine) declaredConstructor.newInstance(nadel);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Unable to create Nadel engine", e);
                }
            } catch (ClassNotFoundException ignored) {
                throw new RuntimeException("Unable to create Nadel engine from known class graphql.nadel.NadelEngine");
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
            Nadel nadelStep1 = new Nadel(
                serviceNDSLs,
                serviceExecutionFactory,
                instrumentation,
                preparsedDocumentProvider,
                executionIdProvider,
                serviceExecutionHooks,
                overallWiringFactory,
                underlyingWiringFactory,
                schemaTransformationHook);

            NadelExecutionEngine executionEngine;
            if (engineFactory == null) {
                executionEngine = buildDefaultEngine(nadelStep1);
            } else {
                executionEngine = engineFactory.create(nadelStep1);
            }

            Assert.assertNotNull(executionEngine, () -> "The engine factory must return an engine instance");
            return new Nadel(nadelStep1, executionEngine);
        }
    }
}
