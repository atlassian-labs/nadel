package graphql.nadel.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ServiceRequestCanonicalizerTest {
    @Test
    fun `variable names and definition order do not affect a service request`() {
        val expected = canonicalizeServiceRequest(
            query = """
                query Test(${'$'}id: ID!, ${'$'}include: Boolean!) {
                  node(id: ${'$'}id) {
                    ...Fields @include(if: ${'$'}include)
                  }
                }

                fragment Fields on Node {
                  id
                }
            """.trimIndent(),
            variables = linkedMapOf(
                "id" to "ari:example:1",
                "include" to true,
            ),
        )
        val actual = canonicalizeServiceRequest(
            query = """
                fragment Fields on Node {
                  id
                }

                query Test(${'$'}condition: Boolean!, ${'$'}identifier: ID!) {
                  node(id: ${'$'}identifier) {
                    ...Fields @include(if: ${'$'}condition)
                  }
                }
            """.trimIndent(),
            variables = linkedMapOf(
                "condition" to true,
                "identifier" to "ari:example:1",
            ),
        )

        assertEquals(expected, actual)
        assertEquals(listOf("v0", "v1"), actual.variables.keys.toList())
    }

    @Test
    fun `variable bindings still affect a service request`() {
        val expected = canonicalizeServiceRequest(
            query = """
                query Test(${'$'}first: String!, ${'$'}second: String!) {
                  echo(first: ${'$'}first, second: ${'$'}second)
                }
            """.trimIndent(),
            variables = mapOf(
                "first" to "one",
                "second" to "two",
            ),
        )
        val actual = canonicalizeServiceRequest(
            query = """
                query Test(${'$'}a: String!, ${'$'}b: String!) {
                  echo(first: ${'$'}b, second: ${'$'}a)
                }
            """.trimIndent(),
            variables = mapOf(
                "a" to "one",
                "b" to "two",
            ),
        )

        assertNotEquals(expected, actual)
    }
}
