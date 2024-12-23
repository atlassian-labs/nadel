package graphql.nadel.util

import graphql.schema.FieldCoordinates

/**
 * Stores values that belong to a particular field.
 *
 * Refer to one of the constructor methods [groupBy] or [from]
 */
class NadelFieldMap<T> private constructor(
    private val map: Map<Int, T>,
) {
    operator fun get(fieldCoordinates: FieldCoordinates): T? {
        return get(fieldCoordinates.typeName, fieldCoordinates.fieldName)
    }

    fun get(objectTypeName: String, fieldName: String): T? {
        return map[hashCode(objectTypeName, fieldName)]
    }

    val size: Int = map.size

    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return map.isNotEmpty()
    }

    companion object {
        private val emptyMap = NadelFieldMap<Any>(kotlin.collections.emptyMap())

        fun <T> emptyMap(): NadelFieldMap<T> {
            @Suppress("UNCHECKED_CAST")
            return emptyMap as NadelFieldMap<T>
        }

        internal inline fun <T> from(
            values: Iterable<T>,
            getCoordinates: (T) -> FieldCoordinates,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getCoordinates = getCoordinates,
                getValue = {
                    it
                },
            )
        }

        @JvmName("fromFieldCoordinates")
        internal inline fun <E, T> from(
            values: Iterable<E>,
            getCoordinates: (E) -> FieldCoordinates,
            getValue: (E) -> T,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getTypeName = {
                    getCoordinates(it).typeName
                },
                getFieldName = {
                    getCoordinates(it).fieldName
                },
                getValue = getValue,
            )
        }

        internal inline fun <T> from(
            values: Iterable<T>,
            getTypeName: (T) -> String,
            getFieldName: (T) -> String,
        ): NadelFieldMap<T> {
            return from(
                values = values,
                getTypeName = getTypeName,
                getFieldName = getFieldName,
                getValue = {
                    it
                },
            )
        }

        internal inline fun <E, T> from(
            values: Iterable<E>,
            getTypeName: (E) -> String,
            getFieldName: (E) -> String,
            getValue: (E) -> T,
        ): NadelFieldMap<T> {
            val map: MutableMap<Int, T> = mutableMapOf()
            values.forEach { value ->
                val typeName = getTypeName(value)
                val fieldName = getFieldName(value)
                map[hashCode(typeName, fieldName)] = getValue(value)
            }
            return NadelFieldMap(map)
        }

        internal inline fun <T> groupBy(
            values: List<T>,
            getCoordinates: (T) -> FieldCoordinates,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getCoordinates = getCoordinates,
                getValue = {
                    it
                },
            )
        }

        @JvmName("groupByFieldCoordinates")
        internal inline fun <E, T> groupBy(
            values: List<E>,
            getCoordinates: (E) -> FieldCoordinates,
            getValue: (E) -> T,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getTypeName = {
                    getCoordinates(it).typeName
                },
                getFieldName = {
                    getCoordinates(it).fieldName
                },
                getValue = getValue,
            )
        }

        internal inline fun <T> groupBy(
            values: List<T>,
            getTypeName: (T) -> String,
            getFieldName: (T) -> String,
        ): NadelFieldMap<List<T>> {
            return groupBy(
                values = values,
                getTypeName = getTypeName,
                getFieldName = getFieldName,
                getValue = {
                    it
                },
            )
        }

        internal inline fun <E, T> groupBy(
            values: List<E>,
            getTypeName: (E) -> String,
            getFieldName: (E) -> String,
            getValue: (E) -> T,
        ): NadelFieldMap<List<T>> {
            val map: MutableMap<Int, MutableList<T>> = mutableMapOf()
            values.forEach { value ->
                val typeName = getTypeName(value)
                val fieldName = getFieldName(value)
                map
                    .computeIfAbsent(hashCode(typeName, fieldName)) {
                        mutableListOf()
                    }
                    .add(getValue(value))
            }
            return NadelFieldMap(map)
        }

        private fun hashCode(typeName: String, fieldName: String): Int {
            var result = 1
            result = 31 * result + typeName.hashCode()
            result = 31 * result + fieldName.hashCode()
            return result
        }
    }
}
