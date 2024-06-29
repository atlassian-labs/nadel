package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.Ignore

open class ComprehensiveDeferQueryWithDifferentServiceCalls : NadelIntegrationTest(
    query = """
        query {
          user {
            name
            ... @defer {
              profilePicture
            }
            ... @defer(label: "team-details") {
              teamName
              teamMembers
            }
          }
          product {
            productName
            ... @defer {
              productImage
            }
            ... @defer(if: false) {
              productDescription
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "shared",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
        Service(
            name = "users",
            overallSchema = """
                type Query {
                  user: UserApi
                }
                type UserApi {
                  name: String
                  profilePicture: String
                  teamName: String
                  teamMembers: [String]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("user") { env ->
                                Any()
                            }
                    }
                    .type("UserApi") { type ->
                        type
                            .dataFetcher("name") { env ->
                                "Steven"
                            }
                            .dataFetcher("profilePicture") { env ->
                                "https://examplesite.com/user/profile_picture.jpg"
                            }
                            .dataFetcher("teamName") { env ->
                                "The Unicorns"
                            }
                            .dataFetcher("teamMembers") { env ->
                                listOf("Felipe", "Franklin", "Juliano")
                            }
                    }
            },
        ),
        Service(
            name = "product",
            overallSchema = """
                type Query {
                  product: ProductApi
                }
                type ProductApi {
                  productName: String
                  productImage: String
                  productDescription: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("product") { env ->
                                Any()
                            }
                    }
                    .type("ProductApi") { type ->
                        type
                            .dataFetcher("productName") { env ->
                                "Awesome Product"
                            }
                            .dataFetcher("profilePicture") { env ->
                                "https://examplesite.com/product/product_image.jpg"
                            }
                            .dataFetcher("profilePicture") { env ->
                                "This is a really awesome product with really awesome features."
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}
