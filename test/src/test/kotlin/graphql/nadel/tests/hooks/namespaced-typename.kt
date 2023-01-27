package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private interface TurnTheFlagOn : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .transformExecutionHints {
                it.internalNamespaceTypenameResolution {
                    true
                }
            }
    }
}

@UseHook
class `typename-is-resolved-even-when-namespaced-type-is-extended-in-declaring-service` : TurnTheFlagOn

@UseHook
class `typename-is-resolved-even-when-no-fields-are-queried-from-declaring-service` : TurnTheFlagOn

@UseHook
class `typename-is-resolved-even-when-there-are-multiple-services-declaring-namespaced-type` : TurnTheFlagOn

@UseHook
class `typename-is-resolved-on-namespaced-fields` : TurnTheFlagOn

@UseHook
class `typename-is-resolved-when-namespaced-field-and-type-are-defined-in-different-services` : TurnTheFlagOn

@UseHook
class `typename-is-wiped-when-other-data-fails` : TurnTheFlagOn

@UseHook
class `typename-is-kept-when-nothing-else-is-asked-for` : TurnTheFlagOn

@UseHook
class `typename-is-kept-when-data-is-partially-returned` : TurnTheFlagOn

@UseHook
class `multiple-typenames-are-wiped-when-other-data-fails` : TurnTheFlagOn

@UseHook
class `multiple-typename-is-kept-when-nothing-else-is-asked-for` : TurnTheFlagOn

@UseHook
class `aliased-typename-is-wiped-when-other-data-fails` : TurnTheFlagOn

@UseHook
class `typename-is-wiped-when-other-data-fails-includes-not-nullable-field` : TurnTheFlagOn
