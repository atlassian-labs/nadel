package graphql.nadel


import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters
import graphql.nadel.testutils.harnesses.IssuesCommentsUsersHarness
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput

/**
 * At this stage this is mostly a test of the harness we can use to build out more complex end to end use cases
 * for Nadel testing
 */
class NadelIssuesAndCommentsAndUsersTest extends Specification {

    class CapturingInstrumentation implements NadelInstrumentation {
        def t = System.nanoTime()
        def dispatched = [:]
        def completed = [:]

        @Override
        InstrumentationContext<ExecutionResultNode> beginFieldFetch(NadelInstrumentationFetchFieldParameters parameters) {
            def info = parameters.getExecutionStepInfo()
            def path = info.getPath()
            return new SimpleInstrumentationContext<ExecutionResultNode>() {

                private long ms() {
                    def plus = System.nanoTime() - t
                    TimeUnit.MILLISECONDS.convert(plus, TimeUnit.NANOSECONDS)
                }

                @Override
                void onDispatched(CompletableFuture<ExecutionResultNode> result) {
                    dispatched.put(path.toString(), path)
                    long t = this.ms()
                    println "Dispatched   <= $path @ T+$t"
                }

                @Override
                void onCompleted(ExecutionResultNode result, Throwable throwable) {
                    completed.put(path.toString(), path)
                    long t = ms()
                    println "   Completed => $path @ T+$t"
                }
            }
        }

    }

    def "basic execution with hydration"() {
        given:
        def query = '''
        {
            issues {
                key
                summary
                key
                summary
                reporter {
                    displayName
                }
                comments {
                    commentText
                    author {
                        displayName
                    }
                }    
            }
        }
        '''

        def instrumentation = new CapturingInstrumentation()

        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .instrumentation(instrumentation)
                .serviceExecutionFactory(serviceExecutionFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.errors.isEmpty()
        result.data == [issues: [
                [key     : "WORK-I1", summary: "Summary for WORK-I1", reporter: [displayName: "Display name of fred"],
                 comments: [[commentText: "Text of C1", author: [displayName: "Display name of fred"]],
                            [commentText: "Text of C3", author: [displayName: "Display name of zed"]],
                            [commentText: "Text of C5", author: [displayName: "Display name of fred"]]]],
                [key     : "WORK-I2", summary: "Summary for WORK-I2", reporter: [displayName: "Display name of zed"],
                 comments: [[commentText: "Text of C2", author: [displayName: "Display name of ned"]],
                            [commentText: "Text of C4", author: [displayName: "Display name of jed"]],
                            [commentText: "Text of C6", author: [displayName: "Display name of ted"]]]]],
        ]

        def dispatched = instrumentation.dispatched.keySet().sort()
        dispatched == [
                "/issues",
                "/issues/comments",
                "/issues/comments/author",
                "/issues/comments/author/displayName",
                "/issues/comments/commentText",
                "/issues/key",
                "/issues/reporter",
                "/issues/reporter/displayName",
                "/issues/summary",
        ]
        def completed = instrumentation.completed.keySet().sort()
        completed == [
                "/issues",
                "/issues/comments",
                "/issues/comments/author",
                "/issues/comments/author/displayName",
                "/issues/comments/commentText",
                "/issues/key",
                "/issues/reporter",
                "/issues/reporter/displayName",
                "/issues/summary",
        ]

    }
}
