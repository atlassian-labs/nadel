package graphql.nadel.util

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class IteratorUtilTest {
    @Test
    fun `can split sequences`() {
        // Given
        val input = sequenceOf("", "Hello", "world", "very cool", " ", " my", " ", "name", " ", "is", "Iterator")

        // When
        val result = input
            .splitBy {
                it.isBlank()
            }
            .toList()

        // Then
        assertTrue(
            result == listOf(
                listOf(),
                listOf("Hello", "world", "very cool"),
                listOf(" my"),
                listOf("name"),
                listOf("is", "Iterator"),
            ),
        )
    }

    @Test
    fun `can split lists`() {
        // Given
        val input = listOf("", "Hello", "world", "very cool", " ", " my", " ", "name", " ", "is", "Iterator")

        // When
        val result = input.splitBy {
            it.isBlank()
        }

        // Then
        assertTrue(
            result == listOf(
                listOf(),
                listOf("Hello", "world", "very cool"),
                listOf(" my"),
                listOf("name"),
                listOf("is", "Iterator"),
            ),
        )
    }
}
