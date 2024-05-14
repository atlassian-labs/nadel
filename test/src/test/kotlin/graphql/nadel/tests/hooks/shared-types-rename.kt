package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

abstract class `shared-types-rename` : EngineTestHook {
    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder.transformExecutionHints {
            it.sharedTypeRenames {
                true
            }
        }
    }
}

@UseHook
class `renamed-type-in-union-declared-in-another-service` : `shared-types-rename`()