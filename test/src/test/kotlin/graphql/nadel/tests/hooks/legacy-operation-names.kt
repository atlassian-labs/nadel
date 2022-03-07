package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

abstract class `legacy-operation-names` : EngineTestHook {
    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder.transformExecutionHints {
            it.legacyOperationNames {
                true
            }
        }
    }
}

@UseHook
class `can-generate-legacy-operation-names` : `legacy-operation-names`() {
}

@UseHook
class `can-generate-legacy-operation-names-forwarding-original-name` : `legacy-operation-names`() {
}

@UseHook
class `can-generate-legacy-operation-name-on-hydration` : `legacy-operation-names`() {
}

@UseHook
class `can-generate-legacy-operation-name-on-batch-hydration` : `legacy-operation-names`() {
}

@UseHook
class `can-generate-legacy-operation-name-on-batch-hydration-for-specific-service` : EngineTestHook {
    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder.transformExecutionHints {
            it.legacyOperationNames { service ->
                service.name == "service2"
            }
        }
    }
}
