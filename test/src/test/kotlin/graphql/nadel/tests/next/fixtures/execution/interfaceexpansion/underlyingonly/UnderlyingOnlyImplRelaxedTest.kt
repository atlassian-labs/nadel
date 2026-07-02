package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.underlyingonly

import graphql.nadel.NadelExecutionHints

/**
 * The underlyingonly scenario with the hint ON: same schema + queries as [UnderlyingOnlyImplementationTestBase],
 * only the hint flipped.
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
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}

class UnderlyingOnlyImplRelaxedBareTypenameTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}
