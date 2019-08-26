package graphql.nadel


import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.nadel.instrumentation.ChainedNadelInstrumentation
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.TracingInstrumentation
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

    def instrumentation = new CapturingInstrumentation()
    def chainedInstrumentation = new ChainedNadelInstrumentation([instrumentation, new TracingInstrumentation()])


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


        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .instrumentation(chainedInstrumentation)
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

        def expectedList = [
                "/issues",
                "/issues[0]/comments",
                "/issues[0]/comments[0]/author",
                "/issues[0]/comments[0]/author/displayName",
                "/issues[0]/comments[0]/commentText",
                "/issues[0]/comments[1]/author",
                "/issues[0]/comments[1]/author/displayName",
                "/issues[0]/comments[1]/commentText",
                "/issues[0]/comments[2]/author",
                "/issues[0]/comments[2]/author/displayName",
                "/issues[0]/comments[2]/commentText",
                "/issues[0]/key",
                "/issues[0]/reporter",
                "/issues[0]/reporter/displayName",
                "/issues[0]/summary",
                "/issues[1]/comments",
                "/issues[1]/comments[0]/author",
                "/issues[1]/comments[0]/author/displayName",
                "/issues[1]/comments[0]/commentText",
                "/issues[1]/comments[1]/author",
                "/issues[1]/comments[1]/author/displayName",
                "/issues[1]/comments[1]/commentText",
                "/issues[1]/comments[2]/author",
                "/issues[1]/comments[2]/author/displayName",
                "/issues[1]/comments[2]/commentText",
                "/issues[1]/key",
                "/issues[1]/reporter",
                "/issues[1]/reporter/displayName",
                "/issues[1]/summary",
        ]
//        def dispatched = instrumentation.dispatched.keySet().sort()
//        dispatched == expectedList
//
//        def completed = instrumentation.completed.keySet().sort()
//        completed == expectedList
    }

    def "BUG FIX - underscore typename and tracing can worked as expected"() {

        given:
        def query = '''
        {
            issues {
                key
                __typename
                reporter {
                    displayName
                    __typename
                }
            }
        }
        '''


        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .instrumentation(chainedInstrumentation)
                .serviceExecutionFactory(serviceExecutionFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.errors.isEmpty()

        result.data == [issues: [
                [key: "WORK-I1", __typename: "Issue", reporter: [displayName: "Display name of fred", __typename: "User"]],
                [key: "WORK-I2", __typename: "Issue", reporter: [displayName: "Display name of zed", __typename: "User"]],
        ]]
    }
}
