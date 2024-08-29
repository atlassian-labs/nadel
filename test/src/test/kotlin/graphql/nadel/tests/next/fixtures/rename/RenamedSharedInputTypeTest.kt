package graphql.nadel.tests.next.fixtures.rename

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * The ConfluenceLegacyPathType enum type is renamed, and we share it between the two defined services in this test.
 * The enum is also exclusively used as an input type, never as an output type.
 *
 * We need to ensure that it's renamed when sent to the underlying service.
 */
class RenamedSharedInputTypeTest : NadelIntegrationTest(
    query = """
      query {
        something {
          users {
            profilePicture {
              path(type: ABSOLUTE)
            }
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "confluence_legacy",
            overallSchema = """
                type Query {
                  me: ConfluenceLegacyUser
                }
                type ConfluenceLegacyUser @renamed(from: "User") {
                  profilePicture: ConfluenceLegacyProfilePicture
                }
                type ConfluenceLegacyProfilePicture @renamed(from: "ProfilePicture") {
                  path(type: ConfluenceLegacyPathType!): String
                }
                enum ConfluenceLegacyPathType @renamed(from: "PathType") {
                  ABSOLUTE
                  RELATIVE
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("me") { env ->
                                throw UnsupportedOperationException("Not implemented")
                            }
                    }
            },
        ),
        Service(
            name = "confluence_something",
            overallSchema = """
                type Query {
                  something: ConfluenceLegacySomething
                }
                type ConfluenceLegacySomething @renamed(from: "Something") {
                  users: [ConfluenceLegacyUser]
                }
            """.trimIndent(),
            // Need to explicitly declare underlying schema for shared type
            underlyingSchema = """
                type Query {
                  something: Something
                }
                type Something {
                  users: [User]
                }
                type User {
                  profilePicture: ProfilePicture
                }
                type ProfilePicture {
                  path(type: PathType!): String
                }
                enum PathType {
                  ABSOLUTE
                  RELATIVE
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class ProfilePicture(
                    val absolutePath: String,
                    val relativePath: String,
                )

                data class User(
                    val profilePicture: ProfilePicture,
                )

                data class Something(
                    val users: List<User>,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("something") { env ->
                                Something(
                                    users = listOf(
                                        User(
                                            profilePicture = ProfilePicture(
                                                relativePath = "/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70",
                                                absolutePath = "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70",
                                            ),
                                        ),
                                    ),
                                )
                            }
                    }
                    .type("ProfilePicture") { type ->
                        type
                            .dataFetcher("path") { env ->
                                val pfp = env.getSource<ProfilePicture>()!!
                                when (val urlType = env.getArgument<String>("type")) {
                                    "ABSOLUTE" -> pfp.absolutePath
                                    "RELATIVE" -> pfp.relativePath
                                    else -> throw IllegalArgumentException(urlType)
                                }
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            // todo: this should be on by default
            .allDocumentVariablesHint {
                true
            }
    }
}
