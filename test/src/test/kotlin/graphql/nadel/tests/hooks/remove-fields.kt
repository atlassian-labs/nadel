package graphql.nadel.tests.hooks

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.nadel.Nadel
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.GraphqlErrorException as GraphQLErrorException

private class RejectField(private val fieldNames: List<String>) : ServiceExecutionHooks {
    constructor(vararg fieldNames: String) : this(fieldNames.toList())

    // override fun isFieldForbidden(
    //     normalizedField: NormalizedQueryField,
    //     hydrationArguments: HydrationArguments,
    //     variables: MutableMap<String, Any>,
    //     graphQLSchema: GraphQLSchema,
    //     userSuppliedContext: Any,
    // ): CompletableFuture<Optional<GraphQLError>> {
    //     return CompletableFuture.completedFuture(
    //         Optional.ofNullable(
    //             if (normalizedField.name in fieldNames) {
    //                 makeError(normalizedField.path)
    //             } else {
    //                 null
    //             }
    //         )
    //     )
    // }

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

// @UseHook
class `hydrated-field-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("author"))
    }
}

// @UseHook
class `nested-hydrated-field-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("author"))
    }
}

// @UseHook
class `field-is-removed-from-nested-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId"))
    }
}

// @UseHook
class `all-fields-in-a-selection-set-are-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title", "description"))
    }
}

// @UseHook
class `field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title"))
    }
}

// @UseHook
class `one-of-top-level-fields-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("commentById"))
    }
}

// @UseHook
class `top-level-field-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("commentById"))
    }
}

// @UseHook
class `top-level-field-in-batched-query-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("comments"))
    }
}

// @UseHook
class `all-fields-are-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId", "displayName"))
    }
}

// @UseHook
class `field-is-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("userId"))
    }
}

// @UseHook
class `all-non-hydrated-fields-in-query-are-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("id", "created", "commentText"))
    }
}

// @UseHook
class `field-with-selections-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("epic"))
    }
}

// @UseHook
class `the-only-field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("title"))
    }
}

// @UseHook
class `field-in-non-hydrated-query-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("created"))
    }
}

// @UseHook
class `restricted-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `restricted-field-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `inserts-one-error-for-a-forbidden-field-in-a-list` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `restricted-single-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(object : ServiceExecutionHooks {
                // override fun isFieldForbidden(
                //     normalizedField: NormalizedQueryField,
                //     hydrationArguments: HydrationArguments,
                //     variables: MutableMap<String, Any>,
                //     graphQLSchema: GraphQLSchema,
                //     userSuppliedContext: Any,
                // ): CompletableFuture<Optional<GraphQLError>> {
                //     return CompletableFuture.completedFuture(
                //         Optional.ofNullable(
                //             if (normalizedField.name == "restricted" && normalizedField.parent.parent.name == "issue") {
                //                 RejectField.makeError(path = normalizedField.path)
                //             } else {
                //                 null
                //             }
                //         ),
                //     )
                // }
            })
    }
}

// @UseHook
class `restricted-single-field-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(object : ServiceExecutionHooks {
                // override fun isFieldForbidden(
                //     normalizedField: NormalizedQueryField,
                //     hydrationArguments: HydrationArguments,
                //     variables: MutableMap<String, Any>,
                //     graphQLSchema: GraphQLSchema,
                //     userSuppliedContext: Any,
                // ): CompletableFuture<Optional<GraphQLError>> {
                //     return CompletableFuture.completedFuture(
                //         Optional.ofNullable(
                //             if (normalizedField.name == "restricted" && normalizedField.parent.name == "issue") {
                //                 RejectField.makeError(path = normalizedField.path)
                //             } else {
                //                 null
                //             }
                //         ),
                //     )
                // }
            })
    }
}
