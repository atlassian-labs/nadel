package graphql.nadel.validation.util

import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.nadel.enginekt.util.AnyNamedNode
import graphql.nadel.schema.NadelDirectives.DYNAMIC_SERVICE_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.HIDDEN_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.HYDRATED_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.HYDRATED_FROM_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.NADEL_HYDRATION_ARGUMENT_DEFINITION
import graphql.nadel.schema.NadelDirectives.NADEL_HYDRATION_COMPLEX_IDENTIFIED_BY
import graphql.nadel.schema.NadelDirectives.NADEL_HYDRATION_FROM_ARGUMENT_DEFINITION
import graphql.nadel.schema.NadelDirectives.NADEL_HYDRATION_TEMPLATE_ENUM_DEFINITION
import graphql.nadel.schema.NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION
import graphql.nadel.schema.NadelDirectives.RENAMED_DIRECTIVE_DEFINITION

object NadelBuiltInTypes {
    val builtInScalars = setOf(
        GraphQLInt,
        GraphQLFloat,
        GraphQLString,
        GraphQLBoolean,
        GraphQLID,
    )

    val builtInScalarNames = builtInScalars
        .asSequence()
        .map { it.name }
        .toSet()

    val builtInDirectiveSyntaxTypeNames = sequenceOf<AnyNamedNode>(
        RENAMED_DIRECTIVE_DEFINITION,
        HYDRATED_DIRECTIVE_DEFINITION,
        NADEL_HYDRATION_ARGUMENT_DEFINITION,
        DYNAMIC_SERVICE_DIRECTIVE_DEFINITION,
        NAMESPACED_DIRECTIVE_DEFINITION,
        HIDDEN_DIRECTIVE_DEFINITION,

        NADEL_HYDRATION_FROM_ARGUMENT_DEFINITION,
        NADEL_HYDRATION_COMPLEX_IDENTIFIED_BY,
        NADEL_HYDRATION_TEMPLATE_ENUM_DEFINITION,
        HYDRATED_FROM_DIRECTIVE_DEFINITION,
        HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION,

        ).map {
        it.name
    }.toSet()

    val allNadelBuiltInTypeNames = builtInScalarNames + builtInDirectiveSyntaxTypeNames
}
