package graphql.nadel.schema

import graphql.language.AstPrinter
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedFromDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedTemplateDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelBatchObjectIdentifiedByDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationFromArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationTemplateEnumDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultFieldPredicateDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultConditionDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import org.junit.jupiter.api.assertThrows

private const val source = "$" + "source"
private const val argument = "$" + "argument"

class NadelDirectivesTest : DescribeSpec({
    val commonDefs = """
        ${AstPrinter.printAst(hydratedDirectiveDefinition)}
        ${AstPrinter.printAst(nadelHydrationArgumentDefinition)}
        ${AstPrinter.printAst(nadelBatchObjectIdentifiedByDefinition)}
        ${AstPrinter.printAst(nadelHydrationFromArgumentDefinition)}
        ${AstPrinter.printAst(nadelHydrationTemplateEnumDefinition)}
        ${AstPrinter.printAst(hydratedFromDirectiveDefinition)}
        ${AstPrinter.printAst(hydratedTemplateDirectiveDefinition)}
        ${AstPrinter.printAst(nadelHydrationConditionDefinition)}
        ${AstPrinter.printAst(nadelHydrationResultFieldPredicateDefinition)}
        ${AstPrinter.printAst(nadelHydrationResultConditionDefinition)}
        scalar JSON
    """

    fun getSchema(schemaText: String): GraphQLSchema {
        val typeDefs = SchemaParser().parse(commonDefs + "\n" + schemaText)
        return SchemaGenerator().makeExecutableSchema(typeDefs, RuntimeWiring
            .newRuntimeWiring()
            .wiringFactory(NeverWiringFactory()).build())
    }

    describe("@hydrated") {
        it("can parse") {
            // given
            val schema = getSchema("""
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
            """.trimIndent())

            val field = schema.queryType.getField("field")

            // when
            val hydration = NadelDirectives.createUnderlyingServiceHydration(field, schema).single()

            // then
            assert(hydration.serviceName == "IssueService")
            assert(hydration.pathToActorField == listOf("jira", "issueById"))
            assert(hydration.batchSize == 50)
            assert(hydration.timeout == 100)
            assert(hydration.arguments.size == 2)

            assert(hydration.arguments[0].name == "fieldVal")
            assert(hydration.arguments[0].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField)
            assert(hydration.arguments[0].remoteArgumentSource.pathToField == listOf("namespace", "issueId"))

            assert(hydration.arguments[1].name == "argVal")
            assert(hydration.arguments[1].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument)
            assert(hydration.arguments[1].remoteArgumentSource.argumentName == "cloudId")
        }
    }

    describe("@hydratedFrom") {
        it("can parse") {
            val schema = getSchema("""
                extend enum NadelHydrationTemplate {
                    JIRA @hydratedTemplate(
                        service: "IssueService"
                        field: "jira.issueById"
                        batchSize: 50
                        timeout: 100
                    )
                }
                type Query {
                    field: String
                        @hydratedFrom(
                            template: JIRA
                            arguments: [
                                {name: "fieldVal" valueFromField: "namespace.issueId"}
                                {name: "argVal" valueFromArg: "cloudId"}
                                # for legacy confusion reasons
                                {name: "fieldValLegacy" valueFromField: "$source.namespace.issueId"}
                                {name: "argValLegacy" valueFromArg: "$argument.cloudId"}
                            ]
                        )
                }
            """.trimIndent())

            val fieldDef = schema.queryType.getFieldDefinition("field")

            // when
            val hydration = NadelDirectives.createUnderlyingServiceHydration(fieldDef, schema).single()

            // then
            assert(hydration.serviceName == "IssueService")
            assert(hydration.pathToActorField == listOf("jira", "issueById"))
            assert(hydration.batchSize == 50)
            assert(hydration.timeout == 100)
            assert(hydration.arguments.size == 4)

            assert(hydration.arguments[0].name == "fieldVal")
            assert(hydration.arguments[0].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField)
            assert(hydration.arguments[0].remoteArgumentSource.pathToField == listOf("namespace", "issueId"))

            assert(hydration.arguments[1].name == "argVal")
            assert(hydration.arguments[1].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument)
            assert(hydration.arguments[1].remoteArgumentSource.argumentName == "cloudId")

            assert(hydration.arguments[2].name == "fieldValLegacy")
            assert(hydration.arguments[2].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField)
            assert(hydration.arguments[2].remoteArgumentSource.pathToField == listOf("namespace", "issueId"))

            assert(hydration.arguments[3].name == "argValLegacy")
            assert(hydration.arguments[3].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument)
            assert(hydration.arguments[3].remoteArgumentSource.argumentName == "cloudId")
        }

        context("throws exception if valueFromField or valueFromArg are both specified or if neither are specified") {
            data class Input(val arguments: String, val error: String)

            withData(
                // none
                Input(
                    arguments = """{name: "fieldVal"}""",
                    error = "NadelHydrationFromArgument requires one of valueFromField or valueFromArg to be set",
                ),
                // both
                Input(
                    arguments = """{name: "fieldVal" valueFromField: "issueId" valueFromArg: "cloudId"}""",
                    error = "NadelHydrationFromArgument can not have both valueFromField and valueFromArg set",
                ),
            ) { (arguments, error) ->
                // given
                val schema = getSchema("""
                    extend enum NadelHydrationTemplate {
                        JIRA @hydratedTemplate(
                            service: "IssueService"
                            field: "jira.issueById"
                            batchSize: 50
                            timeout: 100
                        )
                    }
                    type Query {
                        field: String
                            @hydratedFrom(
                                template: JIRA
                                arguments: [$arguments]
                            )
                    }
                """.trimIndent())
                val fieldDef = schema.queryType.getField("field")

                // when
                val ex = assertThrows<IllegalArgumentException> {
                    NadelDirectives.createUnderlyingServiceHydration(fieldDef, schema).single()
                }

                // then
                assert(ex.message == error)
            }
        }
    }
})
