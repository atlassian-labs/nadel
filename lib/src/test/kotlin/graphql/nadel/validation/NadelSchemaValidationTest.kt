package graphql.nadel.validation

import graphql.nadel.NadelSchemas.Companion.newNadelSchemas
import io.kotest.core.spec.style.DescribeSpec

class NadelSchemaValidationTest : DescribeSpec({
    describe("validate") {
        it("takes in overall schema and services to validate") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.isEmpty())
        }
    }
})

fun validate(fixture: NadelValidationTestFixture): Set<NadelSchemaValidationError> {
    return NadelSchemaValidation(
        newNadelSchemas()
            .overallSchemas(fixture.overallSchema)
            .underlyingSchemas(fixture.underlyingSchema)
            .stubServiceExecution()
            .build()
    ).validate()
}
