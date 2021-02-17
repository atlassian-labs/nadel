package graphql.nadel.engine


import graphql.GraphQLError
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.DefinitionRegistry
import graphql.nadel.ServiceExecution
import graphql.nadel.StrategyTestHelper
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.testutils.TestUtil

class NadelExecutionStrategyTest4 extends StrategyTestHelper {

    ExecutionHelper executionHelper
    def service1Execution
    def service2Execution
    def serviceDefinition
    def definitionRegistry
    def instrumentation
    def serviceExecutionHooks
    def resultComplexityAggregator

    void setup() {
        executionHelper = new ExecutionHelper()
        service1Execution = Mock(ServiceExecution)
        service2Execution = Mock(ServiceExecution)
        serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        definitionRegistry = Mock(DefinitionRegistry)
        instrumentation = new NadelInstrumentation() {}
        serviceExecutionHooks = new ServiceExecutionHooks() {}
        resultComplexityAggregator = new ResultComplexityAggregator()
    }


    def "renamed and hydrated query using same underlying source"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                fooz: Foo => renamed from foo
              } 
              type Foo {
                 field: String => renamed from issue.field
                 name: String => renamed from issue.name
                 id: ID => renamed from issue.fooId
                 detail: Detail => renamed from issue.detail
              }
              type Detail {
                 detailId: ID! => renamed from id
                 detailName: String => renamed from name
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Foo 
                detail(detailIds: [ID]): Detail
              } 
              type Foo {
                issue: Issue
              }
              
              type Issue {
                fooId: ID
                field: String
                detail: Detail
                name: String
              }
              type Detail {
                 id: ID!
                 name: String
              }
        """)
        def query = """{ fooz {field name id detail { detailId detailName}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {issue {field} issue {name} issue {fooId} issue {detail {id name}}}}"
        def response1 = [foo: [issue: [field: "field", name: "name", fooId:"ID", detail:[id:"detailId", name:"detailName"]]]]
//        def expectedQuery2 = "query nadel_2_Foo {detail(detailIds:\"ID\") {name}}"
//        def response2 = [detail: [name: "apple"]]
        def overallResponse = [fooz:[field:"field", name:"name", id:"ID", detail:[detailId:"detailId", detailName:"detailName"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithNHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["fooz"],
                [expectedQuery1],
                [response1],
                1,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }



}
