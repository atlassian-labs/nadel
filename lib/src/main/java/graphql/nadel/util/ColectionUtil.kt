package graphql.nadel.util

inline fun <E, T> Collection<E>.mapToSet(transform: (E) -> T): Set<T> {
    return mapTo(LinkedHashSet(size), transform)
}
