package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.union

/**
 * `__typename` at the union level is relaxed (sent bare) just like on an
 * interface, and a hidden member's node is stripped to `{}`. Anything else on a union needs an explicit
 * `... on Member` fragment and is never relaxed.
 */

/** `actors { __typename }` goes bare; the hidden `Bot` is stripped to `{}`. */
class UnionHiddenMemberRelaxedBareTypenameTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actors {
            __typename
          }
        }
    """.trimIndent(),
)

/** `... on User { id }`: explicit member fragment, unchanged (honoured verbatim); the hidden `Bot` node → `{}`. */
class UnionHiddenMemberRelaxedExplicitMemberTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actors {
            ... on User {
              id
            }
          }
        }
    """.trimIndent(),
)
