package graphql.nadel.tests.hooks

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.execution.AbortExecutionException
import graphql.nadel.Nadel
import graphql.nadel.hooks.HydrationArguments
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.normalized.NormalizedQueryField
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import java.util.Optional
import java.util.concurrent.CompletableFuture
import graphql.GraphqlErrorException as GraphQLErrorException

private class RejectField(private val fieldNames: List<String>) : ServiceExecutionHooks {
    constructor(vararg fieldNames: String) : this(fieldNames.toList())

    override fun isFieldForbidden(
        normalizedField: NormalizedQueryField,
        hydrationArguments: HydrationArguments,
        variables: MutableMap<String, Any>,
        userSuppliedContext: Any,
    ): CompletableFuture<Optional<GraphQLError>> {
        return CompletableFuture.completedFuture(
            Optional.ofNullable(
                if (normalizedField.name in fieldNames) {
                    makeError(normalizedField.path)
                } else {
                    null
                }
            )
        )
    }

    companion object {
        fun makeError(path: List<String>): GraphQLError {
            return GraphQLErrorException
                .newErrorException()
                .message("removed field")
                .path(path as List<Any>)
                .errorClassification(object : ErrorClassification {
                    override fun toString(): String {
                        return "ExecutionAborted"
                    }
                })
                .build()
        }
    }
}

@KeepHook
class `hydrated-field-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("author"))
    }
}

@KeepHook
class `nested-hydrated-field-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("author"))
    }
}

@KeepHook
class `field-is-removed-from-nested-hydrated-field` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId"))
    }
}

@KeepHook
class `all-fields-in-a-selection-set-are-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title", "description"))
    }
}

@KeepHook
class `field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title"))
    }
}

@KeepHook
class `one-of-top-level-fields-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("commentById"))
    }
}

@KeepHook
class `top-level-field-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("commentById"))
    }
}

@KeepHook
class `top-level-field-in-batched-query-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("comments"))
    }
}

@KeepHook
class `all-fields-are-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId", "displayName"))
    }
}

@KeepHook
class `field-is-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId"))
    }
}

@KeepHook
class `all-non-hydrated-fields-in-query-are-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("id", "created", "commentText"))
    }
}

@KeepHook
class `field-with-selections-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("epic"))
    }
}

@KeepHook
class `the-only-field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title"))
    }
}

@KeepHook
class `field-in-non-hydrated-query-is-removed` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("created"))
    }
}

@KeepHook
class `restricted-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

@KeepHook
class `restricted-field-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

@KeepHook
class `inserts-one-error-for-a-forbidden-field-in-a-list` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

@KeepHook
class `restricted-single-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(object : ServiceExecutionHooks {
                override fun isFieldForbidden(
                    normalizedField: NormalizedQueryField,
                    hydrationArguments: HydrationArguments,
                    variables: MutableMap<String, Any>,
                    userSuppliedContext: Any,
                ): CompletableFuture<Optional<GraphQLError>> {
                    return CompletableFuture.completedFuture(
                        Optional.ofNullable(
                            if (normalizedField.name == "restricted" && normalizedField.parent.parent.name == "issue") {
                                RejectField.makeError(path = normalizedField.path)
                            } else {
                                null
                            }
                        ),
                    )
                }
            })
    }
}

@KeepHook
class `restricted-single-field-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(object : ServiceExecutionHooks {
                override fun isFieldForbidden(
                    normalizedField: NormalizedQueryField,
                    hydrationArguments: HydrationArguments,
                    variables: MutableMap<String, Any>,
                    userSuppliedContext: Any,
                ): CompletableFuture<Optional<GraphQLError>> {
                    return CompletableFuture.completedFuture(
                        Optional.ofNullable(
                            if (normalizedField.name == "restricted" && normalizedField.parent.name == "issue") {
                                RejectField.makeError(path = normalizedField.path)
                            } else {
                                null
                            }
                        ),
                    )
                }
            })
    }
}
