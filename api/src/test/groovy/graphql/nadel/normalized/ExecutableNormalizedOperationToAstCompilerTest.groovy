package graphql.nadel.normalized

import graphql.GraphQL
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.nadel.testutils.TestUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static graphql.language.OperationDefinition.Operation.MUTATION
import static graphql.language.OperationDefinition.Operation.QUERY
import static graphql.language.OperationDefinition.Operation.SUBSCRIPTION
import static graphql.nadel.normalized.ExecutableNormalizedOperationToAstCompiler.compileToDocument

class ExecutableNormalizedOperationToAstCompilerTest extends Specification {

    def "test pet interfaces"() {
        String sdl = """
        type Query { 
            animal: Animal
        }
        interface Animal {
            name: String
            friends: [Friend]
        }

        union Pet = Dog | Cat

        type Friend {
            name: String
            isBirdOwner: Boolean
            isCatOwner: Boolean
            pets: [Pet] 
        }

        type Bird implements Animal {
           name: String 
           friends: [Friend]
        }

        type Cat implements Animal {
           name: String 
           friends: [Friend]
           breed: String 
           mood: String 
        }

        type Dog implements Animal {
           name: String 
           breed: String
           friends: [Friend]
        }
        """

        String query = """
        {
            animal {
                name
                otherName: name
                ... on Animal {
                    name
                }
                ... on Cat {
                    name
                    mood
                    friends {
                        ... on Friend {
                            isCatOwner
                            pets {
                                ... on Dog {
                                    name
                                }
                            }
                        }
                    }
                }
                ... on Bird {
                    friends {
                        isBirdOwner
                    }
                    friends {
                        name
                        pets {
                            ... on Cat {
                                breed
                            }
                        }
                    }
                }
                ... on Dog {
                    name
                    breed
                }
            }
        }
        """
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        def printed = AstPrinter.printAst(document)
        then:
        printed == '''query {
  animal {
    ... on Bird {
      name
      otherName: name
      friends {
        isBirdOwner
        name
        pets {
          ... on Cat {
            breed
          }
        }
      }
    }
    ... on Cat {
      name
      otherName: name
      mood
      friends {
        isCatOwner
        pets {
          ... on Dog {
            name
          }
        }
      }
    }
    ... on Dog {
      name
      otherName: name
      breed
    }
  }
}
'''
    }

    def "test a combination of plain objects and interfaces"() {
        def sdl = '''
        type Query {
            foo(arg: I): Foo
        }
        type Foo {
            bar(arg: I): Bar
        }
        type Bar {
            baz : Baz
        }
        interface Baz {
            boo : String
        }
        type ABaz implements Baz {
            boo : String
            a : String
        }
        type BBaz implements Baz {
            boo : String
            b : String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''query {
    foo(arg: {arg1 : "fooArg"}) {
        bar(arg: {arg1 : "barArg"}) {
            baz {
                ... on ABaz {
                    boo
                    a
                }
            }
        }
    }
}
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo(arg: {arg1 : "fooArg"}) {
    bar(arg: {arg1 : "barArg"}) {
      baz {
        ... on ABaz {
          boo
          a
        }
      }
    }
  }
}
'''
    }

    def "test arguments"() {
        def sdl = '''
        type Query {
            foo1(arg: String): String
            foo2(a: Int, b: Boolean, c: Float): String
        }
        '''
        def query = ''' {
            foo1(arg: "hello")
            foo2(a: 123, b: true, c: 123.45)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo1(arg: "hello")
  foo2(a: 123, b: true, c: 123.45)
}
'''
    }

