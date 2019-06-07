package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.ParseResult;
import graphql.PublicApi;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.nadel.dsl.CommonDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.engine.Execution;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters;
import graphql.nadel.introspection.DefaultIntrospectionRunner;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.nadel.schema.NeverWiringFactory;
import graphql.nadel.schema.OverallSchemaGenerator;
import graphql.nadel.schema.UnderlyingSchemaGenerator;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static graphql.execution.instrumentation.DocumentAndVariables.newDocumentAndVariables;
import static graphql.nadel.util.Util.buildServiceRegistry;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@PublicApi
public class Nadel {

    private final Logger log = LoggerFactory.getLogger(Nadel.class);
    private final StitchingDsl stitchingDsl;
    private final ServiceExecutionFactory serviceExecutionFactory;
    private final NSDLParser NSDLParser = new NSDLParser();
    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final PreparsedDocumentProvider preparsedDocumentProvider;
    private final ExecutionIdProvider executionIdProvider;
    private final IntrospectionRunner introspectionRunner;
    private final DefinitionRegistry commonTypes;
    private final WiringFactory overallWiringFactory;
    private final WiringFactory underlyingWiringFactory;
    private final OverallSchemaGenerator overallSchemaGenerator = new OverallSchemaGenerator();

    private Nadel(Reader nsdl,
                  ServiceExecutionFactory serviceExecutionFactory,
                  NadelInstrumentation instrumentation,
                  PreparsedDocumentProvider preparsedDocumentProvider,
                  ExecutionIdProvider executionIdProvider,
                  IntrospectionRunner introspectionRunner,
                  ServiceExecutionHooks serviceExecutionHooks,
                  WiringFactory overallWiringFactory,
                  WiringFactory underlyingWiringFactory) {
        this.serviceExecutionFactory = serviceExecutionFactory;
        this.instrumentation = instrumentation;
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.preparsedDocumentProvider = preparsedDocumentProvider;
        this.executionIdProvider = executionIdProvider;

        this.stitchingDsl = this.NSDLParser.parseDSL(nsdl);
        this.introspectionRunner = introspectionRunner;
        this.overallWiringFactory = overallWiringFactory;
        this.underlyingWiringFactory = underlyingWiringFactory;
        this.services = createServices();
        this.commonTypes = createCommonTypes();
        this.overallSchema = createOverallSchema();
    }

    private DefinitionRegistry createCommonTypes() {
        CommonDefinition commonDefinition = stitchingDsl.getCommonDefinition();
        return buildServiceRegistry(commonDefinition);
    }

    private List<Service> createServices() {
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        UnderlyingSchemaGenerator underlyingSchemaGenerator = new UnderlyingSchemaGenerator();

        List<Service> serviceList = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            String serviceName = serviceDefinition.getName();
            ServiceExecution serviceExecution = this.serviceExecutionFactory.getServiceExecution(serviceName);
            TypeDefinitionRegistry underlyingTypeDefinitions = this.serviceExecutionFactory.getUnderlyingTypeDefinitions(serviceName);

            GraphQLSchema underlyingSchema = underlyingSchemaGenerator.buildUnderlyingSchema(underlyingTypeDefinitions, underlyingWiringFactory);
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
        //
        // make sure that the overall schema has the standard scalars in it since he underlying may use them EVEN if the overall does not
        // make direct use of them, we still have to map between them
        return schema.transform(builder -> ScalarInfo.STANDARD_SCALARS.forEach(builder::additionalType));
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
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(nadelExecutionInput.getQuery())
                .operationName(nadelExecutionInput.getOperationName())
                .context(nadelExecutionInput.getContext())
                .variables(nadelExecutionInput.getVariables())
                .executionId(nadelExecutionInput.getExecutionId())
                .build();

        try {
            log.debug("Executing request. operation name: '{}'. query: '{}'. variables '{}'", executionInput.getOperationName(), executionInput.getQuery(), executionInput.getVariables());
            InstrumentationState instrumentationState = instrumentation.createState(new NadelInstrumentationCreateStateParameters(overallSchema, executionInput));

            NadelInstrumentationQueryExecutionParameters inputInstrumentationParameters = new NadelInstrumentationQueryExecutionParameters(executionInput, overallSchema, instrumentationState);
            executionInput = instrumentation.instrumentExecutionInput(executionInput, inputInstrumentationParameters);

            NadelInstrumentationQueryExecutionParameters instrumentationParameters = new NadelInstrumentationQueryExecutionParameters(executionInput, overallSchema, instrumentationState);
            InstrumentationContext<ExecutionResult> executionInstrumentation = instrumentation.beginQueryExecution(instrumentationParameters);

            CompletableFuture<ExecutionResult> executionResult = parseValidateAndExecute(executionInput, overallSchema, instrumentationState, nadelExecutionInput.getArtificialFieldsUUID());
            //
            // finish up instrumentation
            executionResult = executionResult.whenComplete(executionInstrumentation::onCompleted);
            //
            // allow instrumentation to tweak the result
            executionResult = executionResult.thenCompose(result -> instrumentation.instrumentExecutionResult(result, instrumentationParameters));
            return executionResult;
        } catch (AbortExecutionException abortException) {
            return CompletableFuture.completedFuture(abortException.toExecutionResult());
        }
    }

