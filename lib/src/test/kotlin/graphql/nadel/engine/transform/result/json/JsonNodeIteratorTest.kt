package graphql.nadel.engine.transform.result.json

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.jsonObjectMapper
import graphql.nadel.result.NadelResultPathBuilder
import graphql.nadel.result.NadelResultPathSegment
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JsonNodeIteratorTest {
    private data class TraversedJsonNode(
        override val queryPath: List<String>,
        override val resultPath: List<NadelResultPathSegment>,
        override val value: Any?,
    ) : EphemeralJsonNode() {
        constructor(other: EphemeralJsonNode) : this(
            queryPath = other.queryPath.toList(),
            resultPath = other.resultPath.toList(),
            value = other.value,
        )
    }

    @Test
    fun traverseObjects() {
        val root = jsonObjectMapper.readValue<JsonMap>(
            // language=JSON
            """
                {
                  "users": {
                    "id": "100",
                    "friend": {
                      "id": "100",
                      "phoneNumber": {
                        "value": "+61"
                      }
                    },
                    "email": "@.com"
                  }
                }
            """.trimIndent(),
        )

        val expectedTraversals = listOf(
            TraversedJsonNode(
                queryPath = emptyList(),
                resultPath = emptyList(),
                value = root,
            ),
            TraversedJsonNode(
                queryPath = listOf("users"),
                resultPath = NadelResultPathBuilder()
                    .add("users")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": "100", "friend": {"id": "100", "phoneNumber": {"value": "+61"}}, "email": "@.com"}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("users", "friend"),
                resultPath = NadelResultPathBuilder()
                    .add("users")
                    .add("friend")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": "100", "phoneNumber": {"value": "+61"}}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("users", "friend", "phoneNumber"),
                resultPath = NadelResultPathBuilder()
                    .add("users")
                    .add("friend")
                    .add("phoneNumber")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"value": "+61"}""",
                ),
            ),
        )

        val iterator = JsonNodeIterator(
            root = root,
            queryPath = NadelQueryPath(listOf("users", "friend", "phoneNumber")),
            flatten = true,
        )

        // When
        val traversed = iterator
            .asSequence()
            .map(::TraversedJsonNode)
            .toList()

        // Then
        val uniqueQueryPaths = traversed.mapTo(LinkedHashSet()) { it.queryPath }
        val uniqueResultPaths = traversed.mapTo(LinkedHashSet()) { it.resultPath }
        assertTrue(uniqueResultPaths.size == traversed.size)
        assertTrue(uniqueQueryPaths.size == traversed.size)

        assertTrue(traversed.size == expectedTraversals.size)
        traversed.zip(expectedTraversals)
            .forEach { (actual, expected) ->
                assertTrue(actual == expected)
            }
    }

    @Test
    fun traverseNull() {
        val root = jsonObjectMapper.readValue<JsonMap>(
            // language=JSON
            """
                {
                  "users": {
                    "id": "100",
                    "friend": null,
                    "email": "@.com"
                  }
                }
            """.trimIndent(),
        )

        val expectedTraversals = listOf(
            TraversedJsonNode(
                queryPath = emptyList(),
                resultPath = emptyList(),
                value = root,
            ),
            TraversedJsonNode(
                queryPath = listOf("users"),
                resultPath = NadelResultPathBuilder()
                    .add("users")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": "100", "friend": null, "email": "@.com"}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("users", "friend"),
                resultPath = NadelResultPathBuilder()
                    .add("users")
                    .add("friend")
                    .build(),
                value = null,
            ),
        )

        val iterator = JsonNodeIterator(
            root = root,
            queryPath = NadelQueryPath(listOf("users", "friend", "phoneNumber")),
            flatten = true,
        )

        // When
        val traversed = iterator
            .asSequence()
            .map(::TraversedJsonNode)
            .toList()

        // Then
        val uniqueQueryPaths = traversed.mapTo(LinkedHashSet()) { it.queryPath }
        val uniqueResultPaths = traversed.mapTo(LinkedHashSet()) { it.resultPath }
        assertTrue(uniqueResultPaths.size == traversed.size)
        assertTrue(uniqueQueryPaths.size == traversed.size)

        assertTrue(traversed.size == expectedTraversals.size)
        traversed.zip(expectedTraversals)
            .forEach { (actual, expected) ->
                assertTrue(actual == expected)
            }
    }

    @Test
    fun traverseArrays() {
        val root = jsonObjectMapper.readValue<JsonMap>(
            // language=JSON
            """
                {
                  "activities": {
                    "workedOn": [
                      {
                        "data": {
                          "id": 1
                        }
                      },
                      {
                      },
                      {
                        "value": {
                          "friend": null
                        }
                      },
                      {
                        "data": {
                          "friend": {
                            "id": 10
                          }
                        }
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        val expectedTraversals = listOf(
            TraversedJsonNode(
                queryPath = emptyList(),
                resultPath = NadelResultPathBuilder().build(),
                value = root
            ),
            TraversedJsonNode(
                queryPath = listOf("activities"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"workedOn": [{"data": {"id": 1}}, {}, {"value": {"friend": null}}, {"data": {"friend": {"id": 10}}}]}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[{"data": {"id": 1}}, {}, {"value": {"friend": null}}, {"data": {"friend": {"id": 10}}}]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"data": {"friend": {"id": 10}}}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .add("data")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"friend": {"id": 10}}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data", "friend"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .add("data")
                    .add("friend")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": 10}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"value": {"friend": null}}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(1)
                    .build(),
                value = jsonObjectMapper.readValue(
                    "{}",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"data": {"id": 1}}""",
                )
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(0)
                    .add("data")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": 1}""",
                ),
            ),
        )

        val iterator = JsonNodeIterator(
            root = root,
            queryPath = NadelQueryPath(listOf("activities", "workedOn", "data", "friend")),
            flatten = true,
        )

        // When
        val traversed = iterator
            .asSequence()
            .map(::TraversedJsonNode)
            .toList()

        // Then
        val uniqueResultPaths = traversed.mapTo(LinkedHashSet()) { it.resultPath }
        assertTrue(uniqueResultPaths.size == traversed.size)

        assertTrue(traversed.size == expectedTraversals.size)
        traversed.zip(expectedTraversals)
            .forEach { (actual, expected) ->
                assertTrue(actual == expected)
            }
    }

    @Test
    fun traverseNestedArrays() {
        val root = jsonObjectMapper.readValue<JsonMap>(
            // language=JSON
            """
                {
                  "activities": {
                    "workedOn": [
                      [
                        {
                          "data": {
                            "id": 1
                          }
                        }
                      ],
                      [],
                      [
                        {
                        },
                        {
                          "value": {
                            "friend": null
                          }
                        },
                        [
                          {
                            "data": null
                          }
                        ]
                      ],
                      [
                        {
                          "data": {
                            "friend": {
                              "id": 10
                            }
                          }
                        }
                      ]
                    ]
                  }
                }
            """.trimIndent(),
        )

        val expectedTraversals = listOf(
            TraversedJsonNode(
                queryPath = emptyList(),
                resultPath = NadelResultPathBuilder().build(),
                value = root
            ),
            TraversedJsonNode(
                queryPath = listOf("activities"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"workedOn":[[{"data":{"id":1}}],[],[{},{"value":{"friend":null}},[{"data":null}]],[{"data":{"friend":{"id":10}}}]]}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[[{"data":{"id":1}}],[],[{},{"value":{"friend":null}},[{"data":null}]],[{"data":{"friend":{"id":10}}}]]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[{"data":{"friend":{"id":10}}}]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"data":{"friend":{"id":10}}}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .add(0)
                    .add("data")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"friend": {"id": 10}}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data", "friend"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(3)
                    .add(0)
                    .add("data")
                    .add("friend")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id": 10}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[{},{"value":{"friend":null}},[{"data":null}]]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .add(2)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[{"data":null}]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .add(2)
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"data":null}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .add(2)
                    .add(0)
                    .add("data")
                    .build(),
                value = null,
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .add(1)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"value":{"friend":null}}"""
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(2)
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    "{}",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(1)
                    .build(),
                value = jsonObjectMapper.readValue(
                    "[]",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """[{"data":{"id":1}}]""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(0)
                    .add(0)
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"data":{"id":1}}""",
                ),
            ),
            TraversedJsonNode(
                queryPath = listOf("activities", "workedOn", "data"),
                resultPath = NadelResultPathBuilder()
                    .add("activities")
                    .add("workedOn")
                    .add(0)
                    .add(0)
                    .add("data")
                    .build(),
                value = jsonObjectMapper.readValue(
                    """{"id":1}""",
                ),
            ),
        )

        val iterator = JsonNodeIterator(
            root = root,
            queryPath = NadelQueryPath(listOf("activities", "workedOn", "data", "friend")),
            flatten = true,
        )

        // When
        val traversed = iterator
            .asSequence()
            .map(::TraversedJsonNode)
            .toList()

        // Then
        val uniqueResultPaths = traversed.mapTo(LinkedHashSet()) { it.resultPath }
        assertTrue(uniqueResultPaths.size == traversed.size)

        assertTrue(traversed.size == expectedTraversals.size)
        traversed.zip(expectedTraversals)
            .forEach { (actual, expected) ->
                assertTrue(actual == expected)
            }
    }
}
