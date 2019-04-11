package graphql.nadel

import graphql.nadel.testutils.harnesses.IssuesCommentsUsersHarness
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput

/**
 * At this stage this is mostly a test of the harness we can use to build out more complex end to end use cases
 * for Nadel testing
 */
class NadelIssuesAndCommentsAndUsersTest extends Specification {

    def "basic execution with hydration"() {
        given:
        def query = '''
        {
            issues {
                key
                summary
                reporter {
                    displayName
                }
                comments {
                    commentText
                    author {
                        displayName
                    }
                }    
            }
        }
        '''

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(IssuesCommentsUsersHarness.serviceFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.errors.isEmpty()
        result.data == [issues: [
                [key     : "WORK-I1", summary: "Summary for WORK-I1", reporter: [displayName: "Display name of fred"],
                 comments: [[commentText: "Text of C1", author: [displayName: "Display name of fred"]],
                            [commentText: "Text of C3", author: [displayName: "Display name of zed"]],
                            [commentText: "Text of C5", author: [displayName: "Display name of fred"]]]],
                [key     : "WORK-I2", summary: "Summary for WORK-I2", reporter: [displayName: "Display name of zed"],
                 comments: [[commentText: "Text of C2", author: [displayName: "Display name of ned"]],
                            [commentText: "Text of C4", author: [displayName: "Display name of jed"]],
                            [commentText: "Text of C6", author: [displayName: "Display name of ted"]]]]],
        ]
    }
}
