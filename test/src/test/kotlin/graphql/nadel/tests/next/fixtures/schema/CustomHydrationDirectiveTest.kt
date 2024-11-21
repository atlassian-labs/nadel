package graphql.nadel.tests.next.fixtures.schema

import graphql.language.Argument
import graphql.language.ArrayValue
import graphql.language.AstPrinter
import graphql.language.Directive
import graphql.language.FieldDefinition
import graphql.language.ObjectField
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.Nadel
import graphql.nadel.NadelDefinitionRegistry
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelOperationKind
import graphql.nadel.NadelSchemas
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.schema.NadelServicesTransformationHook
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.scalars.ExtendedScalars

class CustomHydrationDirectiveTest : NadelIntegrationTest(
    query = """
        query {
          businessReport_findRecentWorkByTeam(
            teamId: "hello"
            first: 10
            after: "2012"
          ) {
            edges {
              node {
                ... on JiraIssue {
                  key
                }
                ... on BitbucketPullRequest {
                  title
                  patch
                }
              }
              cursor
            }
            pageInfo {
              hasNextPage
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        // Backing service
        Service(
            name = "graph_store",
            overallSchema = """
                scalar JSON
                type Query {
                  graphStore_query(
                    query: String!
                    first: Int
                    after: String
                    other: JSON @hydrationRemainingArguments
                  ): GraphStoreQueryConnection
                }
                type GraphStoreQueryConnection {
                  edges: [GraphStoreQueryEdge]
                  pageInfo: PageInfo
                }
                type GraphStoreQueryEdge {
                  nodeId: ID
                  cursor: String
                }
                type PageInfo {
                  hasNextPage: Boolean!
                  hasPreviousPage: Boolean!
                  startCursor: String
                  endCursor: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class GraphStoreQueryEdge(
                    val nodeId: String,
                    val cursor: String?,
                )

                data class PageInfo(
                    val hasNextPage: Boolean,
                    val hasPreviousPage: Boolean,
                    val startCursor: String?,
                    val endCursor: String?,
                )

                data class GraphStoreQueryConnection(
                    val edges: List<GraphStoreQueryEdge>,
                    val pageInfo: PageInfo,
                )

                wiring
                    .scalar(ExtendedScalars.Json)
                    .type("Query") { type ->
                        type
                            .dataFetcher("graphStore_query") { env ->
                                GraphStoreQueryConnection(
                                    edges = listOf(
                                        GraphStoreQueryEdge(
                                            nodeId = "ari:cloud:jira::issue/1",
                                            cursor = "1",
                                        ),
                                        GraphStoreQueryEdge(
                                            nodeId = "ari:cloud:bitbucket::pull-request/2",
                                            cursor = "2",
                                        ),
                                    ),
                                    pageInfo = PageInfo(
                                        hasNextPage = true,
                                        hasPreviousPage = false,
                                        startCursor = null,
                                        endCursor = "1",
                                    ),
                                )
                            }
                    }
            },
        ),
        // Service for hydration
        Service(
            name = "jira",
            overallSchema = """
                type Query {
                  issuesByIds(ids: [ID!]!): [JiraIssue]
                }
                type JiraIssue {
                  id: ID!
                  key: String!
                  title: String!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class JiraIssue(
                    val id: String,
                    val key: String,
                    val title: String,
                )

                val issuesByIds = listOf(
                    JiraIssue(
                        id = "ari:cloud:jira::issue/1",
                        key = "GQLGW-1",
                        title = "Fix the speed of light",
                    ),
                    JiraIssue(
                        id = "ari:cloud:jira::issue/2",
                        key = "GQLGW-2",
                        title = "Refactor Nadel",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issuesByIds") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!
                                    .map {
                                        issuesByIds[it]
                                    }
                            }
                    }
            },
        ),
        Service(
            name = "bitbucket",
            overallSchema = """
                type Query {
                  pullRequestsByIds(ids: [ID!]!): [BitbucketPullRequest]
                }
                type BitbucketPullRequest {
                  id: ID!
                  title: String
                  patch: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class BitbucketPullRequest(
                    val id: String,
                    val title: String,
                    val patch: String,
                )

                val issuesByIds = listOf(
                    BitbucketPullRequest(
                        id = "ari:cloud:bitbucket::pull-request/1",
                        title = "Delete everything",
                        patch = "-",
                    ),
                    BitbucketPullRequest(
                        id = "ari:cloud:bitbucket::pull-request/2",
                        title = "Initial Commit",
                        patch = "+",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("pullRequestsByIds") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!
                                    .map {
                                        issuesByIds[it]
                                    }
                            }
                    }
            },
        ),
        // Service that introduces virtual type
        Service(
            name = "work",
            overallSchema = """
                directive @agsQuery(query: String) on FIELD_DEFINITION
                type Query {
                  businessReport_findRecentWorkByTeam(
                    teamId: ID!
                    first: Int
                    after: String
                  ): WorkConnection
                    @agsQuery(query: "DROP TABLE")
                }
                type WorkConnection @virtualType {
                  edges: [WorkEdge]
                  pageInfo: PageInfo
                }
                type WorkEdge @virtualType {
                  nodeId: ID @hidden
                  node: WorkNode
                    @hydrated(
                      service: "jira"
                      field: "issuesByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                    @hydrated(
                      service: "bitbucket"
                      field: "pullRequestsByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                  cursor: String
                }
                union WorkNode = JiraIssue | BitbucketPullRequest
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  business_stub: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        if (instructions.size == 1) {
                            return instructions.single()
                        }

                        @Suppress("UNCHECKED_CAST")
                        val nodeId = (parentNode.value as JsonMap)[aliasHelper.getResultKey("nodeId")] as String

                        val prs = instructions.single { it.backingFieldDef.name == "pullRequestsByIds" }
                        val issues = instructions.single { it.backingFieldDef.name == "issuesByIds" }

                        return when {
                            nodeId.startsWith("ari:cloud:bitbucket::pull-request/") -> prs
                            nodeId.startsWith("ari:cloud:jira::issue/") -> issues
                            else -> throw IllegalArgumentException()
                        }
                    }
                }
            )
    }

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .virtualTypeSupport { true }
    }

    override fun makeNadelSchemas(): NadelSchemas.Builder {
        return super.makeNadelSchemas()
            .servicesTransformationHook(
                CustomHydrationDirectiveTransform(
                    customDirectiveToBackingField = mapOf(
                        "agsQuery" to "graphStore_query",
                    ),
                ),
            )
    }
}

private data class CustomHydrationDirectiveTransformContext(
    val services: List<Service>,
    val fieldToService: Map<String, Service>,
)

private class CustomHydrationDirectiveTransform(
    private val customDirectiveToBackingField: Map<String, String>,
) : NadelServicesTransformationHook {
    override fun transform(services: List<Service>): List<Service> {
        val context = CustomHydrationDirectiveTransformContext(
            services = services,
            fieldToService = createFieldContributorMap(services),
        )

        return with(context) {
            transformServices(services)
        }
    }

    private fun createFieldContributorMap(services: List<Service>): Map<String, Service> {
        return services
            .asSequence()
            .flatMap { service ->
                val queryDefs = (service.definitionRegistry.operationMap[NadelOperationKind.Query] ?: emptyList())
                queryDefs
                    .asSequence()
                    .flatMap {
                        it.fieldDefinitions
                    }
                    .map {
                        it.name to service
                    }
            }
            .toMap()
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformServices(services: List<Service>): List<Service> {
        return services
            .map { service ->
                val newDefinitions = service.definitionRegistry
                    .definitions
                    .map { type ->
                        if (type is ObjectTypeDefinition) {
                            transformObjectType(type)
                        } else {
                            type
                        }
                    }

                Service(
                    name = service.name,
                    underlyingSchema = service.underlyingSchema,
                    serviceExecution = service.serviceExecution,
                    definitionRegistry = NadelDefinitionRegistry.from(newDefinitions),
                )
            }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformObjectType(objectType: ObjectTypeDefinition): ObjectTypeDefinition {
        return objectType
            .transform { builder ->
                builder.fieldDefinitions(transformFields(objectType.fieldDefinitions))
            }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformFields(fieldDefinitions: List<FieldDefinition>): List<FieldDefinition> {
        return fieldDefinitions
            .map { fieldDefinition ->
                transformField(fieldDefinition)
            }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformField(fieldDefinition: FieldDefinition): FieldDefinition {
        return if (fieldDefinition.directives.any { it.name in customDirectiveToBackingField }) {
            fieldDefinition
                .transform { builder ->
                    builder.directives(transformDirectives(fieldDefinition))
                }
        } else {
            fieldDefinition
        }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformDirectives(
        fieldDefinition: FieldDefinition,
    ): List<Directive> {
        return fieldDefinition.directives
            .map {
                if (it.name in customDirectiveToBackingField) {
                    transformCustomHydrationDefinition(fieldDefinition, it)
                } else {
                    it
                }
            }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun transformCustomHydrationDefinition(
        parent: FieldDefinition,
        customHydrationDirective: Directive,
    ): Directive {
        val staticArgs = customHydrationDirective
            .arguments
            .map {
                ObjectValue(
                    listOf(
                        ObjectField("name", StringValue(it.name)),
                        ObjectField("value", it.value),
                    ),
                )
            }

        val pathToBackingField = customDirectiveToBackingField[customHydrationDirective.name]!!
        val backingFieldDef = getBackingFieldDef(customHydrationDirective)

        val matchingArgs = parent.inputValueDefinitions
            .asSequence()
            .filter { virtualFieldArg ->
                backingFieldDef.inputValueDefinitions.any { it.name == virtualFieldArg.name }
            }
            .map { arg ->
                ObjectValue(
                    listOf(
                        ObjectField("name", StringValue(arg.name)),
                        ObjectField("value", StringValue("$" + "argument.${arg.name}")),
                    ),
                )
            }
            .toList()

        return Directive.newDirective()
            .name(NadelDirectives.hydratedDirectiveDefinition.name)
            .argument(
                Argument(
                    NadelHydrationDefinition.Keyword.field,
                    StringValue(pathToBackingField),
                ),
            )
            .argument(
                Argument(
                    NadelHydrationDefinition.Keyword.arguments,
                    ArrayValue(staticArgs + matchingArgs),
                ),
            )
            .build()
            .also {
                println(AstPrinter.printAst(it))
            }
    }

    context(CustomHydrationDirectiveTransformContext)
    private fun getBackingFieldDef(customHydrationDirective: Directive): FieldDefinition {
        val pathToBackingField = customDirectiveToBackingField[customHydrationDirective.name]!!.split(".")
        val service = fieldToService[pathToBackingField.first()]!!
        val queryTypes = service.definitionRegistry.operationMap[NadelOperationKind.Query]!!

        val parentTypes = pathToBackingField
            .viewOfExcludingLast(1)
            // e.g. [(Query, jira), (JiraQuery, issues), (JiraIssueQuery, byId)] etc.
            .fold(queryTypes.asSequence()) { parentTypes, pathSegment ->
                val fieldDef = parentTypes
                    .flatMap {
                        it.fieldDefinitions
                    }
                    .first {
                        it.name == pathSegment
                    }

                service.definitionRegistry.getDefinitions(fieldDef.type.unwrapAll().name)
                    .asSequence()
                    .filterIsInstance<ObjectTypeDefinition>()
            }

        return parentTypes
            .flatMap {
                it.fieldDefinitions
            }
            .first {
                it.name == pathToBackingField.last()
            }
    }

    /**
     * [List.dropLast] but implemented using [List.subList] so it never clones the list.
     */
    private fun <E> List<E>.viewOfExcludingLast(n: Int): List<E> {
        return subList(0, size - n)
    }
}
