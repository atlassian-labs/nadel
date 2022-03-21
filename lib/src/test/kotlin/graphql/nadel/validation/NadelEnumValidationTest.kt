package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingEnumValue
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec

class NadelEnumValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if all enum values exist in underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            myRank: Professional
                        }
                        enum Professional {
                            P5
                            P4
                            P6
                            P3
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            myRank: Professional
                        }
                        enum Professional {
                            P3
                            P4
                            P5
                            P6
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isEmpty())
        }

        it("passes if enum was renamed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            myRank: Professional
                        }
                        enum Professional @renamed(from: "P") {
                            P5
                            P4
                            P6
                            P3
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            myRank: P
                        }
                        enum P {
                            P3
                            P4
                            P5
                            P6
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isEmpty())
        }

        it("fails if enum value does not exist in underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "promos" to """
                        $renameDirectiveDef
 
                        type Query {
                            myRank: Professional
                        }
                        enum Professional {
                            P2
                            P3
                            P4
                            P5
                            P6
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "promos" to """
                        type Query {
                            myRank: Professional
                        }
                        enum Professional {
                            P3
                            P4
                            P5
                            P6
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isNotEmpty())

            val error = errors.assertSingleOfType<MissingUnderlyingEnumValue>()
            assert(error.service.name == "promos")
            assert(error.overallValue.name == "P2")
        }
    }
})
