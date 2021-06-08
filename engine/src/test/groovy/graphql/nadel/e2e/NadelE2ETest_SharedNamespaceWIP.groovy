package graphql.nadel.e2e

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.testutils.TestUtil
import spock.lang.Specification

import static graphql.nadel.NadelEngine.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.engine.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest_SharedNamespaceWIP extends Specification {

    def "query with two services sharing a namespaced field"() {
        def nsdl = [
                Issues     : '''
            service Issues {
              
              directive @namespaced on FIELD_DEFINITION
              
              type Query {
                issue: IssueQuery @namespaced
              }
              
              type IssueQuery {
                getIssue: Issue
              }
              
              type Issue {
                id: ID
                text: String
              }
            }
        ''',
                IssueSearch: '''
            service IssueSearch {
              
              extend type IssueQuery {
                search: SearchResult 
              }
              
              type SearchResult {
                id: ID
                count: Int
              }
            }
        ''']
        def underlyingSchema1 = typeDefinitions('''
            type Query {
              issue: IssueQuery
            }
            
            type IssueQuery {
              getIssue: Issue
            }
            
            type Issue {
              id: ID
              text: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query {
              issue: IssueQuery
            }
            
            type IssueQuery {
              search: SearchResult
            }
            
            type SearchResult {
              id: ID
              count: Int
            }  
        ''')
        def query = '''
            { 
              issue {
                getIssue {
                  text
                }
                
                search {
                  count
                }
              }
            }
        '''
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Issues     : new Tuple2(delegatedExecution1, underlyingSchema1),
                IssueSearch: new Tuple2(delegatedExecution2, underlyingSchema2)]
        )

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [issue: [getIssue: [text: "Foo"]]]
        def data2 = [issue: [search: [count: 1]]]
        ServiceExecutionResult delegatedExecutionResult1 = new ServiceExecutionResult(data1)
        ServiceExecutionResult delegatedExecutionResult2 = new ServiceExecutionResult(data2)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.execute(_) >> completedFuture(delegatedExecutionResult2)
        def er = result.join()
        er.data == [issue: [
                getIssue: [text: "Foo"],
                search  : [count: 1]
        ]]
    }

    def "query with two services sharing a namespaced field and a non namespaced top level field"() {
        def nsdl = [
                Issues     : '''
            service Issues {
              
              directive @namespaced on FIELD_DEFINITION
              
              type Query {
                issue: IssueQuery @namespaced
                conf: Page
              }
              
              type IssueQuery {
                getIssue: Issue
              }
              
              type Issue {
                id: ID
                text: String
              }
              
              type Page {
                id: ID
                title: String
              }
            }
        ''',
                IssueSearch: '''
            service IssueSearch {
              extend type IssueQuery {
                search: SearchResult 
              }
              
              type SearchResult {
                id: ID
                count: Int
              }
            }
        ''']
        def underlyingSchema1 = typeDefinitions('''
            type Query {
              issue: IssueQuery
              conf: Page
            }
            
            type Page {
              id: ID
              title: String
            }
            
            type IssueQuery {
              getIssue: Issue
            }
            
            type Issue {
              id: ID
              text: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query {
              issue: IssueQuery
            }
            
            type IssueQuery {
              search: SearchResult
            }
            
            type SearchResult {
              id: ID
              count: Int
            }
        ''')

        def query = '''
            { 
              issue {
                getIssue {
                  text
                }
                
                search {
                  count
                }
              }

              conf {
                title
              }
            }
        '''
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Issues     : new Tuple2(delegatedExecution1, underlyingSchema1),
                IssueSearch: new Tuple2(delegatedExecution2, underlyingSchema2)]
        )

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [issue: [getIssue: [text: "Foo"]]]
        def data2 = [issue: [search: [count: 1]]]
        def data3 = [conf: [title: "LOL"]]
        ServiceExecutionResult delegatedExecutionResult1 = new ServiceExecutionResult(data1)
        ServiceExecutionResult delegatedExecutionResult2 = new ServiceExecutionResult(data2)
        ServiceExecutionResult delegatedExecutionResult3 = new ServiceExecutionResult(data3)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.execute(_) >> completedFuture(delegatedExecutionResult2)
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult3)
        def er = result.join()
        er.data == [
                issue: [
                        getIssue: [text: "Foo"],
                        search  : [count: 1]],
                conf : [title: "LOL"]
        ]
    }

    def "query with 2 namespaced fields that have matching subfields"() {
        def nsdl = [
                Issues     : '''
            service Issues {
              
              directive @namespaced on FIELD_DEFINITION
              
              type Query {
                issue: IssueQuery @namespaced
              }
              
              type IssueQuery {
                getIssue: Issue
              }
              
              type Issue {
                id: ID
                text: String
              }
            }
        ''',
                IssueSearch: '''
            service IssueSearch {
              
              extend type IssueQuery {
                search: SearchResult 
              }
              
              type SearchResult {
                id: ID
                count: Int
              }
            }
        ''',
                Pages      : '''
            service Pages {
              type Query {
                page: PagesQuery @namespaced
              }
              
              type PagesQuery {
                getIssue: IssuePage
              }
              
              type IssuePage {
                id: ID
                pageText: String
              }
            }
        ''']
        def underlyingSchema1 = typeDefinitions('''
            type Query {
              issue: IssueQuery
            }
            
            type IssueQuery {
              getIssue: Issue
            }
            
            type Issue {
              id: ID
              text: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query {
              issue: IssueQuery
            }
            
            type IssueQuery {
              search: SearchResult
            }
            
            type SearchResult {
              id: ID
              count: Int
            }  
        ''')
        def underlyingSchema3 = typeDefinitions('''
            type Query {
              page: PageQuery
            }
            
            type PageQuery {
              getIssue: IssuePage
            }
            
            type IssuePage {
              id: ID
              pageText: String
            }
        ''')

        def query = '''
            { 
              issue {
                getIssue {
                  text
                }
                
                search {
                  count
                }
              }

              page {
                getIssue {
                  pageText
                }
              }
            }
        '''
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution3 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Issues     : new Tuple2(delegatedExecution1, underlyingSchema1),
                IssueSearch: new Tuple2(delegatedExecution2, underlyingSchema2),
                Pages      : new Tuple2(delegatedExecution3, underlyingSchema3)
        ])

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [issue: [getIssue: [text: "Foo"]]]
        def data2 = [issue: [search: [count: 1]]]
        def data3 = [page: [getIssue: [pageText: "Bar"]]]
        ServiceExecutionResult delegatedExecutionResult1 = new ServiceExecutionResult(data1)
        ServiceExecutionResult delegatedExecutionResult2 = new ServiceExecutionResult(data2)
        ServiceExecutionResult delegatedExecutionResult3 = new ServiceExecutionResult(data3)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.execute(_) >> completedFuture(delegatedExecutionResult2)
        1 * delegatedExecution3.execute(_) >> completedFuture(delegatedExecutionResult3)
        def er = result.join()
        er.data == [
                issue: [
                        getIssue: [text: "Foo"],
                        search  : [count: 1]
                ],
                page : [
                        getIssue: [pageText: "Bar"]
                ]
        ]
    }
}
