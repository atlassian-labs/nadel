package graphql.nadel.dsl

import graphql.language.AstPrinter
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.parser.Parser
import io.kotest.core.spec.style.DescribeSpec

class AstValueReplacerTest : DescribeSpec({
    describe("can change values") {
        it("can replace object values in AST") {

            val startingValue = Parser.parseValue(
                """
                {
                    abc : "${'$'}argument.x"
                    note : "not to be changed"
                }
            """.trimIndent()
            )

            val valueReplacer = object : AstValueReplacer.ValueReplacer {
                override fun replaceString(strValue: String): Value<*>? {
                    if (strValue.contains("${'$'}argument.x")) {
                        return ObjectValue(
                            listOf(
                                ObjectField("x", StringValue("X")),
                                ObjectField("y", StringValue("Y"))
                            )
                        )
                    }
                    return null
                }
            }
            val newValue = AstValueReplacer().replaceValues(startingValue, valueReplacer)

            assert(AstPrinter.printAst(newValue) == """{abc : {x : "X", y : "Y"}, note : "not to be changed"}""")

        }
    }
})