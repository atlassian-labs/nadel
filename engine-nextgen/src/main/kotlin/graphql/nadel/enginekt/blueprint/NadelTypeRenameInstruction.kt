package graphql.nadel.enginekt.blueprint

data class NadelTypeRenameInstruction(
    val overallName: String,
    val underlyingName: String,
)
