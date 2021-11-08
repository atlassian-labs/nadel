package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

abstract class `legacy-operation-names` : EngineTestHook {
    override fun makeExecutionInput(
        engineType: NadelEngineType,
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
        engineType: NadelEngineType,
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder.transformExecutionHints {
            it.legacyOperationNames { service ->
                service.name == "service2"
            }
        }
    }
}
