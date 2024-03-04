package graphql.nadel
open class NadelServiceExecutionResultImpl(
    data: MutableMap<String, Any?> = LinkedHashMap(),
    errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    extensions: MutableMap<String, Any?> = LinkedHashMap(),
) : ServiceExecutionResult(data, errors, extensions)
