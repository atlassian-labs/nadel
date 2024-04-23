package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionInput.newExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.ParseAndValidateResult
import graphql.execution.AbortExecutionException
import graphql.execution.ExecutionIdProvider
import graphql.execution.instrumentation.DocumentAndVariables
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.language.Document
import graphql.nadel.engine.blueprint.NadelDefaultIntrospectionRunner
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.schema.QuerySchemaGenerator
import graphql.nadel.schema.SchemaTransformationHook
import graphql.nadel.util.LogKit
import graphql.parser.InvalidSyntaxException
import graphql.parser.Parser
import graphql.schema.GraphQLSchema
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory
import graphql.validation.ValidationError
import graphql.validation.Validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.io.Reader
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicReference

class Nadel private constructor(
    private val engine: NextgenEngine,
    val services: List<Service>,
    val engineSchema: GraphQLSchema,
    val querySchema: GraphQLSchema,
    private val instrumentation: NadelInstrumentation,
    private val preparsedDocumentProvider: PreparsedDocumentProvider,
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Deprecated(message = "Use executeAsync", replaceWith = ReplaceWith("this.executeAsync(executionInput)"))
    @JvmName("execute") // For binary compat
    fun executeJava(executionInput: NadelExecutionInput): CompletableFuture<ExecutionResult> {
        return coroutineScope.future {
            execute(executionInput)
        }
    }

    fun executeAsync(executionInput: NadelExecutionInput): CompletableFuture<ExecutionResult> {
        return coroutineScope.future {
            execute(executionInput)
        }
    }

    @JvmName("executeSuspending")
    suspend fun execute(nadelExecutionInput: NadelExecutionInput): ExecutionResult {
        val executionInput: ExecutionInput = newExecutionInput()
            .query(nadelExecutionInput.query)
            .operationName(nadelExecutionInput.operationName)
            .context(nadelExecutionInput.context)
            .graphQLContext { builder ->
                // we need to transfer over all the keys - ExecutionInput does not allow for setting it direct
                nadelExecutionInput.graphqlContext.stream().forEach {
                    builder.put(it.key, it.value)
                }
            }
            .variables(nadelExecutionInput.variables)
            .extensions(nadelExecutionInput.extensions)
            .executionId(nadelExecutionInput.executionId)
            .build()

        val hints = nadelExecutionInput.nadelExecutionHints
        val instrumentationState = instrumentation.createState(
            NadelInstrumentationCreateStateParameters(querySchema, executionInput),
        )
        val instrumentationParameters = NadelInstrumentationQueryExecutionParameters(
            executionInput, querySchema, instrumentationState
        )

        return try {
            val executionInstrumentation = instrumentation.beginQueryExecution(instrumentationParameters)

            val result = try {
                parseValidateAndExecute(executionInput, instrumentationState, hints)
                    .also {
                        executionInstrumentation.onCompleted(it, null)
                    }
            } catch (e: Exception) {
                executionInstrumentation.onCompleted(null, e)

                val cause = e.cause
                if (e is AbortExecutionException) {
                    e.toExecutionResult()
                } else if (e is CompletionException && cause is AbortExecutionException) {
                    cause.toExecutionResult()
                } else {
                    throw e
                }
            }

            instrumentation
                .instrumentExecutionResult(result, instrumentationParameters)
                .await()
        } catch (e: AbortExecutionException) {
            instrumentation
                .instrumentExecutionResult(e.toExecutionResult(), instrumentationParameters)
                .await()
        }
    }

    fun close() {
        // Closes the scope after letting in flight requests go through
        coroutineScope.launch {
            delay(60_000) // Wait a minute
            coroutineScope.cancel()
        }
    }

    private suspend fun parseValidateAndExecute(
        executionInput: ExecutionInput,
        instrumentationState: InstrumentationState?,
        hints: NadelExecutionHints,
    ): ExecutionResult {
        // todo: I'm pretty sure this is never changed, but let's circle back to that in another PR to reduce changelog here
        val executionInputRef = AtomicReference(executionInput)

        val result = preparsedDocumentProvider
            .getDocumentAsync(executionInput) { transformedInput ->
                // If they change the original query in the pre-parser, then we want to see it downstream from then on
                executionInputRef.set(transformedInput)
                parseAndValidate(executionInputRef, instrumentationState)
            }
            .await()

        return if (result.hasErrors()) {
            ExecutionResultImpl(result.errors)
        } else {
            coroutineScope {
                engine.execute(
                    coroutineScope = this,
                    executionInput = executionInputRef.get()!!,
                    queryDocument = result.document,
                    instrumentationState = instrumentationState,
                    executionHints = hints,
                )
            }
        }
    }

    private fun parseAndValidate(
        executionInputRef: AtomicReference<ExecutionInput>,
        instrumentationState: InstrumentationState?,
    ): PreparsedDocumentEntry {
        var executionInput = executionInputRef.get()!!

        val query = executionInput.query
        logNotSafe.debug("Parsing query: '{}'...", query)
        val parseResult = parse(executionInput, instrumentationState)

        return if (parseResult.isFailure) {
            logNotSafe.warn("Query failed to parse : '{}'", executionInput.query)
            PreparsedDocumentEntry(parseResult.syntaxException.toInvalidSyntaxError())
        } else {
            val document = parseResult.document

            // they may have changed the document and the variables via instrumentation so update the reference to it
            executionInput = executionInput.transform { builder: ExecutionInput.Builder ->
                builder.variables(parseResult.variables)
            }
            executionInputRef.set(executionInput)

            logNotSafe.debug("Validating query: '{}'", query)
            val errors = validate(executionInput, document, instrumentationState)

            if (errors.isNotEmpty()) {
                logNotSafe.warn("Query failed to validate : '{}' because of {} ", query, errors)
                PreparsedDocumentEntry(errors)
            } else {
                PreparsedDocumentEntry(document)
            }
        }
    }

    private fun parse(
        executionInput: ExecutionInput,
        instrumentationState: InstrumentationState?,
    ): ParseAndValidateResult {
        val parameters = NadelInstrumentationQueryExecutionParameters(
            executionInput,
            querySchema,
            instrumentationState
        )

        val parseInstrumentation = instrumentation.beginParse(parameters)
        val document: Document
        val documentAndVariables: DocumentAndVariables

        try {
            document = Parser().parseDocument(executionInput.query)
            documentAndVariables = DocumentAndVariables.newDocumentAndVariables()
                .document(document)
                .variables(executionInput.variables)
                .build()
        } catch (e: InvalidSyntaxException) {
            parseInstrumentation.onCompleted(null, e)
            return ParseAndValidateResult.newResult().syntaxException(e).build()
        }

        parseInstrumentation.onCompleted(documentAndVariables.document, null)

        return ParseAndValidateResult
            .newResult()
            .document(documentAndVariables.document)
            .variables(documentAndVariables.variables)
            .build()
    }

    private fun validate(
        executionInput: ExecutionInput,
        document: Document,
        instrumentationState: InstrumentationState?,
    ): MutableList<ValidationError> {
        val validationCtx = instrumentation.beginValidation(
            NadelInstrumentationQueryValidationParameters(
                executionInput = executionInput,
                document = document,
                schema = querySchema,
                instrumentationState = instrumentationState,
                context = executionInput.context,
            ),
        )
        val validator = Validator()
        val validationErrors = validator.validateDocument(querySchema, document, Locale.getDefault())
        validationCtx.onCompleted(validationErrors, null)
        return validationErrors
    }

    class Builder {
        private var instrumentation: NadelInstrumentation = object : NadelInstrumentation {}
        private var executionHooks: NadelExecutionHooks = object : NadelExecutionHooks {}
        private var preparsedDocumentProvider: PreparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE
        private var executionIdProvider = ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER
        private var transforms = emptyList<NadelTransform<out Any>>()
        private var introspectionRunnerFactory = NadelIntrospectionRunnerFactory(::NadelDefaultIntrospectionRunner)

        private var schemas: NadelSchemas? = null
        private var schemaBuilder = NadelSchemas.Builder()

        private var maxQueryDepth = Integer.MAX_VALUE
        private var maxFieldCount = Integer.MAX_VALUE

        fun schemas(schemas: NadelSchemas): Builder {
            this.schemas = schemas
            return this
        }

        fun overallSchema(serviceName: String, nsdl: Reader): Builder {
            schemaBuilder.overallSchema(serviceName, nsdl)
            return this
        }

        fun overallSchema(serviceName: String, nsdl: String): Builder {
            schemaBuilder.overallSchema(serviceName, nsdl)
            return this
        }

        @JvmName("overallSchemasReader")
        fun overallSchemas(overallSchemas: Map<String, Reader>): Builder {
            schemaBuilder.overallSchemas(overallSchemas)
            return this
        }

        @JvmName("overallSchemasString")
        fun overallSchemas(overallSchemas: Map<String, String>): Builder {
            schemaBuilder.overallSchemas(overallSchemas)
            return this
        }

        fun underlyingSchema(serviceName: String, nsdl: Reader): Builder {
            schemaBuilder.underlyingSchema(serviceName, nsdl)
            return this
        }

        fun underlyingSchema(serviceName: String, nsdl: String): Builder {
            schemaBuilder.underlyingSchema(serviceName, nsdl)
            return this
        }

        fun underlyingSchema(serviceName: String, schema: TypeDefinitionRegistry): Builder {
            schemaBuilder.underlyingSchema(serviceName, schema)
            return this
        }

        @JvmName("underlyingSchemasReader")
        fun underlyingSchemas(underlyingSchemas: Map<String, Reader>): Builder {
            schemaBuilder.underlyingSchemas(underlyingSchemas)
            return this
        }

        @JvmName("underlyingSchemasString")
        fun underlyingSchemas(underlyingSchemas: Map<String, String>): Builder {
            schemaBuilder.underlyingSchemas(underlyingSchemas)
            return this
        }

        @JvmName("underlyingTypeDefs")
        fun underlyingSchemas(value: Map<String, TypeDefinitionRegistry>): Builder = also {
            schemaBuilder.underlyingSchemas(value)
            return this
        }

        fun overallWiringFactory(wiringFactory: WiringFactory): Builder {
            schemaBuilder.overallWiringFactory(wiringFactory)
            return this
        }

        fun underlyingWiringFactory(wiringFactory: WiringFactory): Builder {
            schemaBuilder.underlyingWiringFactory(wiringFactory)
            return this
        }

        fun schemaTransformationHook(hook: SchemaTransformationHook): Builder {
            schemaBuilder.schemaTransformationHook(hook)
            return this
        }

        fun serviceExecutionFactory(serviceExecutionFactory: ServiceExecutionFactory): Builder {
            schemaBuilder.serviceExecutionFactory(serviceExecutionFactory)
            return this
        }

        fun instrumentation(instrumentation: NadelInstrumentation): Builder {
            this.instrumentation = instrumentation
            return this
        }

        fun preparsedDocumentProvider(preparsedDocumentProvider: PreparsedDocumentProvider): Builder {
            this.preparsedDocumentProvider = preparsedDocumentProvider
            return this
        }

        fun executionIdProvider(executionIdProvider: ExecutionIdProvider): Builder {
            this.executionIdProvider = executionIdProvider
            return this
        }

        fun executionHooks(executionHooks: NadelExecutionHooks): Builder {
            this.executionHooks = executionHooks
            return this
        }

        fun transforms(transforms: List<NadelTransform<out Any>>): Builder {
            this.transforms = transforms
            return this
        }

        fun introspectionRunnerFactory(introspectionRunnerFactory: NadelIntrospectionRunnerFactory): Builder {
            this.introspectionRunnerFactory = introspectionRunnerFactory
            return this
        }

        fun maxQueryDepth(maxQueryDepth: Int): Builder {
            this.maxQueryDepth = maxQueryDepth
            return this
        }

        fun maxFieldCount(maxFieldCount: Int): Builder {
            this.maxFieldCount = maxFieldCount
            return this
        }

        fun build(): Nadel {
            val (engineSchema, services) = schemas ?: schemaBuilder.build()

            val querySchema = QuerySchemaGenerator.generateQuerySchema(engineSchema)

            return Nadel(
                engine = NextgenEngine(
                    engineSchema = engineSchema,
                    querySchema = querySchema,
                    instrumentation = instrumentation,
                    executionHooks = executionHooks,
                    executionIdProvider = executionIdProvider,
                    services = services,
                    transforms = transforms,
                    introspectionRunnerFactory = introspectionRunnerFactory,
                    maxQueryDepth = maxQueryDepth,
                    maxFieldCount = maxFieldCount,
                ),
                services = services,
                engineSchema = engineSchema,
                querySchema = querySchema,
                instrumentation = instrumentation,
                preparsedDocumentProvider = preparsedDocumentProvider,
            )
        }

        @Deprecated("Use overallSchema instead", replaceWith = ReplaceWith("overallSchema(serviceName, nsdl)"))
        fun dsl(serviceName: String, nsdl: Reader): Builder {
            overallSchema(serviceName, nsdl)
            return this
        }

        @Deprecated("Use overallSchema instead", replaceWith = ReplaceWith("overallSchema(serviceName, nsdl)"))
        fun dsl(serviceName: String, nsdl: String): Builder {
            return dsl(serviceName, StringReader(nsdl))
        }

        @Deprecated("Use overallSchemas instead", replaceWith = ReplaceWith("overallSchemas(serviceDSLs)"))
        fun dsl(serviceDSLs: Map<String, String>): Builder {
            return serviceDSLs(serviceDSLs.mapValues { (k, v) -> StringReader(v) })
        }

        @Deprecated("Use overallSchemas instead", replaceWith = ReplaceWith("overallSchemas(serviceDSLs)"))
        fun serviceDSLs(serviceDSLs: Map<String, Reader>): Builder {
            schemaBuilder.overallSchemas(serviceDSLs)
            return this
        }
    }

    companion object {
        private val logNotSafe: Logger = LogKit.getNotPrivacySafeLogger<Nadel>()
        private val log: Logger = LogKit.getLogger<Nadel>()

        /**
         * @return a builder of Nadel objects
         */
        @JvmStatic
        fun newNadel(): Builder {
            return Builder()
        }
    }
}