    def "sets operation name"() {
        def sdl = '''
        type Query {
            foo1(arg: String): String
            foo2(a: Int,b: Boolean, c: Float): String
        }
        '''
        def query = ''' {
            foo1(arg: "hello")
            foo2(a: 123, b: true, c: 123.45)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, "My_Op23", fields).first
        then:
        AstPrinter.printAst(document) == '''query My_Op23 {
  foo1(arg: "hello")
  foo2(a: 123, b: true, c: 123.45)
}
'''
    }

    def "test input object arguments"() {
        def sdl = '''
        type Query {
            foo1(arg: I): String
        }
        input I {
            arg1: String
            arg2: Int
            arg3: ID
            arg4: Boolean
            arg5: Float
            nested: I
        }
        '''
        def query = '''{
            foo1(arg: {
             arg1: "Hello"
             arg2: 123
             arg3: "IDID"
             arg4: false
             arg5: 123.123
             nested: {
                 arg1: "Hello2"
                 arg2: 1234
                 arg3: "IDID1"
                 arg4: null
                 arg5: null
             }
            })
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo1(arg: {arg1 : "Hello", arg2 : 123, arg3 : "IDID", arg4 : false, arg5 : 123.123, nested : {arg1 : "Hello2", arg2 : 1234, arg3 : "IDID1", arg4 : null, arg5 : null}})
}
'''
    }

    def "test mutations"() {
        def sdl = '''
        type Query {
            foo1(arg: I): String
        }
        type Mutation {
            foo1(arg: I): String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''mutation {
            foo1(arg: {
             arg1: "Mutation"
            })
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, MUTATION, null, fields).first
        then:
        AstPrinter.printAst(document) == '''mutation {
  foo1(arg: {arg1 : "Mutation"})
}
'''
    }

    def "test subscriptions"() {
        def sdl = '''
        type Query {
            foo1(arg: I): String
        }
        type Subscription {
            foo1(arg: I): String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''subscription {
            foo1(arg: {
             arg1: "Subscription"
            })
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, SUBSCRIPTION, null, fields).first
        then:
        AstPrinter.printAst(document) == '''subscription {
  foo1(arg: {arg1 : "Subscription"})
}
'''
    }

    def "test redundant inline fragments specified in original query"() {
        def sdl = '''
        type Query {
            foo1(arg: I): Foo 
        }
        type Mutation {
            foo1(arg: I): Foo 
        }
        type Foo {
            test: String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''mutation {
            ... on Mutation {
                foo1(arg: {
                    arg1: "Mutation"
                }) {
                    ... on Foo {
                        test
                    }
                }
            }
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, MUTATION, null, fields).first
        then:
        AstPrinter.printAst(document) == '''mutation {
  foo1(arg: {arg1 : "Mutation"}) {
    test
  }
}
'''
    }

    def "inserts inline fragment on interface types"() {
        def sdl = '''
        type Query {
            foo1(arg: I): Foo 
        }
        type Mutation {
            foo1(arg: I): Foo 
        }
        interface Foo {
            test: String
        }
        type AFoo implements Foo {
            test: String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''query {
            ... on Query {
                foo1(arg: {
                    arg1: "Query"
                }) {
                    ... on Foo {
                        test
                    }
                }
            }
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo1(arg: {arg1 : "Query"}) {
    ... on AFoo {
      test
    }
  }
}
'''
    }

    def "handles concrete and abstract fields"() {
        def sdl = '''
        type Query {
            foo1(arg: I): Foo 
        }
        type Mutation {
            foo1(arg: I): Foo 
        }
        interface Foo {
            test: String
        }
        type AFoo implements Foo {
            test: String
            afoo: String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''query {
            ... on Query {
                foo1(arg: {
                    arg1: "Query"
                }) {
                    test
                    ... on AFoo {
                        afoo
                    }
                    ... on AFoo {
                        __typename
                    }
                }
            }
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo1(arg: {arg1 : "Query"}) {
    ... on AFoo {
      test
      afoo
      __typename
    }
  }
}
'''
    }

    def "handles typename outside fragment and inside fragment"() {
        def sdl = '''
        type Query {
            foo1(arg: I): Foo 
        }
        type Mutation {
            foo1(arg: I): Foo 
        }
        interface Foo {
            test: String
        }
        type AFoo implements Foo {
            test: String
        }
        input I {
            arg1: String
        }
        '''
        def query = '''query {
            ... on Query {
                foo1(arg: {
                    arg1: "Query"
                }) {
                    __typename
                    test
                    ... on AFoo {
                        __typename
                    }
                }
            }
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)
        when:
        def document = compileToDocument(schema, QUERY, null, fields).first
        then:
        AstPrinter.printAst(document) == '''query {
  foo1(arg: {arg1 : "Query"}) {
    ... on AFoo {
      __typename
      test
    }
  }
}
'''
    }

    // --------------------------------------------------------------------------------------
    // Custom JSON handling

    def "test JSON when input is a variable"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON!): String
        }
        
        scalar JSON
        '''
        def query = '''mutation hello($var: JSON!) {
            foo1(arg: $var)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)

        def vars = [var: ["48x48": "hello"]]
        def fields = createNormalizedFields(schema, query, vars)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [var_0: ["48x48": "hello"]]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON!) {
  foo1(arg: $var_0)
}
'''
    }

    def "test JSON when input is a string variable"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON!): String
        }
        
        scalar JSON
        '''
        def query = '''mutation hello($var: JSON!) {
            foo1(arg: $var)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)

