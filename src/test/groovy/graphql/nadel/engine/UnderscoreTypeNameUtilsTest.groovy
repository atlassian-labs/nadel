package graphql.nadel.engine

import graphql.language.AstPrinter
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import spock.lang.Specification

import static graphql.nadel.testutils.ExecutionResultNodeUtil.leaf
import static graphql.nadel.testutils.ExecutionResultNodeUtil.list
import static graphql.nadel.testutils.ExecutionResultNodeUtil.object
import static graphql.nadel.testutils.ExecutionResultNodeUtil.root
import static graphql.nadel.testutils.ExecutionResultNodeUtil.toData
import static graphql.nadel.testutils.TestUtil.mkField

class UnderscoreTypeNameUtilsTest extends Specification {

    def context = NadelContext.newContext()
    def underscoreTypeNameAlias = context.underscoreTypeNameAlias
    def interfaceType = GraphQLInterfaceType.newInterface().name("I").build()
    def objectType = GraphQLObjectType.newObject().name("O").build()

    def "test that underscore type alias is added on interfaces"() {

        def petField = mkField('''
            pet {
                name
                title
            }
        ''')

        when:
        def newField = UnderscoreTypeNameUtils.maybeAddUnderscoreTypeName(context, petField, interfaceType)
        then:
        AstPrinter.printAstCompact(newField) == """pet {name title $underscoreTypeNameAlias:__typename}"""
    }

    def "test that underscore type alias is added on interfaces even if they have an __typename"() {

        def petField = mkField('''
            pet {
                name
                title
                ... {
                    __typename
                }
            }
        ''')

        when:
        def newField = UnderscoreTypeNameUtils.maybeAddUnderscoreTypeName(context, petField, interfaceType)
        then:
        AstPrinter.printAstCompact(newField) == """pet {name title ... {__typename} $underscoreTypeNameAlias:__typename}"""
    }

    def "test that underscore type alias is skipped on object"() {

        def petField = mkField('''
            pet {
                name
                title
            }
        ''')

        when:
        def newField = UnderscoreTypeNameUtils.maybeAddUnderscoreTypeName(context, petField, objectType)
        then:
        AstPrinter.printAstCompact(newField) == """pet {name title}"""
    }

    def "test that it removes the aliased field but leaves the specific one alone"() {

        def nodeWithAliasedTypeNameAndTypeName = root([
                object("pet", [
                        leaf("__typename"), //  <-- manually added by consumer
                        list("owners", [
                                object("0", [
                                        leaf("__typename"), //  <-- manually added by consumer say via Fragment Spread
                                        leaf("name"),
                                        leaf("__typename", underscoreTypeNameAlias),
                                        leaf("title"),
                                ]),
                                object("1", [
                                        leaf("name"),
                                        leaf("title"),
                                ]),
                        ])
                ])
        ])

        when:
        def newNode = UnderscoreTypeNameUtils.maybeRemoveUnderscoreTypeName(context, nodeWithAliasedTypeNameAndTypeName)
        def data = toData(newNode)
        then:
        data == [
                pet: [
                        __typename: "__typenameVal",
                        owners    : [
                                [
                                        __typename: "__typenameVal",
                                        name      : "nameVal",
                                        title     : "titleVal",
                                ],
                                [
                                        name : "nameVal",
                                        title: "titleVal",
                                ],
                        ]
                ]
        ]
    }

}
