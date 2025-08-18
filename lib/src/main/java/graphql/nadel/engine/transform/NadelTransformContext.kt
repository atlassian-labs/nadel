package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.schema.GraphQLSchema

internal interface NadelTransformContext {
    val operationExecutionContext: NadelOperationExecutionContext
    val executionContext: NadelExecutionContext

    val userContext: Any?
        get() = executionContext.userContext

    val service: Service
        get() = operationExecutionContext.service

    val executionBlueprint: NadelOverallExecutionBlueprint
        get() = executionContext.executionBlueprint

    val engineSchema: GraphQLSchema
        get() = executionBlueprint.engineSchema
}
