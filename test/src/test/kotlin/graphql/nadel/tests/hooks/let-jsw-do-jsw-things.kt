package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.GatewaySchemaWiringFactory
import graphql.nadel.tests.UseHook
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLScalarType

@UseHook
class `let-jsw-do-jsw-things` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .overallWiringFactory(GatewaySchemaWiringFactory())
            .underlyingWiringFactory(GatewaySchemaWiringFactory())
    }

    override fun isSchemaValid(errors: Set<NadelSchemaValidationError>): Boolean {
        if (errors.size == 4) {
            return errors.hasTypeMismatch {
                val schemaElement = it.schemaElement
                schemaElement.overall is GraphQLScalarType
                    && schemaElement.underlying is GraphQLEnumType
                    && schemaElement.overall.name == "C"
                    && schemaElement.underlying.name == "Y"
            } && errors.hasTypeMismatch {
                val schemaElement = it.schemaElement
                schemaElement.overall is GraphQLScalarType
                    && schemaElement.underlying is GraphQLEnumType
                    && schemaElement.overall.name == "D"
                    && schemaElement.underlying.name == "Y"
            } && errors.wasTypeDuplicated { error ->
                error.duplicates
                    .mapTo(mutableSetOf()) {
                        it.overall.name to it.underlying.name
                    } == setOf("B" to "X", "A" to "X")
            } && errors.wasTypeDuplicated { error ->
                error.duplicates
                    .mapTo(mutableSetOf()) {
                        it.overall.name to it.underlying.name
                    } == setOf("D" to "Y", "C" to "Y")
            }
        }

        return false
    }

    private fun Iterable<NadelSchemaValidationError>.wasTypeDuplicated(
        predicate: (DuplicatedUnderlyingType) -> Boolean,
    ): Boolean {
        return any {
            if (it is DuplicatedUnderlyingType) {
                predicate(it)
            } else {
                false
            }
        }
    }

    private fun Iterable<NadelSchemaValidationError>.hasTypeMismatch(
        predicate: (IncompatibleType) -> Boolean,
    ): Boolean {
        return any {
            if (it is IncompatibleType) {
                predicate(it)
            } else {
                false
            }
        }
    }
}