    private CompletableFuture<ExecutionResult> parseValidateAndExecute(ExecutionInput executionInput, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState, String artificialFieldsUUID) {
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
        return executeImpl(executionInputRef.get(), preparsedDoc.getDocument(), instrumentationState, artificialFieldsUUID);
    }

    private PreparsedDocumentEntry parseAndValidate(AtomicReference<ExecutionInput> executionInputRef, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {

        ExecutionInput executionInput = executionInputRef.get();
        String query = executionInput.getQuery();

        log.debug("Parsing query: '{}'...", query);
        ParseResult parseResult = parse(executionInput, graphQLSchema, instrumentationState);
        if (parseResult.isFailure()) {
            log.warn("Query failed to parse : '{}'", executionInput.getQuery());
            return new PreparsedDocumentEntry(parseResult.getException().toInvalidSyntaxError());
        } else {
            final Document document = parseResult.getDocument();
            // they may have changed the document and the variables via instrumentation so update the reference to it
            executionInput = executionInput.transform(builder -> builder.variables(parseResult.getVariables()));
            executionInputRef.set(executionInput);

            log.debug("Validating query: '{}'", query);
            final List<ValidationError> errors = validate(executionInput, document, graphQLSchema, instrumentationState);
            if (!errors.isEmpty()) {
                log.warn("Query failed to validate : '{}' because of {} ", query, errors);
                return new PreparsedDocumentEntry(errors);
            }

            return new PreparsedDocumentEntry(document);
        }
    }

    private ParseResult parse(ExecutionInput executionInput, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        NadelInstrumentationQueryExecutionParameters parameters = new NadelInstrumentationQueryExecutionParameters(executionInput, graphQLSchema, instrumentationState);
        InstrumentationContext<Document> parseInstrumentation = instrumentation.beginParse(parameters);

        Parser parser = new Parser();
        Document document;
        DocumentAndVariables documentAndVariables;
        try {
            document = parser.parseDocument(executionInput.getQuery());
            documentAndVariables = newDocumentAndVariables()
                    .document(document).variables(executionInput.getVariables()).build();
            documentAndVariables = instrumentation.instrumentDocumentAndVariables(documentAndVariables, parameters);
        } catch (InvalidSyntaxException e) {
            parseInstrumentation.onCompleted(null, e);
            return ParseResult.ofError(e);
        }

        parseInstrumentation.onCompleted(documentAndVariables.getDocument(), null);
        return ParseResult.of(documentAndVariables);
    }

    private List<ValidationError> validate(ExecutionInput executionInput, Document document, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        InstrumentationContext<List<ValidationError>> validationCtx = instrumentation.beginValidation(new NadelNadelInstrumentationQueryValidationParameters(executionInput, document, graphQLSchema, instrumentationState));

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);

        validationCtx.onCompleted(validationErrors, null);
        return validationErrors;
    }

    private CompletableFuture<ExecutionResult> executeImpl(ExecutionInput executionInput, Document document, InstrumentationState instrumentationState, String artificialFieldsUUID) {

        String query = executionInput.getQuery();
        String operationName = executionInput.getOperationName();
        Object context = executionInput.getContext();

        ExecutionId executionId = executionInput.getExecutionId();
        if (executionId == null) {
            executionId = executionIdProvider.provide(query, operationName, context);
        }

        Execution execution = new Execution(getServices(), overallSchema, instrumentation, introspectionRunner, serviceExecutionHooks);

        return execution.execute(executionInput, document, executionId, instrumentationState, artificialFieldsUUID);
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Builder newNadel() {
        return new Builder();
    }

    public static class Builder {
        private Reader nsdl;
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


        public Builder dsl(Reader nsdl) {
            this.nsdl = requireNonNull(nsdl);
            return this;
        }

        public Builder dsl(String nsdl) {
            return dsl(new StringReader(requireNonNull(nsdl)));
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

        public Nadel build() {
            return new Nadel(
                    nsdl,
                    serviceExecutionFactory,
                    instrumentation,
                    preparsedDocumentProvider,
                    executionIdProvider,
                    introspectionRunner,
                    serviceExecutionHooks,
                    overallWiringFactory,
                    underlyingWiringFactory);
        }
    }
}
