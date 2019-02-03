package graphql.nadel.engine

import graphql.execution.nextgen.FetchedValueAnalysis
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.execution.nextgen.result.LeafExecutionResultNode
import graphql.execution.nextgen.result.RootExecutionResultNode
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TraverserVisitor
import graphql.util.TraverserVisitorStub
import graphql.util.TreeTransformerUtil
import spock.lang.Ignore
import spock.lang.Specification

class ResultNodesTransformerTest extends Specification {

    // doesn't work at the moment in one traversal this way
    @Ignore
    def "change root node on leave with previously changed children"() {
        given:
        def fetchedValueAnalysis = Mock(FetchedValueAnalysis)
        LeafExecutionResultNode leafExecutionResultNode1 = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        LeafExecutionResultNode leafExecutionResultNode2 = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        RootExecutionResultNode rootExecutionResultNode = new RootExecutionResultNode([a: leafExecutionResultNode1, b: leafExecutionResultNode2])

        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer()

        when:
        def newRoot = resultNodesTransformer.transform(rootExecutionResultNode, new TraverserVisitor<ExecutionResultNode>() {
            @Override
            TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof LeafExecutionResultNode) {
                    LeafExecutionResultNode newNode = new LeafExecutionResultNode(fetchedValueAnalysis, null)
                    return TreeTransformerUtil.changeNode(context, newNode)
                }
                return TraversalControl.CONTINUE
            }

            @Override
            TraversalControl leave(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof RootExecutionResultNode) {
                    def childrenContexts = context.getChildrenContexts()
                    def changedChild1 = childrenContexts["a"][0].thisNode()
                    def changedChild2 = childrenContexts["b"][0].thisNode()
                    RootExecutionResultNode newNode = new RootExecutionResultNode([x: changedChild1, y: changedChild2])
                    return TreeTransformerUtil.changeNode(context, newNode)
                }
                return TraversalControl.CONTINUE
            }
        })

        then:
        newRoot != null

    }

    def "change root node with previously changed children in two traversals"() {
        given:
        def fetchedValueAnalysis = Mock(FetchedValueAnalysis)
        LeafExecutionResultNode leafExecutionResultNode1 = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        LeafExecutionResultNode leafExecutionResultNode1Changed = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        LeafExecutionResultNode leafExecutionResultNode2 = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        LeafExecutionResultNode leafExecutionResultNode2Changed = new LeafExecutionResultNode(fetchedValueAnalysis, null)
        RootExecutionResultNode rootExecutionResultNode = new RootExecutionResultNode([a: leafExecutionResultNode1, b: leafExecutionResultNode2])

        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer()

        when:
        def newRoot = resultNodesTransformer.transform(rootExecutionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof LeafExecutionResultNode) {
                    LeafExecutionResultNode changedNode = context.thisNode() == leafExecutionResultNode1 ? leafExecutionResultNode1Changed : leafExecutionResultNode2Changed
                    return TreeTransformerUtil.changeNode(context, changedNode)
                }
                return TraversalControl.CONTINUE
            }

        })
        newRoot = resultNodesTransformer.transform(newRoot, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof RootExecutionResultNode) {
                    def childrenContexts = context.thisNode().getNamedChildren()
                    def changedChild1 = childrenContexts["a"][0]
                    def changedChild2 = childrenContexts["b"][0]
                    RootExecutionResultNode newNode = new RootExecutionResultNode([x: changedChild1, y: changedChild2])
                    return TreeTransformerUtil.changeNode(context, newNode)
                }
                return TraversalControl.CONTINUE
            }
        })


        then:
        newRoot.getNamedChildren()["x"][0] == leafExecutionResultNode1Changed
        newRoot.getNamedChildren()["y"][0] == leafExecutionResultNode2Changed

    }
}
