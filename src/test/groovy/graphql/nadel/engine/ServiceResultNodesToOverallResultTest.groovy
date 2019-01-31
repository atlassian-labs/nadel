package graphql.nadel.engine

import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.nextgen.FetchedValueAnalysis
import graphql.execution.nextgen.result.LeafExecutionResultNode
import graphql.execution.nextgen.result.RootExecutionResultNode
import graphql.language.Field
import graphql.schema.GraphQLSchema
import spock.lang.Specification

class ServiceResultNodesToOverallResultTest extends Specification {


    def "maps ResultNode with a field rename"() {
        given:
        /*
        * This test maps a result tree with two children a,b to a result tree with x,y.
        * This simulates a field from a -> x and b -> y
         */
        def fetchedAnalysisMapper = Mock(FetchedAnalysisMapper)
        ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult()
        serviceResultNodesToOverallResult.fetchedAnalysisMapper = fetchedAnalysisMapper

        def analysis1 = Mock(FetchedValueAnalysis)
        def mappedAnalysis1 = Mock(FetchedValueAnalysis)
        def stepInfo1 = Mock(ExecutionStepInfo)
        1 * mappedAnalysis1.getExecutionStepInfo() >> stepInfo1
        1 * stepInfo1.getField() >> MergedField.newMergedField(Field.newField("x").build()).build()

        def analysis2 = Mock(FetchedValueAnalysis)
        def mappedAnalysis2 = Mock(FetchedValueAnalysis)
        def stepInfo2 = Mock(ExecutionStepInfo)
        1 * mappedAnalysis2.getExecutionStepInfo() >> stepInfo2
        1 * stepInfo2.getField() >> MergedField.newMergedField(Field.newField("y").build()).build()


        def leafResultNode1 = new LeafExecutionResultNode(analysis1, null)
        def leafResultNode2 = new LeafExecutionResultNode(analysis2, null)

        def rootResultNode = new RootExecutionResultNode([a: leafResultNode1, b: leafResultNode2])

        def overallSchema = Mock(GraphQLSchema)
        1 * fetchedAnalysisMapper.mapFetchedValueAnalysis(analysis1, overallSchema, [:]) >> mappedAnalysis1
        1 * fetchedAnalysisMapper.mapFetchedValueAnalysis(analysis2, overallSchema, [:]) >> mappedAnalysis2

        when:
        def newRoot = serviceResultNodesToOverallResult.convert(rootResultNode, overallSchema, [:])


        then:
        newRoot.getChildrenMap().size() == 2
        newRoot.getChildrenMap()["x"] instanceof LeafExecutionResultNode
        newRoot.getChildrenMap()["y"] instanceof LeafExecutionResultNode

    }


}
