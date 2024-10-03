package graphql.nadel.result

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.ExecutionResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.foldWhileNotNull
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.jsonObjectMapper
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelResultTrackerTest {
    @Test
    fun canSearch2dArrays() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": [
                        [
                            {"Hello": null},
                            {"World": null}
                        ],
                        [
                            {"Greetings": null},
                            {"Friend": null}
                        ],
                        [
                            {"Bye": null},
                            {"For now": null}
                        ]
                    ]
                }
            """.trimIndent()
        )
        val data = result.getData<List<List<JsonMap>>>()

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        for (i in 0..2) {
            for (j in 0..1) {
                assertTrue(
                    subject.getResultPath(
                        NadelQueryPath.root,
                        JsonNode(data[i][j]),
                    ) == NadelResultPathBuilder()
                        .add(i)
                        .add(j)
                        .build(),
                )
            }
        }
    }

    @Test
    fun canSearch3dArrays() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": {
                        "matrix": [
                            [
                                [
                                    {"Hello": null},
                                    {"World": null}
                                ],
                                [
                                    {"Greetings": null},
                                    {"Friend": null}
                                ],
                                [
                                    {"Bye": null},
                                    {"For now": null}
                                ]
                            ],
                            [
                                [
                                    {"Hello": null},
                                    {"World": null}
                                ],
                                [
                                    {"Greetings": null},
                                    {"Friend": null}
                                ],
                                [
                                    {"Bye": null},
                                    {"For now": null}
                                ]
                            ],
                            [
                                [
                                    {"Hello": null},
                                    {"World": null}
                                ],
                                [
                                    {"Greetings": null},
                                    {"Friend": null}
                                ],
                                [
                                    {"Bye": null},
                                    {"For now": null}
                                ]
                            ]
                        ]
                    }
                }
            """.trimIndent()
        )

        @Suppress("UNCHECKED_CAST")
        val data = result.getData<JsonMap>()["matrix"] as List<List<List<JsonMap>>>

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        for (i in 0..2) {
            for (j in 0..2) {
                for (k in 0..1) {
                    assertTrue(
                        subject.getResultPath(
                            NadelQueryPath(listOf("matrix")),
                            JsonNode(data[i][j][k]),
                        ) == NadelResultPathBuilder()
                            .add("matrix")
                            .add(i)
                            .add(j)
                            .add(k)
                            .build(),
                    )
                }
            }
        }
    }

    /**
     * Shouldn't really happen in a real scenario, but we can support it.
     *
     * i.e. this tests that matrix is not guaranteed to be a 2d or 3d array
     */
    @Test
    fun canNavigateVariableNesting() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": {
                        "matrix": [
                            [
                                [
                                    []
                                ],
                                [
                                    [],
                                    {"World": 10},
                                    []
                                ]
                            ],
                            [
                                {
                                    "id": 10
                                }
                            ],
                            {
                                "name": 10
                            }
                        ]
                    }
                }
            """.trimIndent()
        )

        val data = result.getData<JsonMap>()

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        JsonNode(data).dfs { path, value ->
            if (value is AnyList || value is AnyMap) {
                val queryPath = NadelQueryPath(
                    path
                        .value
                        .filterIsInstance<NadelResultPathSegment.Object>()
                        .map(NadelResultPathSegment.Object::key),
                )

                assertTrue(subject.getResultPath(queryPath, JsonNode(value)) == path)
            }
        }
    }

    /**
     * Ensure that when we reach a dead end array, we can keep searching.
     */
    @Test
    fun searchesEmptyArrays() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": {
                        "users": [
                            {
                                "friends": []
                            },
                            {
                                "friends": [
                                    {
                                        "name": "Who was it again?"
                                    }
                                ]
                            },
                            {
                                "friends": []
                            }
                        ]
                    }
                }
            """.trimIndent()
        )

        val data = result.getData<JsonMap>()

        val toFindPath = NadelResultPathBuilder()
            .add("users")
            .add(1)
            .add("friends")
            .add(0)
            .build()
        val toFind = JsonNode(data).getAt(toFindPath)!!

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        val queryPath = NadelQueryPath(listOf("users", "friends"))

        assertTrue(subject.getResultPath(queryPath, JsonNode(toFind)) == toFindPath)
    }

    /**
     * Ensure that when we reach a dead end array, we can keep searching.
     */
    @Test
    fun canHandleObjectDeadEnds() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": {
                        "users": [
                            {},
                            {
                                "friends": []
                            },
                            {
                                "friends": [null]
                            },
                            {
                                "friends": [
                                    null,
                                    {
                                        "user": {
                                            "name": "Lando Won"
                                        }
                                    },
                                    {},
                                    null
                                ]
                            },
                            {
                                "friends": [
                                    null,
                                    {},
                                    null,
                                    null
                                ]
                            },
                            {
                                "friends": null
                            },
                            {}
                        ]
                    }
                }
            """.trimIndent()
        )

        val data = result.getData<JsonMap>()

        val toFindPath = NadelResultPathBuilder()
            .add("users")
            .add(3)
            .add("friends")
            .add(1)
            .add("user")
            .build()
        val toFind = JsonNode(data).getAt(toFindPath)!!
        assertTrue(toFind is AnyMap && toFind["name"] == "Lando Won")

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        val queryPath = NadelQueryPath(listOf("users", "friends", "user"))

        assertTrue(subject.getResultPath(queryPath, JsonNode(toFind)) == toFindPath)
    }

    /**
     * Not really a real GraphQL scenario, but ensures we support navigating it.
     *
     * i.e. Here matrix.x and matrix.y are sometimes arrays, and sometimes objects.
     */
    @Test
    fun arraysMixedWithObjects() = runTest {
        // Given
        val result = resultOf(
            """
                {
                    "data": {
                        "matrix": [
                            {
                                "x": [
                                    {"value": 10}
                                ]
                            },
                            {
                                "y": {
                                    "value": 10
                                }
                            },
                            {
                                "y": [
                                    {"value": 10}
                                ]
                            },
                            {
                                "x": {
                                    "value": 10
                                }
                            },
                            {
                                "x": {
                                    "value": 10
                                }
                            },
                            {
                                "z": {
                                    "val": 10
                                }
                            },
                            {
                                "y": {
                                    "value": 10
                                }
                            }
                        ]
                    }
                }
            """.trimIndent()
        )

        val data = result.getData<JsonMap>()

        val subject = NadelResultTracker()
        subject.complete(result)

        // Then
        JsonNode(data).dfs { path, value ->
            if (value is AnyList || value is AnyMap) {
                val queryPath = NadelQueryPath(
                    path
                        .value
                        .filterIsInstance<NadelResultPathSegment.Object>()
                        .map(NadelResultPathSegment.Object::key),
                )

                assertTrue(subject.getResultPath(queryPath, JsonNode(value)) == path)
            }
        }
    }

    /**
     * Just tests our [JsonNode.dfs] test implementation.
     *
     * If that didn't work then all our tests may silently pass.
     */
    @Test
    fun dfs() = runTest {
        val result = resultOf(
            """
                {
                    "data": {
                        "matrix": [
                            {
                                "x": [
                                    {"value": 10}
                                ]
                            },
                            {
                                "y": {
                                    "value": 10
                                }
                            },
                            {
                                "x": {
                                    "value": 10
                                }
                            },
                            {
                                "x": {
                                    "value": 10
                                }
                            },
                            {
                                "z": {
                                    "val": 10
                                }
                            },
                            {
                                "y": {
                                    "value": 10
                                }
                            }
                        ]
                    }
                }
            """.trimIndent()
        )

        val data = result.getData<JsonMap>()

        // When
        val visited = mutableListOf<Pair<NadelResultPath, Any?>>()
        JsonNode(data).dfs { path, value ->
            visited.add(path to value)
        }

        // Then
        assertTrue(visited.mapTo(HashSet()) { (path) -> path }.size == visited.size)

        // Code to generate expected
        // visited.forEach { (path, value) ->
        //     val code = path
        //         .joinToString(prefix = "NadelResultPathBuilder()", postfix = ".build()", separator = "") {
        //             when (it) {
        //                 is NadelResultPathSegment.Array -> ".add(${it.index})"
        //                 is NadelResultPathSegment.Object -> ".add(${jsonObjectMapper.writeValueAsString(it.key)})"
        //             }
        //         }
        //     // Write twice to escape string
        //     val serializedValue = jsonObjectMapper.writeValueAsString(
        //         jsonObjectMapper.writeValueAsString(value),
        //     )
        //     println("$code to \njsonObjectMapper.readValue<Any?>(${serializedValue}),")
        // }

        val expected = listOf(
            NadelResultPathBuilder().build() to
                jsonObjectMapper.readValue<Any?>("""{"matrix":[{"x":[{"value":10}]},{"y":{"value":10}},{"x":{"value":10}},{"x":{"value":10}},{"z":{"val":10}},{"y":{"value":10}}]}"""),
            NadelResultPathBuilder().add("matrix").build() to
                jsonObjectMapper.readValue<Any?>("""[{"x":[{"value":10}]},{"y":{"value":10}},{"x":{"value":10}},{"x":{"value":10}},{"z":{"val":10}},{"y":{"value":10}}]"""),
            NadelResultPathBuilder().add("matrix").add(0).build() to
                jsonObjectMapper.readValue<Any?>("""{"x":[{"value":10}]}"""),
            NadelResultPathBuilder().add("matrix").add(0).add("x").build() to
                jsonObjectMapper.readValue<Any?>("""[{"value":10}]"""),
            NadelResultPathBuilder().add("matrix").add(0).add("x").add(0).build() to
                jsonObjectMapper.readValue<Any?>("""{"value":10}"""),
            NadelResultPathBuilder().add("matrix").add(0).add("x").add(0).add("value").build() to
                jsonObjectMapper.readValue<Any?>("10"),
            NadelResultPathBuilder().add("matrix").add(1).build() to
                jsonObjectMapper.readValue<Any?>("""{"y":{"value":10}}"""),
            NadelResultPathBuilder().add("matrix").add(1).add("y").build() to
                jsonObjectMapper.readValue<Any?>("""{"value":10}"""),
            NadelResultPathBuilder().add("matrix").add(1).add("y").add("value").build() to
                jsonObjectMapper.readValue<Any?>("10"),
            NadelResultPathBuilder().add("matrix").add(2).build() to
                jsonObjectMapper.readValue<Any?>("""{"x":{"value":10}}"""),
            NadelResultPathBuilder().add("matrix").add(2).add("x").build() to
                jsonObjectMapper.readValue<Any?>("""{"value":10}"""),
            NadelResultPathBuilder().add("matrix").add(2).add("x").add("value").build() to
                jsonObjectMapper.readValue<Any?>("10"),
            NadelResultPathBuilder().add("matrix").add(3).build() to
                jsonObjectMapper.readValue<Any?>("""{"x":{"value":10}}"""),
            NadelResultPathBuilder().add("matrix").add(3).add("x").build() to
                jsonObjectMapper.readValue<Any?>("""{"value":10}"""),
            NadelResultPathBuilder().add("matrix").add(3).add("x").add("value").build() to
                jsonObjectMapper.readValue<Any?>("10"),
            NadelResultPathBuilder().add("matrix").add(4).build() to
                jsonObjectMapper.readValue<Any?>("""{"z":{"val":10}}"""),
            NadelResultPathBuilder().add("matrix").add(4).add("z").build() to
                jsonObjectMapper.readValue<Any?>("""{"val":10}"""),
            NadelResultPathBuilder().add("matrix").add(4).add("z").add("val").build() to
                jsonObjectMapper.readValue<Any?>("10"),
            NadelResultPathBuilder().add("matrix").add(5).build() to
                jsonObjectMapper.readValue<Any?>("""{"y":{"value":10}}"""),
            NadelResultPathBuilder().add("matrix").add(5).add("y").build() to
                jsonObjectMapper.readValue<Any?>("""{"value":10}"""),
            NadelResultPathBuilder().add("matrix").add(5).add("y").add("value").build() to
                jsonObjectMapper.readValue<Any?>("10"),
        )

        assertTrue(visited == expected)
    }

    private fun resultOf(json: String): ExecutionResult {
        val result = jsonObjectMapper.readValue<JsonMap>(json)
        @Suppress("UNCHECKED_CAST")
        return ExecutionResult.newExecutionResult()
            .data(result["data"])
            .errors((result["errors"] as List<JsonMap>?)?.map(::toGraphQLError))
            .extensions(result["extensions"] as Map<Any?, Any?>?)
            .build()
    }

    /**
     * But why are you testing NadelResultTracker by effectively duplicating the functionality??
     *
     * Well the point of NadelResultTracker is that you're finding one node.
     * The tricky part is creating one result node path and reusing it as we're traversing.
     * It's much easier to reason the logic in this recursive function.
     */
    private suspend fun JsonNode.dfs(
        path: NadelResultPath = NadelResultPath.empty,
        onConsume: suspend (NadelResultPath, Any?) -> Unit,
    ) {
        onConsume(path, value)

        when (val value = value) {
            is AnyMap -> value.forEach { (key, element) ->
                assertTrue(key is String)
                JsonNode(element).dfs(path.toBuilder().add(key).build(), onConsume)
            }
            is AnyList -> value.forEachIndexed { index, element ->
                JsonNode(element).dfs(path.toBuilder().add(index).build(), onConsume)
            }
        }
    }

    private fun JsonNode.getAt(
        path: NadelResultPath,
    ): Any? {
        return getAt(path.value)
    }

    private fun JsonNode.getAt(
        path: List<NadelResultPathSegment>,
    ): Any? {
        return path.foldWhileNotNull(value) { prev, pathSegment ->
            when (pathSegment) {
                is NadelResultPathSegment.Array -> (prev as AnyList)[pathSegment.index]
                is NadelResultPathSegment.Object -> (prev as AnyMap)[pathSegment.key]
            }
        }
    }
}
