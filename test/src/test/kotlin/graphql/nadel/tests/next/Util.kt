package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.kotlinpoet.CodeBlock
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.TypeRuntimeWiring
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toCollection
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun listOfJsonStrings(
    @Language("JSON")
    vararg values: String,
): List<String> {
    return listOf(*values)
}

fun <T : Any> KClass<T>.asTestName(): String {
    return simpleName!!
        .removeSuffix("Test")
        .fold(StringBuilder()) { acc, value ->
            if (value.isUpperCase()) {
                acc.append(' ').append(value.lowercase())
            } else {
                acc.append(' ')
            }
            acc
        }
        .trim()
        .toString()
        .replaceFirstChar {
            it.uppercase()
        }
}

fun TypeRuntimeWiring.Builder.jsonDataFetcher(
    field: String,
    dataFetcher: (DataFetchingEnvironment) -> SerializedJsonValue,
): TypeRuntimeWiring.Builder {
    return dataFetcher(field) { env ->
        when (val serializedValue = dataFetcher(env)) {
            is SerializedJsonValue.JsonObject -> jsonObjectMapper.readValue<JsonMap>(serializedValue.serialized)
            is SerializedJsonValue.JsonArray -> jsonObjectMapper.readValue<AnyList>(serializedValue.serialized)
        }
    }
}

sealed interface SerializedJsonValue {
    val serialized: String

    data class JsonObject(
        @Language("JSON")
        override val serialized: String,
    ) : SerializedJsonValue

    data class JsonArray(
        @Language("JSON")
        override val serialized: String,
    ) : SerializedJsonValue
}

suspend fun <T> Flow<T>.toMutableList(): MutableList<T> {
    return toCollection(mutableListOf())
}

inline fun <E> MutableList<E>.forEachElementInIterator(onEach: (MutableIterator<E>, E) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        onEach(iterator, iterator.next())
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> KClass<*>.asSubclassOfOrNull(): KClass<T>? {
    return if (isSubclassOf(T::class)) {
        this as KClass<T>
    } else {
        null
    }
}

inline fun CodeBlock.Builder.indented(function: CodeBlock.Builder.() -> Unit) {
    indent()
    try {
        function()
    } finally {
        unindent()
    }
}
