package graphql.nadel.engine.tracking


import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.nadel.engine.NadelContext
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static graphql.nadel.testutils.ExecutionResultNodeUtil.esi
import static graphql.nadel.testutils.ExecutionResultNodeUtil.leaf
import static graphql.nadel.testutils.ExecutionResultNodeUtil.list
import static graphql.nadel.testutils.ExecutionResultNodeUtil.root

class FieldTrackingTest extends Specification {

    def executionContext

    void setup() {
        executionContext = Mock(ExecutionContext)
        1 * executionContext.getContext() >> NadelContext.newContext().build()
    }

    class TestInstrumentation implements NadelInstrumentation {
        def dispatched = [:]
        def completed = [:]

        @Override
        InstrumentationContext<ExecutionResultNode> beginFieldFetch(NadelInstrumentationFetchFieldParameters parameters) {
            return new InstrumentationContext<ExecutionResultNode>() {
                @Override
                void onDispatched(CompletableFuture<ExecutionResultNode> result) {
                    dispatched.compute(parameters.executionStepInfo.path.toString(), addOne())
                }

                @Override
                void onCompleted(ExecutionResultNode result, Throwable t) {
                    completed.compute(parameters.executionStepInfo.path.toString(), addOne())
                }

                private Closure<Integer> addOne() {
                    { k, v -> v == null ? 1 : v + 1 }
                }
            }
        }
    }

    @Ignore
    def "can dispatch and complete fields only once"() {
        def instrumentation = new TestInstrumentation()
        def fieldTracking = new FieldTracking(instrumentation, executionContext)
        when:
        fieldTracking.fieldsDispatched([esi("/a"), esi("/b")])

        then:
        instrumentation.dispatched == ["/a": 1, "/b": 1]
        instrumentation.completed == [:]

        when:
        fieldTracking.fieldsDispatched([esi("/a"), esi("/b")])

        then:
        instrumentation.dispatched == ["/a": 1, "/b": 1]
        instrumentation.completed == [:]

        when:
        fieldTracking.fieldsCompleted(root([leaf("/a"), leaf("/b")]), null)

        then:
        instrumentation.dispatched == ["/a": 1, "/b": 1]
        instrumentation.completed == ["/a": 1, "/b": 1]

        when:
        fieldTracking.fieldsCompleted(root([leaf("/a"), leaf("/b")]), null)

        then:
        instrumentation.dispatched == ["/a": 1, "/b": 1]
        instrumentation.completed == ["/a": 1, "/b": 1]
    }

    @Ignore
    def "will dispatch if it was not previously dispatched"() {
        def instrumentation = new TestInstrumentation()
        def fieldTracking = new FieldTracking(instrumentation, executionContext)
        when:
        fieldTracking.fieldsCompleted(root([leaf("/a"), leaf("/b")]), null)

        then:
        instrumentation.dispatched == ["/a": 1, "/b": 1]
        instrumentation.completed == ["/a": 1, "/b": 1]
    }

    def "will ignore list only paths"() {
        def instrumentation = new TestInstrumentation()
        def fieldTracking = new FieldTracking(instrumentation, executionContext)

        when:
        fieldTracking.fieldsDispatched([esi("/a"), esi("/a[0]"), esi("/a[0]/b")])
        then:
        instrumentation.dispatched == ["/a": 1, "/a[0]/b": 1]

        when:
        fieldTracking.fieldsCompleted(root([list("/a", [leaf("/a[0]"), leaf("/a[0]/b")])]), null)

        then:
        instrumentation.dispatched == ["/a": 1, "/a[0]/b": 1]
    }
}
