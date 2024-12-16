package graphql.nadel.tests.hooks

import graphql.ErrorType
import graphql.GraphqlErrorBuilder
import graphql.execution.ResultPath
import graphql.nadel.Nadel
import graphql.nadel.ServiceLike
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

class Hooks : NadelExecutionHooks {
    private fun resolveServiceGeneric(
        services: List<ServiceLike>,
        idArgument: Any,
        resultPath: ResultPath,
    ): NadelDynamicServiceResolutionResult {
        if (idArgument.toString().contains("pull-request")) {
            return NadelDynamicServiceResolutionResult.Success(
                services.first { service -> service.name == "RepoService" },
            )
        }

        if (idArgument.toString().contains("issue")) {
            return NadelDynamicServiceResolutionResult.Success(
                services.first { service -> service.name == "IssueService" },
            )
        }

        return NadelDynamicServiceResolutionResult.Error(
            GraphqlErrorBuilder.newError()
                .message("Could not resolve service for field: %s", resultPath)
                .errorType(ErrorType.ExecutionAborted)
                .path(resultPath)
                .build()
        )
    }

    override fun resolveServiceForField(
        services: List<ServiceLike>,
        executableNormalizedField: ExecutableNormalizedField,
    ): NadelDynamicServiceResolutionResult {
        return resolveServiceGeneric(
            services,
            executableNormalizedField.getNormalizedArgument("id").value,
            ResultPath.fromList(listOf(executableNormalizedField.resultKey))
        )
    }
}

@UseHook
class `dynamic-service-resolution-multiple-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-simple-success-case` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-multiple-services-with-one-unmapped-node-lookup` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-handles-inline-fragments-from-multiple-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-handles-complex-fragments` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-with-no-fragments` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `dynamic-service-resolution-directive-not-in-interface` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}

@UseHook
class `typename-is-passed-on-queries-using-dynamic-resolved-services` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(Hooks())
    }
}
