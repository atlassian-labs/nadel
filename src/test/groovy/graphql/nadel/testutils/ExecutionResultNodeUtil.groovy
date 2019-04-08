package graphql.nadel.testutils

import graphql.execution.nextgen.FetchedValueAnalysis
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.execution.nextgen.result.LeafExecutionResultNode
import graphql.execution.nextgen.result.ListExecutionResultNode
import graphql.execution.nextgen.result.ObjectExecutionResultNode
import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.execution.nextgen.result.RootExecutionResultNode

import static graphql.Scalars.GraphQLString
import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo
import static graphql.execution.FetchedValue.newFetchedValue
import static graphql.execution.MergedField.newMergedField
import static graphql.execution.nextgen.FetchedValueAnalysis.newFetchedValueAnalysis
import static graphql.language.Field.newField

/**
 * A helper for tests around {@link graphql.execution.nextgen.result.ExecutionResultNode}s
 *
 * The convention is that the field name is the field value with "Val" appended
 */
class ExecutionResultNodeUtil {


    static FetchedValueAnalysis fva(String fieldName) {
        fva(fieldName, null)
    }

    static FetchedValueAnalysis fva(String fieldName, String alias) {
        def field = newMergedField(newField(fieldName).alias(alias).build()).build()
        def info = newExecutionStepInfo().type(GraphQLString).field(field).build()
        def value = fieldName + "Val"
        def fetchedValue = newFetchedValue().fetchedValue(value).build()
        return newFetchedValueAnalysis()
                .executionStepInfo(info)
                .fetchedValue(fetchedValue)
                .completedValue(value)
                .valueType(FetchedValueAnalysis.FetchedValueType.SCALAR)
                .build()
    }

    static LeafExecutionResultNode leaf(String name, String alias) {
        new LeafExecutionResultNode(fva(name, alias), null)
    }

    static LeafExecutionResultNode leaf(String name) {
        new LeafExecutionResultNode(fva(name), null)
    }

    static ObjectExecutionResultNode object(String name, List<ExecutionResultNode> children) {
        new ObjectExecutionResultNode(fva(name), children)
    }

    static ListExecutionResultNode list(String name, List<ExecutionResultNode> children) {
        new ListExecutionResultNode(fva(name), children)
    }

    static RootExecutionResultNode root(List<ExecutionResultNode> children) {
        new RootExecutionResultNode(children)
    }

    static Map toData(ExecutionResultNode resultNode) {
        def executionResult = ResultNodesUtil.toExecutionResult(resultNode)
        def specification = executionResult.toSpecification()
        return specification["data"]
    }

}
