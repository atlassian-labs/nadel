package graphql.nadel.enginekt.blueprint

data class NadelFieldRenameInstruction(
    val parentTypeName: String,
    val overallName: String,
    val underlyingName: String,
)
