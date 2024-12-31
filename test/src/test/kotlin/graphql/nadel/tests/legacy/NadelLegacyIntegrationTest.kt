package graphql.nadel.tests.legacy

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.getTestHook
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.validation.NadelSchemaValidationError
import org.intellij.lang.annotations.Language

abstract class NadelLegacyIntegrationTest(
    operationName: String? = null,
    @Language("GraphQL")
    query: String,
    variables: JsonMap = emptyMap(),
    userContext: Any? = null,
    services: List<Service>,
) : NadelIntegrationTest(
    operationName = operationName,
    query = query,
    variables = variables,
    userContext = userContext,
    services = services,
) {
    private val legacyHook: EngineTestHook = getTestHook(javaClass.simpleName) ?: EngineTestHook.noOp

    override fun makeExecutionInput(): NadelExecutionInput.Builder {
        return legacyHook.makeExecutionInput(super.makeExecutionInput())
    }

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return legacyHook.makeExecutionHints(super.makeExecutionHints())
    }

    override fun makeNadel(): Nadel.Builder {
        return legacyHook.makeNadel(super.makeNadel())
            .transforms(legacyHook.customTransforms)
            .schemaTransformationHook(legacyHook.schemaTransformationHook)
    }

    override fun makeNadelSchemas(): NadelSchemas.Builder {
        return super.makeNadelSchemas()
            .overallWiringFactory(legacyHook.wiringFactory)
            .underlyingWiringFactory(legacyHook.wiringFactory)
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        return legacyHook.wrapServiceExecution(service.name, super.makeServiceExecution(service))
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        legacyHook.assertResult(result)
    }

    override fun assertSchemaErrors(errors: Set<NadelSchemaValidationError>) {
        if (legacyHook.isSchemaValid(errors)) {
            return
        }

        super.assertSchemaErrors(errors)
    }
}
