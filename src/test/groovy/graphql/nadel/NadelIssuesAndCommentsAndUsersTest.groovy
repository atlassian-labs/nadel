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

        NadelInstrumentation instrumentation = new NadelInstrumentation() {
            def t = System.nanoTime()

            @Override
            InstrumentationContext<ExecutionResultNode> beginFieldFetch(NadelInstrumentationFetchFieldParameters parameters) {
                def path = parameters.getExecutionStepInfo().getPath()
                return new SimpleInstrumentationContext<ExecutionResultNode>() {

                    private long secs() {
                        def plus = System.nanoTime() - t
                        TimeUnit.SECONDS.convert(plus, TimeUnit.NANOSECONDS)
                    }

                    @Override
                    void onDispatched(CompletableFuture<ExecutionResultNode> result) {
                        long t = this.secs()
                        println "Dispatched   <= $path @ T+$t"
                    }

                    @Override
                    void onCompleted(ExecutionResultNode result, Throwable throwable) {
                        long t = secs()
                        println "   Completed => $path @ T+$t"
                    }
                }
            }
        }

        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2  )

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
    }
}
