package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelCoerceTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeUtil

/**
 * This transform ensures that scalar values are coerced according to the scalar type used in the overall schema.
 *
 * In all ordinary cases this coercion is not necessary, because the value returned by the underlying service
 * will already have been coerced to the type used in the overall schema.
 *
 * There is however, one edge case, which is when there's a conflict between the scalar type used in the overall schema
 * and the scalar value returned by the underlying service.
 *
 * Example:
 * ```
 * # overall schema
 * type Dog {
 *   age: Int
 * }
 * ```
 *
 * ```
 * # original service schema
 * type Dog {
 *   age: String
 * }
 * ```
 * In this case the underlying service would return `age` as a String:
 *
 * ```
 * {
 *   "data": {
 *     "dog": {
 *       "age": "10"
 *     }
 *   }
 * }
 * ```
 * This transform ensures that the value of `age` is coerced according to the `Int` scalar, which is the type used in
 * the overall schema. The result data would look like this:
 * ```
 * {
 *   "data": {
 *     "dog": {
 *       "age": 10
 *     }
 *   }
 * }
 * ```
 *
 * This transformer replicates the exact same behaviour as the current gen.
 */
internal class NadelCoerceTransform : NadelTransform<State> {
    data class State(
        val fieldType: GraphQLScalarType,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField
    ): State? {
        val schema = executionBlueprint.schema

        val unwrappedTypes = overallField.objectTypeNames
            .asSequence()
            .mapNotNull {
                schema.getFieldDefinition(
                    FieldCoordinates.coordinates(
                        it,
                        overallField.fieldName
                    )
                )
            }
            .map { it.type }
            .map { GraphQLTypeUtil.unwrapAll(it) }
            .toList()

        val distinctTypes = unwrappedTypes.distinct().count()

        // In the case of scalars, there should only be 1 unwrapped type.
        // Object types could result in more than 1 distinct type, in the case of different interface implementations
        // having different concrete types, but this transform only cares about scalar types.
        if (distinctTypes != 1) {
            return null
        }

        val type = unwrappedTypes.first()

        if (GraphQLTypeUtil.isScalar(type)) {
            return State(type as GraphQLScalarType)
        }

        return null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State
    ): List<NadelResultInstruction> {
        return NadelTransformUtil.createSetInstructions(
            underlyingParentField,
            result,
            overallField
        ) { value ->
            state.fieldType.coercing.parseValue(value)
        }
    }
}