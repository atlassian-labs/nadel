package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

import graphql.nadel.NadelExecutionHints

/**
 * The hiddenmembership scenario with the hint ON: same schema + queries as [HiddenImplementationTestBase], only
 * the hint flipped, so the snapshot diff against the hint-OFF tests is the fix. Relaxed selections go bare
 * downstream and non-exposed nodes are stripped to `{}`, so the client-facing result is unchanged.
 */

/** The fix: `nodes { id }` goes bare downstream; the hidden `Task` still reduces to `{}`. */
class HiddenImplRelaxedBareInterfaceFieldTest : HiddenImplementationTestBase(
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

/** Naming every exposed impl normalizes to the bare selection, so it relaxes too (result-identical). */
class HiddenImplRelaxedExplicitAllExposedImplsTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            ... on Issue {
              id
            }
            ... on Story {
              id
            }
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}

/** `__typename` is relaxed too; the hidden node is stripped after the type-rename transform (this runs last). */
class HiddenImplRelaxedBareTypenameTest : HiddenImplementationTestBase(
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

/** Over-return protection: an explicit single-impl selection is not relaxed (would leak `Story` data). */
class HiddenImplRelaxedExplicitExposedImplTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            ... on Issue {
              issueField
            }
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}
