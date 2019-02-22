package graphql.nadel.engine

import graphql.execution.ExecutionPath
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.language.Field
import graphql.nadel.TestUtil
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLTypeUtil
import spock.lang.Specification

class ExecutionStepInfoMapperTest extends Specification {


    def "maps list"() {
        given:
        def schema = TestUtil.schema("type Query { foo: [String] }")
        def overallSchema = TestUtil.schema("type Query { foo: [String] }")
        GraphQLObjectType queryType = schema.getType("Query")
        def fooField = queryType.getFieldDefinition("foo")

        def parentInfo = StrategyUtil.createRootExecutionStepInfo(schema)

        MergedField mergedField = MergedField.newMergedField(new Field("foo")).build()
        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .fieldDefinition(fooField)
                .type(fooField.getType())
                .field(mergedField)
                .fieldContainer(queryType)
                .parentInfo(parentInfo)
                .path(ExecutionPath.parse("/foo"))
                .build()

        ExecutionStepInfoMapper mapper = new ExecutionStepInfoMapper()
        when:
        def mappedInfo = mapper.mapExecutionStepInfo(executionStepInfo, overallSchema, [:])


        then:
        mappedInfo.getFieldContainer() == overallSchema.getType("Query")
        GraphQLTypeUtil.simplePrint(mappedInfo.getType()) == "[String]"
    }

    def "maps wrapped types"() {
        given:
        def schema = TestUtil.schema("type Query { foo: [String!]! }")
        def overallSchema = TestUtil.schema("type Query { foo: [String] }")
        GraphQLObjectType queryType = schema.getType("Query")
        def fooField = queryType.getFieldDefinition("foo")

        def parentInfo = StrategyUtil.createRootExecutionStepInfo(schema)

        MergedField mergedField = MergedField.newMergedField(new Field("foo")).build()
        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .fieldDefinition(fooField)
                .type(fooField.getType())
                .field(mergedField)
                .fieldContainer(queryType)
                .parentInfo(parentInfo)
                .path(ExecutionPath.parse("/foo"))
                .build()

        ExecutionStepInfoMapper mapper = new ExecutionStepInfoMapper()
        when:
        def mappedInfo = mapper.mapExecutionStepInfo(executionStepInfo, overallSchema, [:])


        then:
        mappedInfo.getFieldContainer() == overallSchema.getType("Query")
        GraphQLTypeUtil.simplePrint(mappedInfo.getType()) == "[String!]!"
    }

}
