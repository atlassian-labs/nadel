package graphql.nadel.engine

import graphql.AssertException
import graphql.GraphQLError
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.*
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultComplexityAggregator
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLSchema

import java.util.concurrent.ExecutionException

class Executionstrategytest4 extends StrategyTestHelper {

    ExecutionHelper executionHelper = new ExecutionHelper()
    def resultComplexityAggregator = new ResultComplexityAggregator()


    def "single source that returns a list for list type"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(fooId: $source.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(fooId: ID): [Foo]
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barId}}"
        def response1 = [foo: [barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(fooId:[\"ID\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name", object_identifier__UUID:"ID"], [name: "name2", object_identifier__UUID:"ID2"]]]
        def overallResponse = [foo: [cd: [name: "name"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }
    def "single source that returns an object for list type with list args"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(fooIds: $source.barIds) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(fooIds: [ID]): Foo
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds}}"
        def response1 = [foo: [barIds: ["ID", "ID2", "ID3"]]]
        def expectedQuery2 = "query nadel_2_Foo {doos(fooIds:\"ID\") {name}}"
        def response2 = [doos: [name: "name1"]]
        def expectedQuery3 = "query nadel_2_Foo {doos(fooIds:\"ID2\") {name}}"
        def response3 = [doos: [name: "name2"]]
        def expectedQuery4 = "query nadel_2_Foo {doos(fooIds:\"ID3\") {name}}"
        def response4 = [doos: [name: "name3"]]
        def responses = [response1, response2, response3, response4]
        def expectedQueries = [expectedQuery1, expectedQuery2, expectedQuery3, expectedQuery4]

        def overallResponse = [foo: [cd: [[name: "name1"],[name: "name2"],[name: "name3"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithNHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQueries,
                responses,
                4,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "single source that returns an object for list type with single arg"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(fooId: $source.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(fooIds: [ID]): Foo
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barId}}"
        def response1 = [foo: [barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(fooId:\"ID\") {name}}"
        def response2 = [doos: [name: "name2"]]
        def overallResponse = [foo: [cd: [name: "name2"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    def "1 list source with another single source that returns a list"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $primarySource.barIds, fooId: $source.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID], fooId: ID): [Foo]
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds extra_source_arg_barId:barId}}"
        def response1 = [foo: [barIds:["bar1", "bar2"], extra_source_arg_barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(ids:[\"bar1\",\"bar2\"],fooId:[\"ID\",\"ID\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "John", object_identifier__UUID:"bar1"], [name: "Don", object_identifier__UUID:"bar2"]]]
        def overallResponse = [foo: [cd: [[name: "John"], [name: "Don"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "single source with a list source that returns a list"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $source.barIds, fooId: $primarySource.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID], fooId: ID): [Foo]
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barId extra_source_arg_barIds:barIds}}"
        def response1 = [foo: [extra_source_arg_barIds:["bar1", "bar2"], barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(fooId:[\"ID\"],ids:[\"bar1\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "John", object_identifier__UUID:"ID"], [name: "Don", object_identifier__UUID:"ID"]]]
        def overallResponse = [foo: [cd: [[name: "John"], [name: "Don"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "single source that returns a list"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(fooId: $source.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(fooId: ID): [Foo]
              } 
              type Bar {
                barId: ID
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barId}}"
        def response1 = [foo: [barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(fooId:[\"ID\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "John", object_identifier__UUID:"ID"], [name: "Don", object_identifier__UUID:"ID"]]]
        def overallResponse = [foo: [cd: [name: "John"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    def "two single sources that returns a list object"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.doos(id: $primarySource.accountId, fooId: $source.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(id: ID, fooId: ID): [Foo]
              } 
              type Bar {
                barId: ID
                accountId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {accountId extra_source_arg_barId:barId}}"
        def response1 = [foo: [accountId:"a1", extra_source_arg_barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {doos(id:[\"a1\"],fooId:[\"ID\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name1", object_identifier__UUID:"a1"], [name: "name2", object_identifier__UUID:"a2"]]]
        def overallResponse = [foo: [cd: [name: "name1"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "1 list source that returns list object for list type with list input args"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $source.barIds) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID]): [Foo]
              } 
              type Bar {
                barId: [ID]
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds}}"
        def response1 = [foo: [barIds:["bar1", "bar2"]]]
        def expectedQuery2 = "query nadel_2_Foo {doos(ids:[\"bar1\",\"bar2\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name", object_identifier__UUID:"bar1"], [name: "name2", object_identifier__UUID:"bar2"]]]

        def overallResponse = [foo:[cd:[[name:"name"], [name:"name2"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "2 list sources that returns a list"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $primarySource.barIds, fooIds: $source.accountIds) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID], fooId: [ID]): [Foo]
              } 
              type Bar {
                accountIds: [ID]
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds extra_source_arg_accountIds:accountIds}}"
        def response1 = [foo: [barIds:["bar1", "bar2", "bar3"], extra_source_arg_accountIds: ["ID", "ID2", "ID3"]]]
        def expectedQuery2 = "query nadel_2_Foo {doos(ids:[\"bar1\",\"bar2\",\"bar3\"],fooIds:[\"ID\",\"ID2\",\"ID3\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name", object_identifier__UUID:"bar1"], [name: "name2", object_identifier__UUID:"bar2"], [name: "name3", object_identifier__UUID:"bar3"]]]

        def overallResponse = [foo:[cd:[[name:"name"], [name:"name2"], [name:"name3"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "2 list sources that returns a list but secondary source has null field"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $primarySource.barIds, fooIds: $source.accountIds) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID], fooId: [ID]): [Foo]
              } 
              type Bar {
                accountIds: [ID]
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds extra_source_arg_accountIds:accountIds}}"
        def response1 = [foo: [barIds:["bar1", "bar2", "bar3"], extra_source_arg_accountIds: ["ID", null, "ID3"]]]
        def expectedQuery2 = "query nadel_2_Foo {doos(ids:[\"bar1\",\"bar2\",\"bar3\"],fooIds:[\"ID\",null,\"ID3\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name", object_identifier__UUID:"bar1"], [name: "name3", object_identifier__UUID:"bar3"]]]

        def overallResponse = [foo:[cd:[[name:"name"], null, [name:"name3"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "2 list sources that returns a list but secondary source has less values"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                foo: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.doos(ids: $primarySource.barIds, fooIds: $source.accountIds) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                foo: Bar 
                doos(ids: [ID], fooId: [ID]): [Foo]
              } 
              type Bar {
                accountIds: [ID]
                barIds: [ID]
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ foo { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {foo {barIds extra_source_arg_accountIds:accountIds}}"
        def response1 = [foo: [barIds:["bar1", "bar2", "bar3"], extra_source_arg_accountIds: ["ID", "ID2"]]]
        def expectedQuery2 = "query nadel_2_Foo {doos(ids:[\"bar1\",\"bar2\",\"bar3\"],fooIds:[\"ID\",\"ID2\",null]) {name object_identifier__UUID:fooId}}"
        def response2 = [doos: [[name: "name", object_identifier__UUID:"bar1"], [name: "name2", object_identifier__UUID:"bar2"]]]

        def overallResponse = [foo:[cd:[[name:"name"], [name:"name2"], null]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["foo"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse

    }


    ExecutionHelper.ExecutionData createExecutionData(String query, GraphQLSchema overallSchema) {
        createExecutionData(query, [:], overallSchema)
    }

    def "two single sources that returns a single object"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                bar: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.foo(id: $primarySource.accountId, fooId: $source.barId)
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                bar: Bar 
                foo(id: ID, fooId: ID): Foo
              } 
              type Bar {
                barId: ID
                accountId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ bar { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {bar {accountId extra_source_arg_barId:barId}}"
        def response1 = [bar: [accountId:"a1", extra_source_arg_barId: "ID"]]
        def expectedQuery2 = "query nadel_2_Foo {foo(id:\"a1\",fooId:\"ID\") {name}}"
        def response2 = [foo: [name: "name1", object_identifier__UUID:"a1"]]
        def overallResponse = [bar: [cd: [name: "name1"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["bar"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "two single sources that returns a single object where secondary source is null"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                bar: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.foo(id: $primarySource.accountId, fooId: $source.barId)
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                bar: Bar 
                foo(id: ID, fooId: ID): Foo
              } 
              type Bar {
                barId: ID
                accountId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ bar { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {bar {accountId extra_source_arg_barId:barId}}"
        def response1 = [bar: [accountId:"a1", extra_source_arg_barId: null]]
        def expectedQuery2 = "query nadel_2_Foo {foo(id:\"a1\",fooId:null) {name}}"
        def response2 = [foo: [name: "name1"]]
        def overallResponse = [bar: [cd: [name: "name1"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["bar"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    def "three single sources that returns a single object"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                bar: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.foo(id: $primarySource.accountId, fooId: $source.barId, accountId: $source.thirdId)
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                bar: Bar 
                foo(id: ID, fooId: ID, accountId: ID): Foo
              } 
              type Bar {
                barId: ID
                accountId: ID
                thirdId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ bar { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {bar {accountId extra_source_arg_barId:barId extra_source_arg_thirdId:thirdId}}"
        def response1 = [bar: [accountId:"a1", extra_source_arg_barId: "bar1", extra_source_arg_thirdId: "zoo1"]]
        def expectedQuery2 = "query nadel_2_Foo {foo(id:\"a1\",fooId:\"bar1\",accountId:\"zoo1\") {name}}"
        def response2 = [foo: [name: "name1"]]
        def overallResponse = [bar: [cd: [name: "name1"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["bar"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    def "two single sources that returns a single object with nested id field"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                bar: Bar
              } 
              type Bar {
                 cd: Foo => hydrated from Foo.foo(id: $primarySource.account.accountId, fooId: $source.extendedBar.barId)
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                bar: Bar 
                foo(id: ID, fooId: ID): Foo
              } 
              type Bar {
                extendedBar: ExtendedBar
                account: Account
              }
              type ExtendedBar {
                barId: ID
              }
              type Account {
                accountId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ bar { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {bar {account {accountId} extra_source_arg_extendedBar:extendedBar {barId}}}"
        def response1 = [bar: [account:[accountId:"a1"], extra_source_arg_extendedBar: [barId:"bar1"]]]
        def expectedQuery2 = "query nadel_2_Foo {foo(id:\"a1\",fooId:\"bar1\") {name}}"
        def response2 = [foo: [name: "name1"]]
        def overallResponse = [bar: [cd: [name: "name1"]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["bar"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "two list sources that returns a list object with nested id fields"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Foo {
              type Query {
                bar: Bar
              } 
              type Bar {
                 cd: [Foo] => hydrated from Foo.foo(ids: $primarySource.accounts.accountId, fooIds: $source.extendedBars.barId) object identified by fooId
              }
              type Foo {
                 id: ID!
                 fooId: ID!
                 name: String
              }
        }
        ''')
        def underlyingSchema = TestUtil.schema("""
              type Query {
                bar: Bar 
                foo(ids: [ID], fooIds: [ID]): [Foo]
              } 
              type Bar {
                extendedBars: [ExtendedBar]
                accounts: [Account]
              }
              type ExtendedBar {
                barId: ID
              }
              type Account {
                accountId: ID
              }
              type Foo {
                 fooId: ID!
                 name: String
              }
        """)
        def query = """{ bar { cd { name}}}"""

        def expectedQuery1 = "query nadel_2_Foo {bar {accounts {accountId} extra_source_arg_extendedBars:extendedBars {barId}}}"
        def response1 = [bar: [accounts:[[accountId:"account1"], [accountId:"account2"]], extra_source_arg_extendedBars: [[barId:"bar1"], [barId:"bar2"]]]]
        def expectedQuery2 = "query nadel_2_Foo {foo(ids:[\"account1\",\"account2\"],fooIds:[\"bar1\",\"bar2\"]) {name object_identifier__UUID:fooId}}"
        def response2 = [foo: [[name: "name1", object_identifier__UUID:"account1"], [name: "name2", object_identifier__UUID:"account2"]]]
        def overallResponse = [bar:[cd:[[name:"name1"], [name:"name2"]]]]


        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1ServiceWithHydration(
                overallSchema,
                "Foo",
                underlyingSchema,
                query,
                ["bar"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                resultComplexityAggregator
        )
        then:
        errors.size() == 0
        response == overallResponse
    }
}