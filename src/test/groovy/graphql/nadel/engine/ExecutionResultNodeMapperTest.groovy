package graphql.nadel.engine


import spock.lang.Specification

class ExecutionResultNodeMapperTest extends Specification {


//    def "maps list"() {
//        given:
//        def rootExecutionStepInfo = esi("")
//        def schema = TestUtil.schema("type Query { foo: [String] }")
//        def overallSchema = TestUtil.schema("type Query { foo: [String] }")
//        GraphQLObjectType queryType = schema.getType("Query")
//        def fooField = queryType.getFieldDefinition("foo")
//
//        def parentInfo = StrategyUtil.createRootExecutionStepInfo(schema, Operation.QUERY)
//
//        MergedField mergedField = MergedField.newMergedField(new Field("foo")).build()
//        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
//                .fieldDefinition(fooField)
//                .type(fooField.getType())
//                .field(mergedField)
//                .fieldContainer(queryType)
//                .parentInfo(parentInfo)
//                .path(ExecutionPath.parse("/foo"))
//                .build()
//
//        ExecutionResultNodeMapper mapper = new ExecutionResultNodeMapper()
//        UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(rootExecutionStepInfo, false, false, [:], overallSchema)
//        when:
//        def mappedInfo = mapper.mapERNFromUnderlyingToOverall(executionStepInfo, unapplyEnvironment)
//
//
//        then:
//        mappedInfo.getFieldContainer() == overallSchema.getType("Query")
//        GraphQLTypeUtil.simplePrint(mappedInfo.getType()) == "[String]"
//    }
//
//    def "maps wrapped types"() {
//        given:
//        def rootERN = RootExecutionResultNode.newRootExecutionResultNode().build()
//        def schema = TestUtil.schema("type Query { foo: [String!]! }")
//        def overallSchema = TestUtil.schema("type Query { foo: [String] }")
//        GraphQLObjectType queryType = schema.getType("Query")
//        def fooField = queryType.getFieldDefinition("foo")
//
//        def parentInfo = StrategyUtil.createRootExecutionStepInfo(schema, Operation.QUERY)
//
//        MergedField mergedField = MergedField.newMergedField(new Field("foo")).build()
//        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
//                .fieldDefinition(fooField)
//                .type(fooField.getType())
//                .field(mergedField)
//                .fieldContainer(queryType)
//                .parentInfo(parentInfo)
//                .path(ExecutionPath.parse("/foo"))
//                .build()
//
//        ExecutionResultNodeMapper mapper = new ExecutionResultNodeMapper()
//        UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(rootERN, false, false, [:], overallSchema)
//        when:
//        def mappedInfo = mapper.mapERNFromUnderlyingToOverall(executionStepInfo, unapplyEnvironment)
//
//
//        then:
//        mappedInfo.getFieldContainer() == overallSchema.getType("Query")
//        GraphQLTypeUtil.simplePrint(mappedInfo.getType()) == "[String!]!"
//    }

}
