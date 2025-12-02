package graphql.nadel.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.GraphQLError
import graphql.language.AstPrinter
import graphql.language.StringValue
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput.Companion.newNadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.hooks.ServiceOrError
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.nadel.validation.NadelSchemaValidationFactory
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.future.asDeferred
import java.io.File
import java.util.concurrent.CompletableFuture

val File.parents: Sequence<File>
    get() = sequence {
        var file: File? = parentFile
        while (file != null) {
            yield(file)
            file = file.parentFile
        }
    }

/**
 * By default, we don't actually run any queries against the Nadel instance.
 *
 * Set this to true to run the query. You'll have to modify the [ServiceExecutionFactory] to
 * actually return something e.g. response from Splunk etc.
 */
const val runQuery = false

/**
 * You can use this script to run central schema locally in Nadel without
 * booting up the full Gateway.
 *
 * This can be used for debugging Nadel instantiation, query execution, and
 * running the latest validation against central schema.
 *
 * Remember that this is NOT the exact same as running the full Gateway.
 * This is only here for convenience as the full Gateway is a little slower
 * to debug.
 */
suspend fun main() {
    val schema = File(
        "/Users/fwang/Documents/Atlassian/graphql-central-schema/schema/",
    )
    val overallSchemas = mutableMapOf<String, String>()
    val underlyingSchemas = mutableMapOf<String, String>()

    schema.walkTopDown()
        .filter {
            it.isFile
        }
        .mapNotNull { file ->
            val relativeFilePath = file.absolutePath.removePrefix(schema.absolutePath).trimStart('/')
            val serviceName = getServiceName(File(relativeFilePath))

            file to (serviceName ?: return@mapNotNull null)
        }
        .forEach { (file, serviceName) ->
            fun MutableMap<String, String>.append(text: String) {
                compute(serviceName) { _, oldValue ->
                    if (oldValue == null) {
                        text + "\n"
                    } else {
                        oldValue + "\n" + text + "\n"
                    }
                }
            }

            if (file.extension == "nadel") {
                overallSchemas.append(file.readText())
            } else if (file.extension == "graphqls" || file.extension == "graphql") {
                underlyingSchemas.append(file.readText())
            }
        }

    val nadel = Nadel.newNadel()
        .executionHooks(
            object : NadelExecutionHooks {
                override fun resolveServiceForField(
                    services: List<Service>,
                    executableNormalizedField: ExecutableNormalizedField,
                ): ServiceOrError? {
                    require(executableNormalizedField.name == "node")
                    val id = (executableNormalizedField.normalizedArguments["id"]!!.value as StringValue).value
                    return ServiceOrError(services.first { it.name == id }, null)
                }
            }
        )
        .overallSchemas(overallSchemas)
        .underlyingSchemas(underlyingSchemas)
        .serviceExecutionFactory(object : ServiceExecutionFactory {
            override fun getServiceExecution(serviceName: String): ServiceExecution {
                return ServiceExecution {
                    AstPrinter.printAst(it.query).also(::println)

                    val response: JsonMap = ObjectMapper().readValue(
                        File("/Users/fwang/Library/Application Support/JetBrains/IntelliJIdea2021.1/scratches/buffer2.kts")
                            .readText(),
                    )

                    CompletableFuture.completedFuture(
                        NadelServiceExecutionResultImpl(
                            response["data"] as MutableJsonMap
                        ),
                    )
                }
            }
        })
        .overallWiringFactory(GatewaySchemaWiringFactory())
        .underlyingWiringFactory(GatewaySchemaWiringFactory())
        .build()

    val nadelSchemas = NadelSchemas.Builder()
        .overallSchemas(overallSchemas)
        .underlyingSchemas(underlyingSchemas)
        .overallWiringFactory(GatewaySchemaWiringFactory())
        .underlyingWiringFactory(GatewaySchemaWiringFactory())
        .stubServiceExecution()
        .build()

    NadelSchemaValidationFactory.create()
        .validate(nadelSchemas)
        .sortedBy { it.javaClass.name }
        .asSequence()
        .map(NadelSchemaValidationError::toGraphQLError)
        .map(GraphQLError::toSpecification)
        .forEach(::println)

    @Suppress("ConstantConditionIf")
    if (!runQuery) {
        return
    }

    listOf(
        "agent",
        "automation_playbook",
        "bitbucket",
        "canned_response",
        "cc_graphql",
        "ccp",
        "compass",
        "customer360_server",
        "data_depot_external",
        "devai",
        "gira",
        "graph_integrations",
        "help_center",
        "help_layout",
        "help_object_store",
        "intent_management",
        "jcs_barista",
        "jira_align_twg_pipeline",
        "jsm_channel_orchestrator",
        "jsw",
        "kitsune",
        "knowledge_discovery",
        "learner_transcript_service",
        "loom",
        "mercury",
        "org_policy",
        "passionfruit",
        "radar",
        "shepherd",
        "teams",
        "teamsV2",
        "townsquare",
        "trello",
        "virtual_agent",
        "virtual_agent_conversation",
    )
        .forEach { service ->
            println(service)
            nadel
                .execute(
                    newNadelExecutionInput()
                        .query(query)
                        .variables(mapOf("var1" to service))
                        .build(),
                )
                .asDeferred()
                .await()
                .also {
                    println(it)
                }
        }
}

