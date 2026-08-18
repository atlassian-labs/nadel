package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.Service
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.test.mock
import io.mockk.every
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NadelBatchHydrationCoalescingRoundTest {
    @Test
    fun `zero submissions complete without dispatching`() = runTest {
        var dispatchCalls = 0
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = {
                dispatchCalls++
                emptyMap()
            },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)

        val firstOutput = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TEST_TIMEOUT_MILLIS) {
                firstParticipant.completeAndTakeOutput()
            }
        }
        val secondOutput = withTimeout(TEST_TIMEOUT_MILLIS) {
            secondParticipant.completeAndTakeOutput()
        }

        assertEquals(NadelCoalescedBatchHydrationOutput.EMPTY, firstOutput.await())
        assertEquals(NadelCoalescedBatchHydrationOutput.EMPTY, secondOutput)
        assertEquals(0, dispatchCalls)
    }

    @Test
    fun `submissions across participants are dispatched once and delivered only to their owner`() = runTest {
        val firstHydration = preparedHydration(0)
        val secondHydration = preparedHydration(0)
        val thirdHydration = preparedHydration(1)
        val firstOutput = output("first")
        val secondOutput = output("second")
        val thirdOutput = output("third")
        var dispatchCalls = 0
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = { hydrations ->
                dispatchCalls++
                assertEquals(
                    listOf(firstHydration, thirdHydration, secondHydration),
                    hydrations,
                )
                mapOf(
                    firstHydration to firstOutput,
                    secondHydration to secondOutput,
                    thirdHydration to thirdOutput,
                )
            },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)
        assertTrue(firstParticipant.trySubmit(firstHydration))
        assertTrue(secondParticipant.trySubmit(secondHydration))
        assertTrue(firstParticipant.trySubmit(thirdHydration))

        val firstOwnerOutput = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TEST_TIMEOUT_MILLIS) {
                firstParticipant.completeAndTakeOutput()
            }
        }
        val secondOwnerOutput = withTimeout(TEST_TIMEOUT_MILLIS) {
            secondParticipant.completeAndTakeOutput()
        }

        assertEquals(firstOutput + thirdOutput, firstOwnerOutput.await())
        assertEquals(secondOutput, secondOwnerOutput)
        assertEquals(1, dispatchCalls)
        assertEquals(
            NadelCoalescedBatchHydrationOutput.EMPTY,
            withTimeout(TEST_TIMEOUT_MILLIS) {
                firstParticipant.completeAndTakeOutput()
            },
        )
        assertEquals(
            NadelCoalescedBatchHydrationOutput.EMPTY,
            withTimeout(TEST_TIMEOUT_MILLIS) {
                secondParticipant.completeAndTakeOutput()
            },
        )
        assertEquals(1, dispatchCalls)
    }

    @Test
    fun `submit is rejected after participant completion starts`() = runTest {
        val onTimeHydration = preparedHydration()
        val lateHydration = preparedHydration()
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = { emptyMap() },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)

        val firstCompletion = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TEST_TIMEOUT_MILLIS) {
                firstParticipant.complete()
            }
        }

        assertFalse(firstParticipant.trySubmit(lateHydration))
        assertTrue(secondParticipant.trySubmit(onTimeHydration))
        withTimeout(TEST_TIMEOUT_MILLIS) {
            secondParticipant.complete()
        }
        withTimeout(TEST_TIMEOUT_MILLIS) {
            firstCompletion.await()
        }
        assertFalse(secondParticipant.trySubmit(lateHydration))
    }

    @Test
    fun `dispatch failure reaches every waiter`() = runTest {
        val hydration = preparedHydration()
        val failure = IllegalStateException("dispatch failed")
        var dispatchCalls = 0
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = {
                dispatchCalls++
                throw failure
            },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)
        assertTrue(firstParticipant.trySubmit(hydration))

        val firstFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                withTimeout(TEST_TIMEOUT_MILLIS) {
                    firstParticipant.completeAndTakeOutput()
                }
            }.exceptionOrNull()
        }
        val secondFailure = runCatching {
            withTimeout(TEST_TIMEOUT_MILLIS) {
                secondParticipant.completeAndTakeOutput()
            }
        }.exceptionOrNull()

        val firstWaiterFailure = assertIs<IllegalStateException>(firstFailure.await())
        val secondWaiterFailure = assertIs<IllegalStateException>(secondFailure)
        assertEquals(failure.message, firstWaiterFailure.message)
        assertEquals(failure.message, secondWaiterFailure.message)
        assertEquals(1, dispatchCalls)
    }

    @Test
    fun `abort fails waiters without waiting for every participant`() = runTest {
        val failure = IllegalStateException("source execution failed")
        var dispatchCalls = 0
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = {
                dispatchCalls++
                emptyMap()
            },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)
        val firstFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                withTimeout(TEST_TIMEOUT_MILLIS) {
                    firstParticipant.complete()
                }
            }.exceptionOrNull()
        }

        secondParticipant.abort(failure)

        assertEquals(failure.message, assertIs<IllegalStateException>(firstFailure.await()).message)
        assertFalse(firstParticipant.trySubmit(preparedHydration()))
        assertFalse(secondParticipant.trySubmit(preparedHydration()))
        assertEquals(0, dispatchCalls)
    }

    @Test
    fun `cancelling complete aborts a sibling waiter`() = runTest {
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 3,
            enabledBackingServices = emptySet(),
            dispatch = { emptyMap() },
        )
        val cancelledParticipant = round.participant(0)
        val siblingParticipant = round.participant(1)
        val siblingFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                withTimeout(TEST_TIMEOUT_MILLIS) {
                    siblingParticipant.completeAndTakeOutput()
                }
            }.exceptionOrNull()
        }
        val cancelledCompletion = async(start = CoroutineStart.UNDISPATCHED) {
            cancelledParticipant.complete()
        }

        cancelledCompletion.cancel(CancellationException("cancelled source execution"))
        cancelledCompletion.join()

        val failure = withTimeout(TEST_TIMEOUT_MILLIS) {
            siblingFailure.await()
        }
        assertEquals(
            "cancelled source execution",
            assertIs<CancellationException>(failure).message,
        )
    }

    @Test
    fun `cancelling complete and take output aborts a sibling waiter`() = runTest {
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 3,
            enabledBackingServices = emptySet(),
            dispatch = { emptyMap() },
        )
        val cancelledParticipant = round.participant(0)
        val siblingParticipant = round.participant(1)
        val siblingFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                withTimeout(TEST_TIMEOUT_MILLIS) {
                    siblingParticipant.complete()
                }
            }.exceptionOrNull()
        }
        val cancelledCompletion = async(start = CoroutineStart.UNDISPATCHED) {
            cancelledParticipant.completeAndTakeOutput()
        }

        cancelledCompletion.cancel(CancellationException("cancelled result transformation"))
        cancelledCompletion.join()

        val failure = withTimeout(TEST_TIMEOUT_MILLIS) {
            siblingFailure.await()
        }
        assertEquals(
            "cancelled result transformation",
            assertIs<CancellationException>(failure).message,
        )
    }

    @Test
    fun `duplicate completion does not wait or redispatch`() = runTest {
        val hydration = preparedHydration()
        var dispatchCalls = 0
        val round = NadelBatchHydrationCoalescingRound(
            participantCount = 2,
            enabledBackingServices = emptySet(),
            dispatch = {
                dispatchCalls++
                mapOf(hydration to NadelCoalescedBatchHydrationOutput.EMPTY)
            },
        )
        val firstParticipant = round.participant(0)
        val secondParticipant = round.participant(1)
        assertTrue(firstParticipant.trySubmit(hydration))

        val firstCompletion = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TEST_TIMEOUT_MILLIS) {
                firstParticipant.complete()
            }
        }
        withTimeout(TEST_TIMEOUT_MILLIS) {
            firstParticipant.complete()
        }
        withTimeout(TEST_TIMEOUT_MILLIS) {
            secondParticipant.complete()
        }
        withTimeout(TEST_TIMEOUT_MILLIS) {
            firstCompletion.await()
        }
        withTimeout(TEST_TIMEOUT_MILLIS) {
            firstParticipant.complete()
            secondParticipant.complete()
        }

        assertEquals(1, dispatchCalls)
    }

    @Test
    fun `participant reports enabled backing services until it detaches`() = runTest {
        val enabledService = mock<Service>()
        val disabledService = mock<Service>()
        val participant = NadelBatchHydrationCoalescingRound(
            participantCount = 1,
            enabledBackingServices = setOf(enabledService),
            dispatch = { emptyMap() },
        ).participant(0)

        assertTrue(participant.isEnabledFor(enabledService))
        assertFalse(participant.isEnabledFor(disabledService))
        participant.complete()
        assertFalse(participant.isEnabledFor(enabledService))
    }

    private fun preparedHydration(
        fieldOrder: Int = 0,
    ): NadelNewBatchHydrator.PreparedBatchHydration {
        return mock { hydration ->
            every { hydration.operationFieldOrder } returns listOf(fieldOrder)
        }
    }

    private fun output(
        key: String,
    ): NadelCoalescedBatchHydrationOutput {
        return NadelCoalescedBatchHydrationOutput(
            instructions = listOf(
                NadelResultInstruction.Remove(
                    subject = JsonNode(mutableMapOf<String, Any?>()),
                    key = NadelResultKey(key),
                ),
            ),
        )
    }

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 1_000L
    }
}
