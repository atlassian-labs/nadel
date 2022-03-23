package graphql.nadel.validation

internal class NadelUnionValidation(
    private val typeValidation: NadelTypeValidation,
) {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return emptyList()

        // return if (schemaElement.overall is GraphQLUnionType && schemaElement.underlying is GraphQLUnionType) {
        //     schemaElement.overall.types
        //         .flatMap { unionOverallType ->
        //             val unionUnderlyingType = NadelSchemaUtil.getUnderlyingType(unionOverallType, schemaElement.service)
        //
        //             if (!schemaElement.underlying.types.contains(unionUnderlyingType)) {
        //                 listOf(
        //                     UnionHasExtraType(
        //                         service = schemaElement.service,
        //                         unionType = schemaElement.overall,
        //                         extraType = unionOverallType as GraphQLObjectType,
        //                     ),
        //                 )
        //             } else if (unionUnderlyingType == null) {
        //                 listOf(
        //                     MissingUnderlyingType(
        //                         service = schemaElement.service,
        //                         overallType = unionOverallType,
        //                     ),
        //                 )
        //             } else {
        //                 typeValidation.validate(
        //                     NadelServiceSchemaElement(
        //                         service = schemaElement.service,
        //                         overall = unionOverallType,
        //                         underlying = unionUnderlyingType,
        //                     ),
        //                 )
        //             }
        //         }
        // } else {
        //     emptyList()
        // }
    }
}
