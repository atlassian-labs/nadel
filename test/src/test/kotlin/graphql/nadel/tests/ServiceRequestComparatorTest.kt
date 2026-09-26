package graphql.nadel.tests

import graphql.parser.Parser
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceRequestComparatorTest {
    @Test
    fun `variable names and definition order do not affect a service request`() {
        val matches = serviceRequestsMatchIgnoringVariableNames(
            expectedDocument = Parser().parseDocument(
                """
                    query Test(${'$'}id: ID!, ${'$'}include: Boolean!) {
                      node(id: ${'$'}id) {
                        id @include(if: ${'$'}include)
                      }
                    }
                """.trimIndent(),
            ),
            expectedVariables = linkedMapOf(
                "id" to "ari:example:1",
                "include" to true,
            ),
            actualDocument = Parser().parseDocument(
                """
                    query Test(${'$'}condition: Boolean!, ${'$'}identifier: ID!) {
                      node(id: ${'$'}identifier) {
                        id @include(if: ${'$'}condition)
                      }
                    }
                """.trimIndent(),
            ),
            actualVariables = linkedMapOf(
                "condition" to true,
                "identifier" to "ari:example:1",
            ),
        )

        assertTrue(matches)
    }

    @Test
    fun `variable values must follow their query bindings`() {
        val matches = serviceRequestsMatchIgnoringVariableNames(
            expectedDocument = Parser().parseDocument(
                """
                    query Test(${'$'}first: String!, ${'$'}second: String!) {
                      echo(first: ${'$'}first, second: ${'$'}second)
                    }
                """.trimIndent(),
            ),
            expectedVariables = mapOf(
                "first" to "one",
                "second" to "two",
            ),
            actualDocument = Parser().parseDocument(
                """
                    query Test(${'$'}a: String!, ${'$'}b: String!) {
                      echo(first: ${'$'}b, second: ${'$'}a)
                    }
                """.trimIndent(),
            ),
            actualVariables = mapOf(
                "a" to "one",
                "b" to "two",
            ),
        )

        assertFalse(matches)
    }

    @Test
    fun `variables map keys must match query variable names`() {
        assertFailsWith<IllegalArgumentException> {
            serviceRequestsMatchIgnoringVariableNames(
                expectedDocument = Parser().parseDocument(
                    """
                        query Test(${'$'}id: ID!) {
                          node(id: ${'$'}id) {
                            id
                          }
                        }
                    """.trimIndent(),
                ),
                expectedVariables = mapOf("id" to "ari:example:1"),
                actualDocument = Parser().parseDocument(
                    """
                        query Test(${'$'}identifier: ID!) {
                          node(id: ${'$'}identifier) {
                            id
                          }
                        }
                    """.trimIndent(),
                ),
                actualVariables = mapOf("id" to "ari:example:1"),
            )
        }
    }
}
