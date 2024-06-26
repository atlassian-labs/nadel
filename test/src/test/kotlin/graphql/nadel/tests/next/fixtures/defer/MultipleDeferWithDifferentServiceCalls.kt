package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.Ignore

open class MultipleDeferWithDifferentServiceCalls : NadelIntegrationTest(
    query = """
        query {
          user {
            name
            ... @defer {
              profilePicture
            }
          }
          product {
            productName
            ... @defer {
              productImage
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