const val query = """query hydrate(${'$'}var1: ID!) {
  node(id: ${'$'}var1) {
    __typename
    ...DataFields
  }
}

fragment DataFields on GraphStoreCypherQueryV2AriNodeUnion {
  __typename
  ... on AtlassianAccountUser {
    id
    uname: name
  }
  ... on ConfluenceBlogPost {
    author {
      user {
        accountId
        name
      }
    }
    id
    links {
      base
      webUi
    }
    title
  }
  ... on ConfluencePage {
    author {
      user {
        accountId
        name
      }
    }
    id
    links {
      base
      webUi
    }
    title
  }
  ... on ExternalOrganisation {
    displayName
    id
  }
  ... on ExternalPullRequest {
    author {
      linkedUsers @optIn(to: "") {
        edges {
          node {
            __typename
            ... on AtlassianAccountUser {
              name
            }
          }
        }
      }
      thirdPartyUser {
        name
      }
      userId
    }
    commentCount
    destinationBranch {
      name
      url
    }
    id
    lastUpdate
    repositoryId
    reviewers {
      approvalStatus
      user {
        linkedUsers @optIn(to: "") {
          edges {
            node {
              __typename
              ... on AtlassianAccountUser {
                name
              }
            }
          }
        }
        thirdPartyUser {
          name
        }
        userId
      }
    }
    s: status
    title
    u: url
  }
  ... on ExternalRepository {
    id
    lastUpdated
    a: name
    b: url
  }
  ... on JiraIssue {
    assigneeField {
      user {
        accountId
        name
      }
    }
    id
    key
    c: status {
      name
    }
    summary
    webUrl
  }
  ... on JiraWebRemoteIssueLink {
    href
    iconUrl
    id
    summary
    title
  }
  ... on LoomVideo {
    d: description
    id
    isMeeting
    name
    owner {
      id
      name
    }
    playableDuration
    sourceDuration
    e: url
  }
  ... on MercuryFocusArea {
    aboutContent {
      editorAdfContent
    }
    aggregatedFocusAreaStatusCount {
      current {
        atRisk
        completed
        inProgress
        offTrack
        onTrack
        paused
        pending
        total
      }
    }
    createdDate
    focusAreaStatusUpdates(first: 0) {
      edges {
        node {
          ari
          createdBy {
            id
            name
          }
          createdDate
          id
          newHealth {
            displayName
          }
          newStatus {
            displayName
          }
          newTargetDate {
            targetDate
            targetDateType
          }
          previousHealth {
            displayName
          }
          previousStatus {
            displayName
          }
          previousTargetDate {
            targetDate
            targetDateType
          }
          summary
          updatedBy {
            id
            name
          }
          updatedDate
        }
      }
    }
    focusAreaType {
      name
    }
    health {
      displayName
    }
    id
    name
    owner {
      id
      name
    }
    parent {
      id
      name
      url
    }
    f: status {
      displayName
    }
    subFocusAreas(first: 0) {
      edges {
        node {
          id
          name
          owner {
            id
            name
          }
          url
        }
      }
      totalCount
    }
    targetDate {
      targetDate
      targetDateType
    }
    updatedDate
    url
    watchers {
      edges {
        node {
          id
          name
        }
      }
      totalCount
    }
  }
  ... on TeamV2 {
    creator {
      id
      name
    }
    g: description
    displayName
    id
    members(state: FULL_MEMBER) {
      nodes {
        member {
          accountStatus
          id
          name
        }
      }
    }
    membershipSettings
    h: state
  }
  ... on TownsquareGoal {
    creationDate
    i: description
    id
    key
    name
    owner {
      id
      name
    }
    parentGoal {
      id
    }
    status {
      score
      value
    }
    targetDate {
      confidence
      dateRange {
        end
        start
      }
      label
    }
    url
  }
  ... on TownsquareProject {
    description {
      measurement
      what
      why
    }
    dueDate {
      confidence
      dateRange {
        end
        start
      }
      label
    }
    id
    key
    name
    owner {
      id
      name
    }
    state {
      label
      value
    }
    url
  }
}"""

private fun getServiceName(file: File): String? {
    val parts: List<File> = splitFileParts(file)
    val partCount = parts.size

    if (partCount <= 1 || partCount > 4) {
        return null
    }

    if (partCount == 4) {
        // it might be a <serviceGroup>/<serviceName>/underlying|overall/some.file
        val underlyingOrOverallDirPart = parts[2].name
        return if (underlyingOrOverallDirPart == UNDERLYING || underlyingOrOverallDirPart == OVERALL) {
            parts[1].name
        } else null
    }

    if (partCount == 3) {
        // it might be a <serviceGroup>/<serviceName>/some.file
        // OR
        // <serviceName>/underlying|overall/some.file
        val name = parts[1].name
        return if (name == UNDERLYING || name == OVERALL) {
            parts[0].name
        } else name
    }

    return if (parts[1].name == SHARED_DOT_NADEL) {
        // it might be a <serviceGroup>/shared.nadel
        SHARED
    } else {
        // it must be a <serviceName>/some.file
        parts[0].name
    }
}

const val UNDERLYING = "underlying"
const val OVERALL = "overall"
const val SHARED_DOT_NADEL = "shared.nadel"
const val SHARED = "shared"
const val CONFIG_DOT_YAML = "config.yaml"
const val DOT_GRAPHQLS = ".graphqls"
const val DOT_GRAPHQL = ".graphql"
const val DOT_NADEL = ".nadel"

private fun splitFileParts(file: File): List<File> {
    var cursor = file

    val parts: MutableList<File> = ArrayList()
    parts.add(File(cursor.name))

    while (cursor.parentFile != null) {
        parts.add(cursor.parentFile)
        cursor = cursor.parentFile
    }

    parts.reverse()
    return parts
}
