package graphql.nadel.enginekt.blueprint

data class GraphQLRenameInstruction(
        val overallName: String,
        val underlyingName: String,
)