        def vars = [var: "hello there"]
        def fields = createNormalizedFields(schema, query, vars)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [var_0: "hello there"]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON!) {
  foo1(arg: $var_0)
}
'''
    }

    def "test JSON when input is an int variable"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON!): String
        }
        
        scalar JSON
        '''
        def query = '''mutation hello($var: JSON!) {
            foo1(arg: $var)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)

        def vars = [var: 1]
        def fields = createNormalizedFields(schema, query, vars)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [var_0: 1]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON!) {
  foo1(arg: $var_0)
}
'''
    }

    def "test JSON scalar when JSON arg is null"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON): String
        }
        
        scalar JSON
        '''
        def query = '''mutation {
            foo1
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [:]
        AstPrinter.printAst(document) == '''mutation {
  foo1
}
'''
    }

    def "test JSON scalar when JSON arg is explicitly null"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON): String
        }
        
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg: null)
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [:]
        AstPrinter.printAst(document) == '''mutation {
  foo1(arg: null)
}
'''
    }

    def "test JSON scalar when input is inlined"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: JSON!): String
        }
        
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg: {one: "two", three: ["four", "five"]})
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def variables = pair.second
        then:
        variables == [var_0: [one: "two", three: ["four", "five"]]]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON!) {
  foo1(arg: $var_0)
}
'''
    }

    def "test JSON scalar when input is inlined, multiple JSON args"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg1: JSON!, arg2: [JSON!]): String
        }
        
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg1: {one: "two", three: ["four", "five"]}, arg2: [{one: "two", three: ["four", "five"]}])
        }
        '''
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def pair = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = pair.first
        def vars = pair.second
        then:
        vars.size() == 2
        vars['var_0'] == [one: "two", three: ["four", "five"]]
        vars['var_1'] == [[one: "two", three: ["four", "five"]]]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON!, $var_1: [JSON!]) {
  foo1(arg1: $var_0, arg2: $var_1)
}
'''
    }

    def "test JSON scalar inside an input type"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: InputWithJson): String
        }
        
        input InputWithJson {
          id: ID
          json: JSON
        }
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg: {id: "ID-00", json: {name: "Zlatan", lastName: "Ibrahimoviç", clubs: ["MU", "Barsa", "Inter", "Milan"]}})
        }
        '''

        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def variablesAndDocument = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = variablesAndDocument.first
        def vars = variablesAndDocument.second
        then:
        vars.size() == 1
        vars['var_0'] == [lastName: "Ibrahimoviç", name: "Zlatan", clubs: ["MU", "Barsa", "Inter", "Milan"]]
        AstPrinter.printAst(document) == '''mutation ($var_0: JSON) {
  foo1(arg: {id : "ID-00", json : $var_0})
}
'''
    }

    def "test JSON scalar inside an input type, json value is explicitly null"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: InputWithJson): String
        }
        
        input InputWithJson {
          id: ID
          json: JSON
        }
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg: {id: "ID-00", json: null})
        }
        '''

        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def variablesAndDocument = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = variablesAndDocument.first
        def vars = variablesAndDocument.second
        then:
        vars == [:]
        AstPrinter.printAst(document) == '''mutation {
  foo1(arg: {id : "ID-00", json : null})
}
'''
    }

    def "test JSON scalar inside an input type, json value is null"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: InputWithJson): String
        }
        
        input InputWithJson {
          id: ID
          json: JSON
        }
        scalar JSON
        '''
        def query = '''mutation {
            foo1(arg: {id: "ID-00"})
        }
        '''

        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query)

        def variablesAndDocument = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = variablesAndDocument.first
        def vars = variablesAndDocument.second
        then:
        vars == [:]
        AstPrinter.printAst(document) == '''mutation {
  foo1(arg: {id : "ID-00"})
}
'''
    }

    def "test JSON scalar inside an input type, json key is illegal graphql input name"() {
        def sdl = '''
        type Query {
            foo: String
        }
        type Mutation {
            foo1(arg: InputWithJson): String
        }
        
        input InputWithJson {
          id: ID
          json: [JSON!]
        }
        scalar JSON
        '''
        def query = '''mutation test($var: InputWithJson) {
            foo1(arg: $var)
        }
        '''
        def variables = [var: [
                id  : "ID-00",
                json: [[name    : "Zlatan",
                        lastName: "Ibrahimoviç",
                        clubs   : ["MU", "Barsa", "Inter", "Milan", null],
                        "48x48" : "Zlatan_48x48.jpg",
                        "96x96" : null
                       ]]
        ]]
        GraphQLSchema schema = TestUtil.schema(sdl)
        def fields = createNormalizedFields(schema, query, variables)

        def variablesAndDocument = compileToDocument(schema, MUTATION, null, fields)
        when:
        def document = variablesAndDocument.first
        def vars = variablesAndDocument.second
        then:
        vars.size() == 1
        vars['var_0'] == [[name    : "Zlatan",
                           lastName: "Ibrahimoviç",
                           clubs   : ["MU", "Barsa", "Inter", "Milan", null],
                           "48x48" : "Zlatan_48x48.jpg",
                           "96x96" : null
                          ]]
        AstPrinter.printAst(document) == '''mutation ($var_0: [JSON!]) {
  foo1(arg: {id : "ID-00", json : $var_0})
}
'''
    }


    private List<ExecutableNormalizedField> createNormalizedFields(GraphQLSchema schema, String query, Map<String, Object> variables = [:]) {
        assertValidQuery(schema, query, variables)
        Document originalDocument = TestUtil.parseQuery(query)

        ExecutableNormalizedOperationFactory dependencyGraph = new ExecutableNormalizedOperationFactory()
        def tree = dependencyGraph.createExecutableNormalizedOperationWithRawVariables(schema, originalDocument, null, variables)
        return tree.getTopLevelFields()
    }

    private void assertValidQuery(GraphQLSchema graphQLSchema, String query, Map variables = [:]) {
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build()
        assert graphQL.execute(query, null, variables).errors.size() == 0
    }
}
