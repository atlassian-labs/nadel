package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.next.TestSnapshot
import kotlin.test.assertEquals

/**
 * A shared service call exposes every contributing hydration through the additive consumer
 * metadata, while the normal snapshot continues to characterize the generated request.
 */
class BatchHydrationCoalescingConsumerMetadataTest : BatchHydrationCoalescingEqualSelectionsTest() {
    private val capturedConsumers = mutableListOf<List<Pair<String, List<String>>>>()

    override fun makeServiceExecution(service: Service): ServiceExecution {
        val delegate = super.makeServiceExecution(service)

        return ServiceExecution { parameters ->
            parameters.hydrationDetails
                ?.consumerDetails
                ?.map { consumer ->
                    consumer.hydrationVirtualField.toString() to consumer.fieldPath
                }
                ?.let(capturedConsumers::add)

            delegate.execute(parameters)
        }
    }

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        assertEquals(
            expected = listOf(
                listOf(
                    "Issue.assignee" to listOf("issues", "assignee"),
                    "Issue.reporter" to listOf("issues", "reporter"),
                ),
            ),
            actual = capturedConsumers,
        )
    }

    override fun getTestSnapshot(): TestSnapshot {
        return BatchHydrationCoalescingEqualSelectionsTestSnapshot()
    }
}
