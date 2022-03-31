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
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.schema.QuerySchemaGenerator
import graphql.nadel.schema.SchemaTransformationHook
import graphql.parser.InvalidSyntaxException
import graphql.parser.Parser
import graphql.schema.GraphQLSchema
import graphql.schema.idl.WiringFactory
import graphql.validation.ValidationError
import graphql.validation.Validator
import java.io.Reader
import java.io.StringReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

class Nadel private constructor(
    private val engine: NadelExecutionEngine,
    val services: List<Service>,
    val engineSchema: GraphQLSchema,
    val querySchema: GraphQLSchema,
    internal val serviceExecutionFactory: ServiceExecutionFactory,
    internal val instrumentation: NadelInstrumentation,
    internal val serviceExecutionHooks: ServiceExecutionHooks,
    internal val preparsedDocumentProvider: PreparsedDocumentProvider,
    internal val executionIdProvider: ExecutionIdProvider,
) {
    fun execute(nadelExecutionInput: NadelExecutionInput): CompletableFuture<ExecutionResult> {
        val executionInput: ExecutionInput = newExecutionInput()
            .query(nadelExecutionInput.query)
            .operationName(nadelExecutionInput.operationName)
            .context(nadelExecutionInput.context)
            .variables(nadelExecutionInput.variables)
            .executionId(nadelExecutionInput.executionId)
            .build()

        val nadelExecutionParams = NadelExecutionParams(nadelExecutionInput.nadelExecutionHints)
        val instrumentationState = instrumentation.createState(
            NadelInstrumentationCreateStateParameters(querySchema, executionInput),
        )
        val instrumentationParameters = NadelInstrumentationQueryExecutionParameters(
            executionInput, querySchema, instrumentationState
        )

        return try {
            val executionInstrumentation = instrumentation.beginQueryExecution(instrumentationParameters)

            parseValidateAndExecute(executionInput, querySchema, instrumentationState, nadelExecutionParams)
                // finish up instrumentation
                .whenComplete { result: ExecutionResult?, t: Throwable? ->
                    executionInstrumentation.onCompleted(result, t)
                }
                .exceptionally { throwable: Throwable ->
                    if (throwable is AbortExecutionException) {
                        throwable.toExecutionResult()
                    } else if (throwable is CompletionException && throwable.cause is AbortExecutionException) {
                        val abortException = throwable.cause as AbortExecutionException
                        abortException.toExecutionResult()
                    } else if (throwable is RuntimeException) {
                        throw throwable
                    } else {
                        throw RuntimeException(throwable)
                    }
                } //
                // allow instrumentation to tweak the result
                .thenCompose { result: ExecutionResult ->
                    instrumentation.instrumentExecutionResult(result, instrumentationParameters)
                }
        } catch (abortException: AbortExecutionException) {
            instrumentation.instrumentExecutionResult(abortException.toExecutionResult(), instrumentationParameters)
        }
    }

    fun close() {
        engine.close()
    }

    private fun parseValidateAndExecute(
        executionInput: ExecutionInput,
        graphQLSchema: GraphQLSchema,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams,
    ): CompletableFuture<ExecutionResult> {
        val executionInputRef = AtomicReference(executionInput)

        val computeFunction = Function { transformedInput: ExecutionInput ->
            // if they change the original query in the pre-parser, then we want to see it downstream from then on
            executionInputRef.set(transformedInput)
            parseAndValidate(executionInputRef, graphQLSchema, instrumentationState)
        }

        return preparsedDocumentProvider.getDocumentAsync(executionInput, computeFunction)
            .thenCompose { result ->
                if (result.hasErrors()) {
                    CompletableFuture.completedFuture(
                        ExecutionResultImpl(result.errors),
                    )
                } else engine.execute(
                    executionInputRef.get()!!,
                    result.document,
                    instrumentationState,
                    nadelExecutionParams,
                )
            }
    }

    private fun parseAndValidate(
        executionInputRef: AtomicReference<ExecutionInput>,
        graphQLSchema: GraphQLSchema,
        instrumentationState: InstrumentationState?,
    ): PreparsedDocumentEntry {
        var executionInput = executionInputRef.get()!!

        val query = executionInput.query
        val parseResult = parse(executionInput, graphQLSchema, instrumentationState)

        return if (parseResult.isFailure) {
            PreparsedDocumentEntry(parseResult.syntaxException.toInvalidSyntaxError())
        } else {
            val document = parseResult.document

            // they may have changed the document and the variables via instrumentation so update the reference to it
            executionInput = executionInput.transform { builder: ExecutionInput.Builder ->
                builder.variables(parseResult.variables)
            }
            executionInputRef.set(executionInput)

            val errors = validate(executionInput, document, graphQLSchema, instrumentationState)

            if (errors.isNotEmpty()) {
                PreparsedDocumentEntry(errors)
            } else {
                PreparsedDocumentEntry(document)
            }
        }
    }

    private fun parse(
        executionInput: ExecutionInput,
        graphQLSchema: GraphQLSchema,
        instrumentationState: InstrumentationState?,
    ): ParseAndValidateResult {
        val parameters = NadelInstrumentationQueryExecutionParameters(
            executionInput,
            graphQLSchema,
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
        graphQLSchema: GraphQLSchema,
        instrumentationState: InstrumentationState?,
    ): MutableList<ValidationError> {
        val validationCtx = instrumentation.beginValidation(
            NadelInstrumentationQueryValidationParameters(
                executionInput = executionInput,
                document = document,
                schema = graphQLSchema,
                instrumentationState = instrumentationState,
                context = executionInput.context,
            ),
        )
        val validator = Validator()
        val validationErrors = validator.validateDocument(graphQLSchema, document)
        validationCtx.onCompleted(validationErrors, null)
        return validationErrors
    }

    class Builder {
        private var serviceExecutionFactory: ServiceExecutionFactory? = null
        private var instrumentation: NadelInstrumentation = object : NadelInstrumentation {}
        private var serviceExecutionHooks: ServiceExecutionHooks = object : ServiceExecutionHooks {}
        private var preparsedDocumentProvider: PreparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE
        private var executionIdProvider = ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER
        private var engineFactory: NadelExecutionEngineFactory = NadelExecutionEngineFactory(::NextgenEngine)

        private var schemaBuilder = NadelSchemas.Builder()

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
            this.serviceExecutionFactory = serviceExecutionFactory
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

        fun serviceExecutionHooks(serviceExecutionHooks: ServiceExecutionHooks): Builder {
            this.serviceExecutionHooks = serviceExecutionHooks
            return this
        }

        fun engineFactory(engineFactory: NadelExecutionEngineFactory): Builder {
            this.engineFactory = engineFactory
            return this
        }

        fun build(): Nadel {
            val serviceExecutionFactory = requireNotNull(serviceExecutionFactory) {
                "Must set serviceExecutionFactory on the builder"
            }

            val (engineSchema, services) = schemaBuilder.build()

            val querySchema = QuerySchemaGenerator.generateQuerySchema(engineSchema)

            lateinit var nadel: Nadel
            return Nadel(
                engine = object : NadelExecutionEngine {
                    val real by lazy {
                        // Dumb hack because the engine factory takes in a Nadel object, but we haven't created one yet
                        // This used to work because we created two Nadel instances but now we only create one
                        // Todo: we can remove this at some point with some refactoring
                        engineFactory.create(nadel)
                    }

                    override fun execute(
                        executionInput: ExecutionInput,
                        queryDocument: Document,
                        instrumentationState: InstrumentationState?,
                        nadelExecutionParams: NadelExecutionParams,
                    ): CompletableFuture<ExecutionResult> {
                        return real.execute(executionInput, queryDocument, instrumentationState, nadelExecutionParams)
                    }
                },
                serviceExecutionFactory = serviceExecutionFactory,
                services = services,
                engineSchema = engineSchema,
                querySchema = querySchema,
                instrumentation = instrumentation,
                serviceExecutionHooks = serviceExecutionHooks,
                preparsedDocumentProvider = preparsedDocumentProvider,
                executionIdProvider = executionIdProvider,
            ).also {
                nadel = it
            }
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
        /**
         * @return a builder of Nadel objects
         */
        @JvmStatic
        fun newNadel(): Builder {
            return Builder()
        }
    }
}
