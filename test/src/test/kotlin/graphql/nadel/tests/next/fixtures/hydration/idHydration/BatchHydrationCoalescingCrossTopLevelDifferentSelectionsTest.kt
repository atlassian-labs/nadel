package graphql.nadel.tests.next.fixtures.hydration.idHydration

/**
 * Independently executing source services request different Identity selections. The two backing
 * roots must still share one bounded Identity operation without merging their selections.
 */
class BatchHydrationCoalescingCrossTopLevelDifferentSelectionsTest :
    BatchHydrationCoalescingCrossTopLevelServicesBase(
        query = """
            query {
              issue {
                assignee {
                  displayName
                }
              }
              page {
                owner {
                  email
                }
              }
            }
        """.trimIndent(),
    )
