package graphql.nadel.engine.testutils

import graphql.execution.ExecutionStepInfo
import graphql.execution.ResultPath
import graphql.execution.nextgen.result.ResolvedValue
import graphql.nadel.engine.result.ExecutionResultNode
import graphql.nadel.engine.result.LeafExecutionResultNode
import graphql.nadel.engine.result.ListExecutionResultNode
import graphql.nadel.engine.result.ObjectExecutionResultNode
import graphql.nadel.engine.result.ResultNodesUtil
import graphql.nadel.engine.result.RootExecutionResultNode
import graphql.schema.GraphQLFieldDefinition

import static graphql.Scalars.GraphQLString
import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo
import static graphql.execution.MergedField.newMergedField
import static graphql.language.Field.newField
import static graphql.nadel.engine.result.LeafExecutionResultNode.newLeafExecutionResultNode
import static graphql.nadel.engine.result.ListExecutionResultNode.newListExecutionResultNode
import static graphql.nadel.engine.result.ObjectExecutionResultNode.newObjectExecutionResultNode

/**
 * A helper for tests around {@link ExecutionResultNode}s
 *
 * The convention is that the field name is the field value with "Val" appended
 *
 * You can use paths instead of field names and the field name will be the last segment of path
 * eg "/a/b" will result in a field name "b" and value of "bVal"
 */
class ExecutionResultNodeUtil {

    static ExecutionStepInfo esi(String pathName) {
        return esi(pathName, null)
    }

    static ExecutionStepInfo esi(String pathName, String alias) {
        if (pathName == null || pathName.isAllWhitespace()) {
            return newExecutionStepInfo().type(GraphQLString).path(ResultPath.rootPath()).build()
        }
        if (!pathName.contains("/")) {
            pathName = "/" + pathName
        }
        def path = ResultPath.parse(pathName)
        def fieldName = path.getSegmentName()

        def field = newMergedField(newField(fieldName).alias(alias).build()).build()
        newExecutionStepInfo().type(GraphQLString).field(field).path(path).build()
    }

    static GraphQLFieldDefinition fieldDefinition(String name) {
        GraphQLFieldDefinition.newFieldDefinition()
                .type(GraphQLString)
                .name(name)
                .build()
    }


    static ResolvedValue resolvedValue(String value) {
        return ResolvedValue.newResolvedValue().completedValue(value).build();
    }

    static LeafExecutionResultNode leaf(String name, String alias) {
        def info = esi(name, alias)
        newLeafExecutionResultNode().fieldDefinition(fieldDefinition(name)).alias(alias).resultPath(info.path).completedValue(name + "Val").build()
    }

    static LeafExecutionResultNode leaf(String name) {
        def info = esi(name)
        newLeafExecutionResultNode().fieldDefinition(fieldDefinition(name)).resultPath(info.path).completedValue(name + "Val").build()
    }

    static ObjectExecutionResultNode object(String name, List<ExecutionResultNode> children) {
        def info = esi(name)
        newObjectExecutionResultNode().fieldDefinition(fieldDefinition(name)).resultPath(info.path).completedValue(name + "Val").children(children).build()
    }

    static ListExecutionResultNode list(String name, List<ExecutionResultNode> children) {
        def info = esi(name)
        newListExecutionResultNode().fieldDefinition(fieldDefinition(name)).resultPath(info.path).completedValue(name + "Val").children(children).build()
    }

    static RootExecutionResultNode root(List<ExecutionResultNode> children) {
        RootExecutionResultNode.newRootExecutionResultNode().children(children).build()
    }

    static Map toData(ExecutionResultNode resultNode) {
        def executionResult = ResultNodesUtil.toExecutionResult(resultNode)
        def specification = executionResult.toSpecification()
        return specification["data"]
    }

}
