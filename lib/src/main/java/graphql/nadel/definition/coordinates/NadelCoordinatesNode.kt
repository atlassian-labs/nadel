package graphql.nadel.definition.coordinates

data class NadelCoordinatesNode internal constructor(
    val kind: NadelCoordinateKind,
    val name: String,
    val children: List<NadelCoordinatesNode>,
)
