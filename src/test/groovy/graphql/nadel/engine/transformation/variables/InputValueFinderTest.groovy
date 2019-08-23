package graphql.nadel.engine.transformation.variables


import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirectiveContainer
import spock.lang.Specification

class InputValueFinderTest extends Specification {

    def sdl = '''
        
        directive @directive1 on ARGUMENT_DEFINITION | FIELD_DEFINITION
        directive @argDirective on ARGUMENT_DEFINITION

        enum RGB {
            red, blue,green
        }
        
        input DeeperType {
            deeperField : String @directive1
        }
        
        input ComplexInputType {
            stringField : String 
            intField : Int
            enumField : RGB
            nonNullField : Boolean!
            listField : [ComplexInputType]
            objectField : DeeperType     
        }

        type Query {
            field( arg : ComplexInputType) : String
            fieldSimple( simpleArg : String @argDirective , otherArg : String) : String
        }
    '''

    def schema = TestUtil.schema(sdl)

    GraphQLArgument arg = schema.getObjectType("Query").getFieldDefinition("field").getArgument("arg")
    GraphQLArgument simpleArg = schema.getObjectType("Query").getFieldDefinition("fieldSimple").getArgument("simpleArg")
    GraphQLArgument otherArg = schema.getObjectType("Query").getFieldDefinition("fieldSimple").getArgument("otherArg")

    def complexFieldObj1 = [nonNullField: true, listField: [], stringField: "abc"]
    def complexFieldObj2 = [nonNullField: false, listField: [], stringField: "def"]

    def complexValue = [
            stringField: "abc", intField: 666, nonNullField: true, enumField: "red",
            listField  : [complexFieldObj1, complexFieldObj2],
            objectField: [deeperField : "deeperFieldValue"]
    ]

    def "can find directives in complex types"() {

        InputValueFindFunction<GraphQLDirectiveContainer> findFunction = { coercedValue, inputTree ->
            if (inputTree.valueDefinition.getDirective("directive1")) {
                return Optional.of(inputTree.valueDefinition)
            }
            return Optional.empty()
        }

        def coercedArgs = [
                arg:
                        complexValue
        ]

        when:
        List<GraphQLDirectiveContainer> foundValues = InputValueFinder.find([simpleArg, otherArg, arg], coercedArgs, findFunction)

        then:
        !foundValues.isEmpty()
        foundValues[0].name == "deeperField"
    }

    def "can find directives in simple types"() {

        InputValueFindFunction findFunction = { coercedValue, inputTree ->
            if (inputTree.valueDefinition.getDirective("argDirective")) {
                return Optional.of(inputTree.valueDefinition)
            }
            return Optional.empty()
        }
        def coercedArgs = [
                simpleArg: "hi there",
                otherArg : "bye there"
        ]

        when:
        List<GraphQLDirectiveContainer> foundValues = InputValueFinder.find([simpleArg, otherArg], coercedArgs, findFunction)

        then:
        !foundValues.isEmpty()
        foundValues[0].name == "simpleArg"
    }

    def "can NOT find directives"() {

        InputValueFindFunction findFunction = { coercedValue, inputTree ->
            return Optional.empty()
        }
        def coercedArgs = [
                arg:
                        complexValue
        ]

        when:
        List<GraphQLDirectiveContainer> foundValues = InputValueFinder.find([arg], coercedArgs, findFunction)

        then:
        foundValues.isEmpty()
    }
}
