package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.GatewaySchemaWiringFactory
import graphql.nadel.tests.UseHook
import graphql.nadel.validation.NadelSchemaValidationError

@UseHook
class `let-jsw-do-jsw-things` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .overallWiringFactory(GatewaySchemaWiringFactory())
            .underlyingWiringFactory(GatewaySchemaWiringFactory())
    }

    override fun isSchemaValid(errors: Set<NadelSchemaValidationError>): Boolean {
        return errors.mapTo(LinkedHashSet()) { it.message } == setOf(
            "Underlying type X was duplicated by types [A, B] in the service service",
            "Underlying type Y was duplicated by types [C, D] in the service service",
            "Overall type kind of GraphQLScalarType(name=C) in service service does not match underlying type kind GraphQLEnumType(name=Y)",
            "Overall type kind of GraphQLScalarType(name=D) in service service does not match underlying type kind GraphQLEnumType(name=Y)",
        )
    }
}
