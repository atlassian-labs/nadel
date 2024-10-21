package graphql.nadel.schema

import graphql.language.AstPrinter
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.nadelBatchObjectIdentifiedByDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationFromArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultFieldPredicateDefinition
import graphql.nadel.schema.NadelDirectives.partitionDirectiveDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertTrue

private const val source = "$" + "source"
private const val argument = "$" + "argument"

/**
 * todo: move these tests
 */
class NadelDirectivesTest : DescribeSpec({
    val commonDefs = """
        ${AstPrinter.printAst(hydratedDirectiveDefinition)}
        ${AstPrinter.printAst(nadelHydrationArgumentDefinition)}
        ${AstPrinter.printAst(nadelBatchObjectIdentifiedByDefinition)}
        ${AstPrinter.printAst(nadelHydrationFromArgumentDefinition)}
        ${AstPrinter.printAst(nadelHydrationConditionDefinition)}
        ${AstPrinter.printAst(nadelHydrationResultFieldPredicateDefinition)}
        ${AstPrinter.printAst(nadelHydrationResultConditionDefinition)}
        ${AstPrinter.printAst(partitionDirectiveDefinition)}
        scalar JSON
    """

    fun getSchema(schemaText: String): GraphQLSchema {
        val typeDefs = SchemaParser().parse(commonDefs + "\n" + schemaText)
        return SchemaGenerator().makeExecutableSchema(
            typeDefs,
            RuntimeWiring
                .newRuntimeWiring()
                .wiringFactory(NeverWiringFactory())
                .build(),
        )
    }

    describe("@hydrated") {
        it("can parse") {
            // given
            val schema = getSchema(
                """
                type Query {
                    field: String
                        @hydrated(
                            service: "IssueService"
                            field: "jira.issueById"
                            batchSize: 50
                            timeout: 100
                            arguments: [
                                {name: "fieldVal" value: "$source.namespace.issueId"}
                                {name: "argVal" value: "$argument.cloudId"}
                            ]
                            when: {
                                result: {
                                    sourceField: "type"
                                    predicate: { equals: "issue" }
                                }
                            }
                        )
                }
                """.trimIndent()
            )

            val field = schema.queryType.getField("field")

            // when
            val hydration = field.getHydrationDefinitions().single()

            // then
            assert(hydration.backingField == listOf("jira", "issueById"))
            assert(hydration.batchSize == 50)
            assert(hydration.timeout == 100)
            assert(hydration.arguments.size == 2)

            assertTrue(hydration.arguments[0].name == "fieldVal")
            val firstArgumentSource = hydration.arguments[0].value
            assertTrue(firstArgumentSource is NadelHydrationArgumentDefinition.ValueSource.ObjectField)
            assertTrue(firstArgumentSource.pathToField == listOf("namespace", "issueId"))

            assertTrue(hydration.arguments[1].name == "argVal")
            val secondArgumentSource = hydration.arguments[1].value
            assertTrue(secondArgumentSource is NadelHydrationArgumentDefinition.ValueSource.FieldArgument)
            assertTrue(secondArgumentSource.argumentName == "cloudId")
        }
    }
})
