package graphql.nadel.tests.util.fixtures.hooks

import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.language.SourceLocation
import graphql.nadel.Nadel
import graphql.nadel.hooks.HydrationArguments
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.normalized.NormalizedQueryField
import graphql.nadel.tests.Engine
import graphql.nadel.tests.util.fixtures.EngineTestHook
import graphql.nadel.tests.util.fixtures.KeepHook
import java.util.Optional
import java.util.concurrent.CompletableFuture

@KeepHook
class `top-level-field-error-does-not-redact-other-top-level-fields` : EngineTestHook {
    override fun makeNadel(engine: Engine, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(object : ServiceExecutionHooks {
            override fun isFieldForbidden(
                normalizedField: NormalizedQueryField,
                hydrationArguments: HydrationArguments?,
                variables: Map<String, Any>?,
                userSuppliedContext: Any?,
            ): CompletableFuture<Optional<GraphQLError>> {
                val expectedError = GraphqlErrorBuilder.newError()
                    .message("Hello world")
                    .path(listOf("test", "hello"))
                    .build()

                return if (normalizedField.resultKey == "foo") {
                    CompletableFuture.completedFuture(Optional.of(expectedError))
                } else {
                    CompletableFuture.completedFuture(Optional.empty())
                }
            }
        })
    }
}
