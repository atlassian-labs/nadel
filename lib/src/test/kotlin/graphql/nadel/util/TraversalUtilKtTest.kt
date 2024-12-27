package graphql.nadel.util

import kotlin.test.Test
import kotlin.test.assertTrue

class TraversalUtilKtTest {
    @Test
    fun `dfs traverses`() {
        data class Node(
            val label: String,
            val children: List<Node>,
        )

        val root = Node(
            label = "A",
            children = listOf(
                Node(
                    label = "B",
                    children = listOf(
                        Node(
                            label = "D",
                            children = listOf(

                            ),
                        ),
                        Node(
                            label = "E",
                            children = listOf(
                                Node(
                                    label = "F",
                                    children = listOf(
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Node(
                    label = "C",
                    children = listOf(

                    ),
                ),
            ),
        )

        val visited = mutableListOf<String>()

        dfs(
            root,
            getChildren = Node::children,
            consumer = {
                visited += it.label
            }
        )

        assertTrue(visited == listOf("A", "C", "B", "E", "F", "D"))
    }

    @Test
    fun `dfs empty children`() {
        data class Node(
            val label: String,
            val children: List<Node>,
        )

        val root = Node(
            label = "A",
            children = listOf(),
        )

        val visited = mutableListOf<String>()

        dfs(
            root,
            getChildren = Node::children,
            consumer = {
                visited += it.label
            }
        )

        assertTrue(visited == listOf("A"))
    }
}
