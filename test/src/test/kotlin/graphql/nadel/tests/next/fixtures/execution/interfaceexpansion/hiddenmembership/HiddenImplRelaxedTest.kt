package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

/**
 * Relaxed selections go bare downstream and non-exposed nodes are stripped to `{}`, so the client-facing result
 * is unchanged.
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
)

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
)

/** `__typename` is relaxed too; the hidden node is stripped after the type-rename transform (this runs last). */
class HiddenImplRelaxedBareTypenameTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)

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
)
