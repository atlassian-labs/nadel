package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `typename-is-resolved-even-when-namespaced-type-is-extended-in-declaring-service` : EngineTestHook {
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
class `typename-is-resolved-even-when-no-fields-are-queried-from-declaring-service` : EngineTestHook {
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
class `typename-is-resolved-even-when-there-are-multiple-services-declaring-namespaced-type` : EngineTestHook {
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
class `typename-is-resolved-on-namespaced-fields` : EngineTestHook {
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
class `typename-is-resolved-when-namespaced-field-and-type-are-defined-in-different-services` : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .transformExecutionHints {
                it.internalNamespaceTypenameResolution {
                    true
                }
            }
    }
}
