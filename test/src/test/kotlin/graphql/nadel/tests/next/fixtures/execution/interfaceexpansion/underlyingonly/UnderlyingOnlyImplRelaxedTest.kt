package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.underlyingonly

/**
 * Covers relaxed abstract selections when an implementation exists only in the underlying schema.
 */

/** The fix: `nodes { id }` goes bare downstream; the underlying-only `Secret` is stripped to `{}`. */
class UnderlyingOnlyImplRelaxedBareInterfaceFieldTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class UnderlyingOnlyImplRelaxedBareTypenameTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
