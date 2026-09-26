package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * The `ConfluenceLegacyPathType` input type was renamed.
 *
 * In the test snapshot we ensure the variable is defined as `PathType`.
 *
 * This tests the NadelRenameArgumentInputTypesTransform with defer
 */
class DeferredRenamedInputTypeTest : NadelIntegrationTest(
    query = """
      query {
        me {
          profilePicture {
            ...@defer {
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
                data class ProfilePicture(
                    val absolutePath: String,
                    val relativePath: String,
                )

                data class User(
                    val profilePicture: ProfilePicture,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("me") { env ->
                                User(
                                    profilePicture = ProfilePicture(
                                        relativePath = "/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70",
                                        absolutePath = "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70",
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
