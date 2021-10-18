package graphql.nadel.tests.hooks

import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.language.SourceLocation
import graphql.nadel.Nadel
import graphql.nadel.hooks.HydrationArguments
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.normalized.NormalizedQueryField
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.NadelEngineType
import graphql.schema.GraphQLSchema
import java.util.Optional
import java.util.concurrent.CompletableFuture

@UseHook
class `top-level-field-error-is-inserted-with-all-information` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(object : ServiceExecutionHooks {
            override fun isFieldForbidden(
                normalizedField: NormalizedQueryField,
                hydrationArguments: HydrationArguments?,
                variables: Map<String, Any>?,
                graphQLSchema: GraphQLSchema,
                userSuppliedContext: Any?,
            ): CompletableFuture<Optional<GraphQLError>> {
                return CompletableFuture.completedFuture(
                    Optional.of(
                        GraphqlErrorBuilder.newError()
                            .message("Hello world")
                            .path(listOf("test", "hello"))
                            .extensions(mapOf("test" to "Hello there"))
                            .location(SourceLocation(12, 34))
                            .errorType(ErrorType.DataFetchingException)
                            .build(),
                    ),
                )
            }
        })
    }
}
