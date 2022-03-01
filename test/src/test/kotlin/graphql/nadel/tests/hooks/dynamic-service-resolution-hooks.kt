package graphql.nadel.tests.hooks

import graphql.ErrorType
import graphql.GraphqlErrorBuilder
import graphql.execution.ExecutionStepInfo
import graphql.execution.ResultPath
import graphql.nadel.Nadel
import graphql.nadel.Service
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.hooks.ServiceOrError
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

class Hooks : ServiceExecutionHooks {

    private fun resolveServiceGeneric(
        services: Collection<Service>,
        idArgument: Any,
        resultPath: ResultPath
    ): ServiceOrError {
        if (idArgument.toString().contains("pull-request")) {
            return ServiceOrError(
                services.stream().filter { service -> (service.name == "RepoService") }.findFirst().get(),
                null
            )
        }

        if (idArgument.toString().contains("issue")) {
            return ServiceOrError(
                services.stream().filter { service -> (service.name == "IssueService") }.findFirst()
                    .get(), null
            )
        }

        return ServiceOrError(
            null,
            GraphqlErrorBuilder.newError()
                .message("Could not resolve service for field: %s", resultPath)
                .errorType(ErrorType.ExecutionAborted)
                .path(resultPath)
                .build()
        )

    }

    override fun resolveServiceForField(
        services: Collection<Service>,
        executableNormalizedField: ExecutableNormalizedField
    ): ServiceOrError {
        return resolveServiceGeneric(
            services,
            executableNormalizedField.getNormalizedArgument("id").value,
            ResultPath.fromList(listOf(executableNormalizedField.resultKey))
        )
    }


    override fun resolveServiceForField(
        services: Collection<Service>,
        executionStepInfo: ExecutionStepInfo,
    ): ServiceOrError {
        val idArgument = executionStepInfo.arguments.get("id")

        return resolveServiceGeneric(services, idArgument!!, executionStepInfo.path)

    }
}

@UseHook
class `dynamic-service-resolution-multiple-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-simple-success-case` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-multiple-services-with-one-unmapped-node-lookup` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-handles-inline-fragments-from-multiple-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-handles-complex-fragments` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-with-no-fragments` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-directive-not-in-interface` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}

@UseHook
class `typename-is-passed-on-queries-using-dynamic-resolved-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(Hooks())
    }
}
