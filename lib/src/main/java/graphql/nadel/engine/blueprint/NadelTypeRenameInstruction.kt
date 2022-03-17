package graphql.nadel.engine.blueprint

data class NadelTypeRenameInstruction(
    val overallName: String,
    val underlyingName: String,
)
