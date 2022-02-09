package graphql.nadel.validation

import graphql.nadel.enginekt.util.strictAssociateBy
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
    val nadel = fixture.toNadel()
    val services = nadel.services.strictAssociateBy { it.name }
    return NadelSchemaValidation(nadel.overallSchema, services).validate(newHydrationValidation = true)
}
