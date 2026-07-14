package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.multiinterface

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * The relaxed field's parent resolves to MORE THAN ONE interface. `Container.item` is declared as `FeedItem`,
 * but `PageContainer.item` narrows it to `PageItem` and `BlogContainer.item` to `BlogItem` (both implement
 * `FeedItem`). So under a bare `containers { item { id } }` the `id` field's parent output types are
 * `{PageItem, BlogItem}` - two interfaces - and `id` exists on both. Each underlying interface has a hidden impl
 * (`SecretPage`/`SecretBlog`), so a bare selection would otherwise expand into per-exposed-type fragments. This
 * exercises the multi-interface branch of the relaxation check.
 */
abstract class MultiInterfaceHiddenImplTestBase(
    @Language("GraphQL") query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "data",
            overallSchema = """
                type Query {
                  containers: [Container]
                }
                interface Container {
                  item: FeedItem
                }
                type PageContainer implements Container {
                  item: PageItem
                }
                type BlogContainer implements Container {
                  item: BlogItem
                }
                interface FeedItem {
                  id: ID
                }
                interface PageItem implements FeedItem {
                  id: ID
                }
                interface BlogItem implements FeedItem {
                  id: ID
                }
                type Wiki implements PageItem & FeedItem {
                  id: ID
                }
                type Post implements BlogItem & FeedItem {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  containers: [Container]
                }
                interface Container {
                  item: FeedItem
                }
                type PageContainer implements Container {
                  item: PageItem
                }
                type BlogContainer implements Container {
                  item: BlogItem
                }
                interface FeedItem {
                  id: ID
                }
                interface PageItem implements FeedItem {
                  id: ID
                }
                interface BlogItem implements FeedItem {
                  id: ID
                }
                type Wiki implements PageItem & FeedItem {
                  id: ID
                }
                type Post implements BlogItem & FeedItem {
                  id: ID
                }
                type SecretPage implements PageItem & FeedItem {
                  id: ID
                }
                type SecretBlog implements BlogItem & FeedItem {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Wiki(val id: String)
                data class Post(val id: String)
                data class PageContainer(val item: Any)
                data class BlogContainer(val item: Any)

                wiring
                    .type("Container") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("FeedItem") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("PageItem") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("BlogItem") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("containers") { _ ->
                            listOf(
                                PageContainer(item = Wiki(id = "WIKI-1")),
                                BlogContainer(item = Post(id = "POST-1")),
                            )
                        }
                    }
            },
        ),
    ),
)

class MultiInterfaceHiddenImplBareInterfaceFieldTest : MultiInterfaceHiddenImplTestBase(
    query = """
        query {
          containers {
            item {
              id
            }
          }
        }
    """.trimIndent(),
)
