package graphql.nadel.engine.transform.partition

data class NadelFieldPartitionContext(
    val pathToPartitionArg: List<String>,
    val userContext: Any? = null,
) 
