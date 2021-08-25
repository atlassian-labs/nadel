package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service

data class NadelTypeRenameInstruction(
    val service: Service,
    val overallName: String,
    val underlyingName: String,
)
