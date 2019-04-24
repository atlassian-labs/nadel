package graphql.nadel.engine

import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.nextgen.FetchedValueAnalysis
import graphql.execution.nextgen.result.LeafExecutionResultNode
import graphql.execution.nextgen.result.RootExecutionResultNode
import graphql.language.Field
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static graphql.nadel.testutils.ExecutionResultNodeUtil.esi

class ServiceResultNodesToOverallResultTest extends Specification {


    def "maps ResultNode with a field rename"() {
        given:
        def rootExecutionStepInfo = esi("")
        /*
        * This test maps a result tree with two children a,b to a result tree with x,y.
        * This simulates a field from a -> x and b -> y
         */
        def fetchedAnalysisMapper = Mock(FetchedAnalysisMapper)
        ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult()
        serviceResultNodesToOverallResult.fetchedAnalysisMapper = fetchedAnalysisMapper

        def originalAnalysis1 = Mock(FetchedValueAnalysis)
        def originalStepInfo1 = Mock(ExecutionStepInfo)
        originalAnalysis1.getExecutionStepInfo() >> originalStepInfo1
        originalStepInfo1.getField() >> MergedField.newMergedField(Field.newField("a").build()).build()

        def mappedAnalysis1 = Mock(FetchedValueAnalysis)
        def stepInfo1 = Mock(ExecutionStepInfo)
        1 * mappedAnalysis1.getExecutionStepInfo() >> stepInfo1
        1 * stepInfo1.getField() >> MergedField.newMergedField(Field.newField("x").build()).build()

        def originalAnalysis2 = Mock(FetchedValueAnalysis)
        def originalStepInfo2 = Mock(ExecutionStepInfo)
        originalAnalysis2.getExecutionStepInfo() >> originalStepInfo2
        originalStepInfo2.getField() >> MergedField.newMergedField(Field.newField("b").build()).build()

        def mappedAnalysis2 = Mock(FetchedValueAnalysis)
        def stepInfo2 = Mock(ExecutionStepInfo)
        1 * mappedAnalysis2.getExecutionStepInfo() >> stepInfo2
        1 * stepInfo2.getField() >> MergedField.newMergedField(Field.newField("y").build()).build()


        def leafResultNode1 = new LeafExecutionResultNode(originalAnalysis1, null)
        def leafResultNode2 = new LeafExecutionResultNode(originalAnalysis2, null)

        def rootResultNode = new RootExecutionResultNode([leafResultNode1, leafResultNode2])

        def overallSchema = Mock(GraphQLSchema)
        1 * fetchedAnalysisMapper.mapFetchedValueAnalysis(originalAnalysis1, overallSchema, rootExecutionStepInfo, [:]) >> mappedAnalysis1
        1 * fetchedAnalysisMapper.mapFetchedValueAnalysis(originalAnalysis2, overallSchema, rootExecutionStepInfo, [:]) >> mappedAnalysis2

        when:
        def newRoot = serviceResultNodesToOverallResult.convert(rootResultNode, overallSchema, rootExecutionStepInfo, [:])


        then:
        newRoot.getChildren().size() == 2
        newRoot.getChildren()[0] instanceof LeafExecutionResultNode
        newRoot.getChildren()[0].getMergedField().getResultKey() == "x"
        newRoot.getChildren()[1] instanceof LeafExecutionResultNode
        newRoot.getChildren()[1].getMergedField().getResultKey() == "y"

    }


}
