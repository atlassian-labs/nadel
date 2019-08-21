package graphql.nadel.engine.transformation.variables

import graphql.Scalars
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLEnumType
import spock.lang.Specification

class VariablesTransformerTest extends Specification {

    def sdl = '''
        
        directive @directive1 on ARGUMENT_DEFINITION | FIELD_DEFINITION

        enum RGB {
            red, blue,green
        }
        
        input ComplexInputType {
            stringField : String @directive1
            intField : Int
            enumField : RGB
            nonNullField : Boolean!
            listField : [ComplexInputType]
            objectField : ComplexInputType     
        }

        type Query {
            field( arg : ComplexInputType) : String
        }
    '''

    def schema = TestUtil.schema(sdl)

    def arg = schema.getObjectType("Query").getFieldDefinition("field").getArgument("arg")

    def "can modify variables"() {

        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                return value
            }
        }

        def complexFieldObj1 = [nonNullField: true, listField: [], stringField: "abc"]
        def complexFieldObj2 = [nonNullField: false, listField: [], stringField: "def"]
        def complexFieldObj3 = [nonNullField: true, listField: [], stringField: "xyz"]

        def variables = [arg: [
                stringField: "abc", intField: 666, nonNullField: true, enumField: "red",
                listField  : [complexFieldObj1, complexFieldObj2],
                objectField: complexFieldObj3
        ]]

        when:
        def newVariables = VariablesTransformer.transform([arg], variables, transformFunction)

        then:
        !newVariables.is(variables) // not the same object
        newVariables == variables // but build out the sames
    }

    def "can change variables"() {

        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                if (inputTypeTree.inputType == Scalars.GraphQLString) {
                    return String.valueOf(value).toUpperCase()
                }
                if (inputTypeTree.inputType == Scalars.GraphQLInt) {
                    return 999
                }
                if (inputTypeTree.inputType == Scalars.GraphQLBoolean) {
                    return !((Boolean) value)
                }
                if (inputTypeTree.inputType instanceof GraphQLEnumType) {
                    return String.valueOf(value).reverse()
                }

                if (inputTypeTree.name == "listField") {
                    return ["list", "changed"]
                }
                return value
            }
        }

        def complexFieldObj1 = [nonNullField: true, listField: [], stringField: "abc"]
        def complexFieldObj2 = [nonNullField: false, listField: [], stringField: "def"]
        def complexFieldObj3 = [nonNullField: true, listField: [], stringField: "xyz"]

        def variables = [arg: [
                stringField: "abc", intField: 666, nonNullField: true, enumField: "red",
                listField  : [complexFieldObj1, complexFieldObj2],
                objectField: complexFieldObj3
        ]]

        when:
        def newVariables = VariablesTransformer.transform([arg], variables, transformFunction)

        then:
        !newVariables.is(variables) // not the same object
        newVariables["arg"]["stringField"] == "ABC"
        newVariables["arg"]["intField"] == 999
        newVariables["arg"]["enumField"] == "der"
        newVariables["arg"]["nonNullField"] == false
        newVariables["arg"]["listField"] == ["list", "changed"]
    }

    def "leaves unnamed variables alone"() {
        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                if (inputTypeTree.inputType == Scalars.GraphQLString) {
                    return String.valueOf(value).toUpperCase()
                }
                return value
            }
        }

        def variables = [
                arg         : [stringField: "abc", intField: 666, nonNullField: true, enumField: "red",],
                leaveMeAlone: [stringField: "abc"]]

        when:
        def newVariables = VariablesTransformer.transform([arg], variables, transformFunction)

        then:
        newVariables["arg"]["stringField"] == "ABC"
        newVariables["leaveMeAlone"]["stringField"] == "abc"
    }

    def "grabs directive containers as they whizz past"() {
        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                if (inputTypeTree.name == "stringField") {
                    assert inputTypeTree.directivesContainer.getDirective("directive1") != null
                }
                return value
            }
        }

        def variables = [
                arg: [stringField: "abc", intField: 666, nonNullField: true, enumField: "red",]
        ]

        when:
        def newVariables = VariablesTransformer.transform([arg], variables, transformFunction)

        then:
        newVariables["arg"]["stringField"] == "abc"
    }

    def "leaves map with empty arg values alone"() {
        InputValueTransform transformFunction = new InputValueTransform() {
            @Override
            Object transformValue(Object value, InputValueTree inputTypeTree) {
                return value
            }
        }

        def variables = [leaveMeAlone: [stringField: "abc"]]

        when:
        def newVariables = VariablesTransformer.transform([arg], variables, transformFunction)

        then:
        newVariables == variables
    }
}

