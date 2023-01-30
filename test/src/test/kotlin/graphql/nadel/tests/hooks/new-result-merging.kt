package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private interface TurnFlagOn : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .transformExecutionHints {
                it
                    .internalNamespaceTypenameResolution { true }
                    .newResultMerger { true }
            }
    }
}

@UseHook
class `calls-to-multiple-services-are-merged` : TurnFlagOn

@UseHook
class `correct-selection-set-on-failed-result` : TurnFlagOn

@UseHook
class `correct-selection-set-on-partially-failed-result` : TurnFlagOn

@UseHook
class `not-nullable-top-level-field-has-null` : TurnFlagOn

@UseHook
class `not-nullable-top-level-field-has-null-2` : TurnFlagOn

@UseHook
class `not-nullable-top-level-field-is-absent` : TurnFlagOn

@UseHook
class `not-nullable-top-level-field-is-absent-2` : TurnFlagOn

@UseHook
class `not-nullable-namespaced-child-has-null` : TurnFlagOn

@UseHook
class `not-nullable-namespaced-child-is-absent` : TurnFlagOn

@UseHook
class `not-nullable-namespaced-child-is-absent-2` : TurnFlagOn
