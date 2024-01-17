package graphql.nadel.engine.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionUtilTest {
    @Test
    fun `partitionCount counts number of values in each partition`() {
        val partition = listOf(1, 4, 6, 8, 10)

        // When
        val (first, second) = partition.partitionCount { it % 2 == 0 }

        // Then
        assertTrue(first == 4)
        assertTrue(second == 1)
    }

    @Test
    fun `partitionCount returns zero if list is empty`() {
        val partition = emptyList<Int>()

        // When
        val (first, second) = partition.partitionCount { it % 2 == 0 }

        // Then
        assertTrue(first == 0)
        assertTrue(second == 0)
    }

    @Test
    fun `zipOrThrow completes successfully if both sequences have same number of items`() {
        val sequenceA = sequenceOf(2, 4, 6, 8, 10)
        val sequenceB = sequenceOf(1, 3, 5, 7, 9)

        // When
        val output = sequenceA
            .zipOrThrow(sequenceB) {
                throw IllegalArgumentException()
            }
            .toList()

        // Then
        assertTrue(output == sequenceA.zip(sequenceB).toList())
    }

    @Test
    fun `zipOrThrow throws error if left sequence has less items`() {
        val sequenceA = sequenceOf(2, 4, 6, 8)
        val sequenceB = sequenceOf(1, 3, 5, 7, 9)

        // When
        val exception = assertThrows<IllegalArgumentException> {
            sequenceA
                .zipOrThrow(sequenceB) {
                    throw IllegalArgumentException("Bang")
                }
                .toList()
        }

        // Then
        assertTrue(exception.message == "Bang")
    }

    @Test
    fun `zipOrThrow throws error if right sequence has less items`() {
        val sequenceA = sequenceOf(2, 4, 6, 8, 10)
        val sequenceB = sequenceOf(1, 3, 5, 7)

        // When
        val exception = assertThrows<IllegalArgumentException> {
            sequenceA
                .zipOrThrow(sequenceB) {
                    throw IllegalArgumentException("Bang")
                }
                .toList()
        }

        // Then
        assertTrue(exception.message == "Bang")
    }

    @Test
    fun `zipOrThrow completes successfully if both sequences are empty`() {
        val sequenceA = sequenceOf<String>()
        val sequenceB = sequenceOf<String>()

        // When
        val output = sequenceA
            .zipOrThrow(sequenceB) {
                throw IllegalArgumentException("Bang")
            }
            .toList()

        // Then
        assertTrue(output.isEmpty())
    }

    @Test
    fun `zipOrThrow with List`() {
        val sequenceA = sequenceOf(1, 2)
        val sequenceB = listOf("hello", "world")

        // When
        val output = sequenceA
            .zipOrThrow(sequenceB) {
                throw IllegalArgumentException("Bang")
            }
            .toList()

        // Then
        assertTrue(output == sequenceA.zip(sequenceB.asSequence()).toList())
    }

    @Test
    fun `startsWith returns true if list is a prefix`() {
        assertTrue(listOf(1, 3, 10).startsWith(listOf(1, 3)))
        assertTrue(listOf("hello", 10, false).startsWith(listOf("hello", 10)))
    }

    @Test
    fun `startsWith returns true if they are equal`() {
        assertTrue(listOf(1, 3, 10).startsWith(listOf(1, 3, 10)))
    }

    @Test
    fun `startsWith returns true if other list is empty`() {
        assertTrue(listOf(1, 3, 10).startsWith(emptyList()))
    }

    @Test
    fun `startsWith returns false if prefix list is longer`() {
        assertFalse(listOf(1, 3, 10).startsWith(listOf(1, 3, 10, 5)))
    }

    @Test
    fun `startsWith returns false if other list is not a prefix`() {
        assertFalse(listOf(1, 3, 10).startsWith(listOf(10)))
        assertFalse(listOf("hello", 10, false).startsWith(listOf(false, "hello", 10)))
    }
}
