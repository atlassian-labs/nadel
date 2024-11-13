package graphql.nadel.util

/**
 * Very similar to [String.split] but the [splitFunction] determines when to split the input.
 */
internal fun <E> Sequence<E>.splitBy(
    splitFunction: (E) -> Boolean,
): Sequence<List<E>> {
    return sequence {
        var currentSplit = mutableListOf<E>()

        for (e in this@splitBy) {
            if (splitFunction(e)) {
                yield(currentSplit)
                currentSplit = mutableListOf()
            } else {
                currentSplit.add(e)
            }
        }

        yield(currentSplit)
    }
}

/**
 * Very similar to [String.split] but the [splitFunction] determines when to split the input.
 */
internal fun <E> Iterable<E>.splitBy(
    splitFunction: (E) -> Boolean,
): List<List<E>> {
    val splits = mutableListOf<List<E>>()
    var currentSplit = mutableListOf<E>()

    for (e in this) {
        if (splitFunction(e)) {
            splits.add(currentSplit)
            currentSplit = mutableListOf()
        } else {
            currentSplit.add(e)
        }
    }

    splits.add(currentSplit)

    return splits
}
