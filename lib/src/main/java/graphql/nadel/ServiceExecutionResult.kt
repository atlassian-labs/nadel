package graphql.nadel

open class ServiceExecutionResult @JvmOverloads constructor(
    val data: MutableMap<String, Any?> = LinkedHashMap(),
    val errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    val extensions: MutableMap<String, Any?> = LinkedHashMap(),
)
