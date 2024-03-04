package graphql.nadel.tests.hooks

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransform
import graphql.GraphqlErrorException as GraphQLErrorException

private class RejectField(private val fieldNames: List<String>) : NadelExecutionHooks {
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
            .executionHooks(RejectField("author"))
    }
}

// @UseHook
class `nested-hydrated-field-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("author"))
    }
}

// @UseHook
class `field-is-removed-from-nested-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("userId"))
    }
}

// @UseHook
class `all-fields-in-a-selection-set-are-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("title", "description"))
    }
}

// @UseHook
class `field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("title"))
    }
}

// @UseHook
class `one-of-top-level-fields-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("commentById"))
    }
}

@UseHook
class `top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

@UseHook
class `top-level-field-is-removed-hint-is-off` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { false }
}

@UseHook
class `hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

@UseHook
class `namespaced-hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

@UseHook
class `hidden-namespaced-hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

@UseHook
class `namespaced-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

@UseHook
class `namespaced-field-is-removed-with-renames` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder) = builder.shortCircuitEmptyQuery { true }
}

// @UseHook
class `top-level-field-in-batched-query-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("comments"))
    }
}

// @UseHook
class `all-fields-are-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("userId", "displayName"))
    }
}

// @UseHook
class `field-is-removed-from-hydrated-field` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("userId"))
    }
}

// @UseHook
class `all-non-hydrated-fields-in-query-are-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("id", "created", "commentText"))
    }
}

// @UseHook
class `field-with-selections-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("epic"))
    }
}

// @UseHook
class `the-only-field-in-a-selection-set-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("title"))
    }
}

// @UseHook
class `field-in-non-hydrated-query-is-removed` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("created"))
    }
}

// @UseHook
class `restricted-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `restricted-field-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `inserts-one-error-for-a-forbidden-field-in-a-list` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(RejectField("restricted"))
    }
}

// @UseHook
class `restricted-single-field-inside-hydration-via-fragments-used-twice` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionHooks(object : NadelExecutionHooks {
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
            .executionHooks(object : NadelExecutionHooks {
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
