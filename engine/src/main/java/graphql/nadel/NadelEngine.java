package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.ParseAndValidateResult;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.nadel.engine.Execution;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.nadel.util.LogKit;
import graphql.parser.InvalidSyntaxException;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static graphql.execution.instrumentation.DocumentAndVariables.newDocumentAndVariables;

public class NadelEngine implements NadelExecutionEngine {
    /**
     * @return a builder of Nadel objects
     */
    public static Nadel.Builder newNadel() {
        return new Nadel.Builder().engineFactory(NadelEngine::new);
    }

    private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(Nadel.class);
    private static final Logger log = LoggerFactory.getLogger(Nadel.class);

    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final PreparsedDocumentProvider preparsedDocumentProvider;
    private final ExecutionIdProvider executionIdProvider;
    private final IntrospectionRunner introspectionRunner;

    public NadelEngine(Nadel nadel) {
        this.services = nadel.services;
        this.overallSchema = nadel.overallSchema;
        this.instrumentation = nadel.instrumentation;
        this.serviceExecutionHooks = nadel.serviceExecutionHooks;
        this.preparsedDocumentProvider = nadel.preparsedDocumentProvider;
        this.executionIdProvider = nadel.executionIdProvider;
        this.introspectionRunner = nadel.introspectionRunner;
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        long startTime = System.currentTimeMillis();
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(nadelExecutionInput.getQuery())
                .operationName(nadelExecutionInput.getOperationName())
                .context(nadelExecutionInput.getContext())
                .variables(nadelExecutionInput.getVariables())
                .executionId(nadelExecutionInput.getExecutionId())
                .build();

        NadelExecutionParams nadelExecutionParams = new NadelExecutionParams(nadelExecutionInput.getArtificialFieldsUUID(), nadelExecutionInput.getNadelExecutionHints());

        InstrumentationState instrumentationState = instrumentation.createState(new NadelInstrumentationCreateStateParameters(overallSchema, executionInput));
        NadelInstrumentationQueryExecutionParameters instrumentationParameters = new NadelInstrumentationQueryExecutionParameters(executionInput, overallSchema, instrumentationState);
        try {
            logNotSafe.debug("Executing request. operation name: '{}'. query: '{}'. variables '{}'", executionInput.getOperationName(), executionInput.getQuery(), executionInput.getVariables());

            NadelInstrumentationQueryExecutionParameters inputInstrumentationParameters = new NadelInstrumentationQueryExecutionParameters(executionInput, overallSchema, instrumentationState);
            executionInput = instrumentation.instrumentExecutionInput(executionInput, inputInstrumentationParameters);

            InstrumentationContext<ExecutionResult> executionInstrumentation = instrumentation.beginQueryExecution(instrumentationParameters);

            CompletableFuture<ExecutionResult> executionResult = parseValidateAndExecute(executionInput, overallSchema, instrumentationState, nadelExecutionParams);
            //
            // finish up instrumentation
            executionResult = executionResult.whenComplete(executionInstrumentation::onCompleted);
            //
            // allow instrumentation to tweak the result
            executionResult = executionResult.thenCompose(result -> instrumentation.instrumentExecutionResult(result, instrumentationParameters));
            return executionResult.whenComplete((executionResult1, throwable) -> {
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.debug("Finished execution in {} ms, executionId: {}", elapsedTime, nadelExecutionInput.getExecutionId());
            });
        } catch (AbortExecutionException abortException) {
            return instrumentation.instrumentExecutionResult(abortException.toExecutionResult(), instrumentationParameters);
        }
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
        return executeImpl(executionInputRef.get(), preparsedDoc.getDocument(), instrumentationState, nadelExecutionParams);
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
            document = new NadelGraphQLParser().parseDocument(executionInput.getQuery());
            documentAndVariables = newDocumentAndVariables()
                    .document(document).variables(executionInput.getVariables()).build();
            documentAndVariables = instrumentation.instrumentDocumentAndVariables(documentAndVariables, parameters);
        } catch (InvalidSyntaxException e) {
            parseInstrumentation.onCompleted(null, e);
            return ParseAndValidateResult.newResult().syntaxException(e).build();
        }

        parseInstrumentation.onCompleted(documentAndVariables.getDocument(), null);
        return ParseAndValidateResult.newResult().document(documentAndVariables.getDocument()).variables(documentAndVariables.getVariables()).build();
    }

    private List<ValidationError> validate(ExecutionInput executionInput, Document document, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
        InstrumentationContext<List<ValidationError>> validationCtx = instrumentation.beginValidation(new NadelNadelInstrumentationQueryValidationParameters(executionInput, document, graphQLSchema, instrumentationState));

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);

        validationCtx.onCompleted(validationErrors, null);
        return validationErrors;
    }

    private CompletableFuture<ExecutionResult> executeImpl(ExecutionInput executionInput,
                                                           Document document,
                                                           InstrumentationState instrumentationState,
                                                           NadelExecutionParams nadelExecutionParams) {

        String query = executionInput.getQuery();
        String operationName = executionInput.getOperationName();
        Object context = executionInput.getContext();

        ExecutionId executionId = executionInput.getExecutionId();
        if (executionId == null) {
            executionId = executionIdProvider.provide(query, operationName, context);
        }

        if (executionInput.getContext() instanceof BenchmarkContext) {
            BenchmarkContext.ExecutionArgs executionArgs = ((BenchmarkContext) executionInput.getContext()).executionArgs;
            executionArgs.services = services;
            executionArgs.overallSchema = overallSchema;
            executionArgs.instrumentation = instrumentation;
            executionArgs.introspectionRunner = introspectionRunner;
            executionArgs.serviceExecutionHooks = serviceExecutionHooks;
            executionArgs.context = executionInput.getContext();
            executionArgs.executionInput = executionInput;
            executionArgs.document = document;
            executionArgs.executionId = executionId;
            executionArgs.instrumentationState = instrumentationState;
            executionArgs.nadelExecutionParams = nadelExecutionParams;
        }

        Execution execution = new Execution(services, overallSchema, instrumentation, introspectionRunner, serviceExecutionHooks, executionInput.getContext());

        return execution.execute(executionInput, document, executionId, instrumentationState, nadelExecutionParams);
    }
}
