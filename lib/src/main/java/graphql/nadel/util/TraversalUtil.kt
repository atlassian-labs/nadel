package graphql.nadel.util

internal data class NadelDfsNode<T : Any>(
    val parent: NadelDfsNode<T>? = null,
    val children: List<T>,
    var index: Int = children.lastIndex,
)

internal inline fun <T : Any> dfs(
    root: T,
    getChildren: (T) -> List<T>,
    consumer: (T) -> Unit,
) {
    var cursor: NadelDfsNode<T>? = NadelDfsNode(
        children = listOf(root),
    )

    while (cursor != null) {
        val element = cursor.children[cursor.index--]
        consumer(element)

        // Pop
        if (cursor.index < 0) {
            cursor = cursor.parent
        }

        val children = getChildren(element)
        if (children.isNotEmpty()) {
            cursor = NadelDfsNode(
                parent = cursor,
                children = children,
            )
        }
    }
}
