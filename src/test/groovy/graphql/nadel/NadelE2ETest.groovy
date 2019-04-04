
package graphql.nadel

import graphql.ErrorType
import graphql.GraphQLError
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {

    def simpleNDSL = '''
         service MyService {
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
         }
        '''

    def simpleUnderlyingSchema = typeDefinitions('''
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
        ''')

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)

    def "query to one service with execution input passed down"() {

        given:
        def query = '''
        query OpName { hello {name} hello {id} }
        '''

        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceDataFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(["var1": "val1"])
                .context("contextObj")
                .operationName("OpName")
                .build()
        def data = [hello: [id: "3", name: "earth"]]

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "query nadel_2_MyService {hello {name} hello {id}}"
            assert params.context == "contextObj"
            assert params.operationDefinition.name == "nadel_2_MyService"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.join().data == data
    }

    def "graphql-java validation is invoked"() {
        given:
        def query = '''
        query OpName($unusedVariable : String) { hello {name} hello {id} }
        '''

        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceDataFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        !result.errors.isEmpty()
        def error = result.errors[0] as GraphQLError
        error.errorType == ErrorType.ValidationError

    }

    def "query to two services with field rename"() {

        def nsdl = '''
         service Foo {
            type Query{
                foo: Foo  <= \$source.fooOriginal 
            } 
            type Foo {
                name: String
            }
         }
         service Bar {
            type Query{
                bar: Bar 
            } 
            type Bar {
                name: String <= \$source.title
            }
         }
        '''
        def query = '''
        { otherFoo: foo {name} bar{name}}
        '''
        def underlyingSchema1 = typeDefinitions('''
            type Query{
                fooOriginal: Foo  
                
            } 
            type Foo {
                name: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query{
                bar: Bar 
            } 
            type Bar {
                title: String
            }
        ''')
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Foo: new Tuple2(delegatedExecution1, underlyingSchema1),
                Bar: new Tuple2(delegatedExecution2, underlyingSchema2)]
        )

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [otherFoo: [name: "Foo"]]
        def data2 = [bar: [title: "Bar"]]
        ServiceExecutionResult delegatedExecutionResult1 = new ServiceExecutionResult(data1)
        ServiceExecutionResult delegatedExecutionResult2 = new ServiceExecutionResult(data2)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.execute(_) >> completedFuture(delegatedExecutionResult2)
        result.join().data == [otherFoo: [name: "Foo"], bar: [name: "Bar"]]
    }

    def "query with three nested hydrations"() {

        def nsdl = '''
         service Foo {
            type Query{
                foo: Foo  
            } 
            type Foo {
                name: String
                bar: Bar <= \$innerQueries.Bar.barById(id: \$source.barId)
            }
         }
         service Bar {
            type Query{
                bar: Bar 
            } 
            type Bar {
                name: String 
                nestedBar: Bar <= \$innerQueries.Bar.barById(id: \$source.nestedBarId)
            }
         }
        '''
        def underlyingSchema1 = typeDefinitions('''
            type Query{
                foo: Foo  
            } 
            type Foo {
                name: String
                barId: ID
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query{
                bar: Bar 
                barById(id: ID): Bar
            } 
            type Bar {
                id: ID
                name: String
                nestedBarId: ID
            }
        ''')

        def query = '''
            { foo { bar { name nestedBar {name nestedBar { name } } } } }
        '''
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Foo: new Tuple2(serviceExecution1, underlyingSchema1),
                Bar: new Tuple2(serviceExecution2, underlyingSchema2)]
        )
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()

        def topLevelData = [foo: [barId: "barId123"]]
        def hydrationData1 = [barById: [name: "BarName", nestedBarId: "nestedBarId123"]]
        def hydrationData2 = [barById: [name: "NestedBarName1", nestedBarId: "nestedBarId456"]]
        def hydrationData3 = [barById: [name: "NestedBarName2"]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult1 = new ServiceExecutionResult(hydrationData1)
        ServiceExecutionResult hydrationResult2 = new ServiceExecutionResult(hydrationData2)
        ServiceExecutionResult hydrationResult3 = new ServiceExecutionResult(hydrationData3)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >>

                completedFuture(topLevelResult)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult1)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult2)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult3)

        result.join().data == [foo: [bar: [name: "BarName", nestedBar: [name: "NestedBarName1", nestedBar: [name
                                                                                                            : "NestedBarName2"]]]]]
    }

    def 'mutation can be executed'() {

        def query = '''
        mutation M{ hello }
        '''

        given:
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceDataFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: "world"]
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "mutation nadel_2_MyService {hello}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.join().data == data
    }

}
