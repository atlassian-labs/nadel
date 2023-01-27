package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.NamespacedTypeMustBeObject
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec

class NadelNamespaceValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if namespaced type is an object") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        directive @namespaced on FIELD_DEFINITION
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery @namespaced
                        }
                        type IssueQuery {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery
                        }
                        type IssueQuery {
                            issue(id: ID!): Issue
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails if namespaced type is an interface") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        directive @namespaced on FIELD_DEFINITION
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery @namespaced
                        }
                        interface IssueQuery {
                            issue(id: ID!): Issue
                        }
                        type RealIssueQuery implements IssueQuery {
                            issue(id: ID!): Issue!
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery
                        }
                        interface IssueQuery {
                            issue(id: ID!): Issue
                        }
                        type RealIssueQuery implements IssueQuery {
                            issue(id: ID!): Issue!
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<NamespacedTypeMustBeObject>()
            assert(error.subject.name == "IssueQuery")
        }

        it("fails if namespaced type is an enum") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        directive @namespaced on FIELD_DEFINITION
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery @namespaced
                        }
                        enum IssueQuery {
                            ISSUE,
                            ISSUES,
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "issues" to """
                        type Query {
                            issues: IssueQuery
                        }
                        enum IssueQuery {
                            ISSUE,
                            ISSUES,
                        }
                        type Issue {
                            id: ID!
                            title: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<NamespacedTypeMustBeObject>()
            assert(error.subject.name == "IssueQuery")
        }
    }
})
