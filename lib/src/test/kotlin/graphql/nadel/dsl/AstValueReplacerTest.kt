package graphql.nadel.dsl

import graphql.GraphQLContext
import graphql.execution.ValuesResolver
import graphql.language.AstPrinter
import graphql.language.Value
import graphql.parser.Parser
import graphql.schema.GraphQLSchema
import graphql.schema.InputValueWithState
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.DescribeSpec
import java.util.Locale

class AstValueReplacerTest : DescribeSpec({

    fun getSchema(schemaText: String): GraphQLSchema {
        val typeDefs = SchemaParser().parse(schemaText)
        return SchemaGenerator().makeExecutableSchema(typeDefs, RuntimeWiring.MOCKED_WIRING)
    }


    describe("can change values") {

        val schema = getSchema(
            """
            type Query {
                q(arg : ComplexXyType) : String
            }
            
            input ComplexXyType {
                x : String
                y : String
            }
        """.trimIndent()
        )

        val complexXyType = schema.getType("ComplexXyType")!!

        val argument = "${'$'}argument"

        val xyValueReplacer = object : AstValueReplacer.ValueReplacer {
            override fun replaceString(strValue: String): Value<*>? {
                if (strValue.contains("$argument.x")) {
                    // we would find all $argument things
                    // then look them up to resolve to a JVM value
                    // then create an AST value of that external value based on some type
                    // which we get from the target field
                    val jvmValue = mapOf("x" to "X", "y" to "Y") // from argument object
                    val inputValueWithState = InputValueWithState.newExternalValue(jvmValue)
                    // maybe its already a literal ???
                    return ValuesResolver.valueToLiteral(
                        inputValueWithState,
                        complexXyType,
                        GraphQLContext.getDefault(),
                        Locale.getDefault()
                    )
                }
                return null
            }
        }

        it("can replace object values in AST") {

            val startingValue = Parser.parseValue(
                """
                {
                    abc : "${'$'}argument.x"
                    note : "not to be changed"
                }
            """.trimIndent()
            )
            val newValue = AstValueReplacer().replaceValues(startingValue, xyValueReplacer)

            assert(AstPrinter.printAst(newValue) == """{abc : {x : "X", y : "Y"}, note : "not to be changed"}""")

        }

        it("can replace string values in AST") {

            val startingValue = Parser.parseValue(""""${'$'}argument.x"""")

            val newValue = AstValueReplacer().replaceValues(startingValue, xyValueReplacer)

            assert(AstPrinter.printAst(newValue) == """{x : "X", y : "Y"}""")

        }

    }
})