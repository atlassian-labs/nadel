package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.result.NadelResultPath
import graphql.nadel.result.NadelResultPathSegment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NadelResultViewTest {
    @Test
    fun `keeps equal nodes as distinct locatable occurrences`() {
        val firstDuplicate = mutableMapOf<String, Any?>("id" to "same")
        val secondDuplicate = mutableMapOf<String, Any?>("id" to "same")
        val thirdDuplicate = mutableMapOf<String, Any?>("id" to "same")
        val view = NadelResultView.root(
            data = mapOf(
                "groups" to listOf(
                    mapOf("members" to listOf(firstDuplicate, secondDuplicate)),
                    mapOf("members" to listOf(thirdDuplicate)),
                ),
            ),
        )

        val occurrences = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("groups", "members")),
            flatten = true,
        )

        assertEquals(
            expected = listOf(firstDuplicate, secondDuplicate, thirdDuplicate),
            actual = occurrences.map(NadelResultOccurrence::node).map(JsonNode::value),
        )
        assertEquals(
            expected = listOf(
                listOf("groups", 0, "members", 0),
                listOf("groups", 0, "members", 1),
                listOf("groups", 1, "members", 0),
            ),
            actual = occurrences.map { occurrence ->
                view.getResultPath(occurrence)?.toRawPath()
            },
        )
    }

    @Test
    fun `does not invent a path when one object identity occurs more than once`() {
        val sharedUser = mutableMapOf<String, Any?>("id" to "same")
        val view = NadelResultView.root(
            data = mapOf(
                "primary" to sharedUser,
                "secondary" to sharedUser,
            ),
        )

        val primary = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("primary")),
        ).single()
        val secondary = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("secondary")),
        ).single()

        assertNull(view.getResultPath(primary))
        assertNull(view.getResultPath(secondary))
    }

    @Test
    fun `resolves an occurrence after structural result mutation`() {
        val user = mutableMapOf<String, Any?>("id" to "one")
        val underlyingContainer = mutableMapOf<String, Any?>(
            "aliasedUser" to user,
        )
        val data = mutableMapOf<String, Any?>(
            "viewer" to underlyingContainer,
        )
        val view = NadelResultView.root(data)
        val occurrence = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("viewer", "aliasedUser")),
        ).single()

        underlyingContainer.remove("aliasedUser")
        data["renamedViewer"] = user
        data.remove("viewer")

        assertEquals(
            expected = listOf("renamedViewer"),
            actual = view.getResultPath(occurrence)?.toRawPath(),
        )
    }

    @Test
    fun `explicit scope removes its query prefix and retains its response prefix`() {
        val firstFriend = mutableMapOf<String, Any?>("id" to "one")
        val secondFriend = mutableMapOf<String, Any?>("id" to "two")
        val view = NadelResultView.scoped(
            data = mapOf(
                "friends" to listOf(firstFriend, secondFriend),
            ),
            scope = NadelResultScope(
                queryPrefix = NadelQueryPath(listOf("viewer")),
                responsePrefix = NadelResultPath(
                    listOf(
                        NadelResultPathSegment.Object("viewer"),
                    ),
                ),
            ),
        )

        val occurrences = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("viewer", "friends")),
            flatten = true,
        )

        assertEquals(
            expected = listOf(
                listOf("viewer", "friends", 0),
                listOf("viewer", "friends", 1),
            ),
            actual = occurrences.map { occurrence ->
                view.getResultPath(occurrence)?.toRawPath()
            },
        )
        assertEquals(
            expected = emptyList(),
            actual = view.getNodeOccurrencesAt(
                queryPath = NadelQueryPath(listOf("anotherViewer", "friends")),
                flatten = true,
            ),
        )
    }

    @Test
    fun `defer scope retains list indices from its response anchor`() {
        val assignee = mutableMapOf<String, Any?>("id" to "one")
        val view = NadelResultView.deferred(
            data = mapOf(
                "assignee" to assignee,
            ),
            path = listOf("issues", 1),
        )

        val occurrence = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("issues", "assignee")),
        ).single()

        assertEquals(
            expected = listOf("issues", 1, "assignee"),
            actual = view.getResultPath(occurrence)?.toRawPath(),
        )
    }

    @Test
    fun `unaddressable scope does not invent a result path`() {
        val user = mutableMapOf<String, Any?>("id" to "one")
        val view = NadelResultView.unaddressable(
            data = mapOf(
                "users" to listOf(user),
            ),
        )
        val occurrence = view.getNodeOccurrencesAt(
            queryPath = NadelQueryPath(listOf("users")),
            flatten = true,
        ).single()

        assertNull(view.getResultPath(occurrence))
    }

    @Test
    fun `public JsonNodes factory and caching constructor retain value-only behavior`() {
        val data = mapOf(
            "friends" to listOf(
                mapOf("id" to "one"),
                mapOf("id" to "two"),
            ),
        )
        val queryPrefix = NadelQueryPath(listOf("viewer"))
        val queryPath = NadelQueryPath(listOf("viewer", "friends"))
        val factoryNodes = JsonNodes(data, pathPrefix = queryPrefix)
        val directlyConstructedNodes = NadelCachingJsonNodes(data, pathPrefix = queryPrefix)

        assertEquals(
            expected = directlyConstructedNodes.getNodesAt(queryPath, flatten = true),
            actual = factoryNodes.getNodesAt(queryPath, flatten = true),
        )
        assertEquals(
            expected = emptyList(),
            actual = factoryNodes.getNodesAt(
                queryPath = NadelQueryPath(listOf("anotherViewer", "friends")),
                flatten = true,
            ),
        )
    }
}
