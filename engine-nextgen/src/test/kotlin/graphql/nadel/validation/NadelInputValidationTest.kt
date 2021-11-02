package graphql.nadel.validation

import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import io.kotest.core.spec.style.DescribeSpec

class NadelInputValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if all input values exist in underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6
                            M4
                            M3
                            M5
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
                            pay(role: Role): Int
                        }
                        input Role {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6
                            M4
                            M3
                            M5
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
            assert(errors.map { it.message }.isEmpty())
        }

        it("passes if input type was renamed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role @renamed(from: "PayFilter") {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        scalar Nothing
                        type Query {
                            nothing: Nothing
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            pay(role: PayFilter): Int
                        }
                        input PayFilter {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("fails if input field in renamed type does not exist in underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role @renamed(from: "PayFilter") {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        scalar Nothing
                        type Query {
                            nothing: Nothing
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            pay(role: PayFilter): Int
                        }
                        input PayFilter {
                            p: Professional
                            pm: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingUnderlyingInputField>()
            assert(error.service.name == "test")
            assert(error.parentType.overall.name == "Role")
            assert(error.parentType.underlying.name == "PayFilter")
            assert(error.overallField.name == "m")
            assert(error.subject == error.overallField)
        }

        it("fails if input field does not exist in underlying type") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to renameDirectiveDef,
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            p: Professional
                            m: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        scalar Nothing
                        type Query {
                            nothing: Nothing
                        }
                    """.trimIndent(),
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            p: Professional
                            pm: Manager
                        }
                        enum Manager {
                            M6 M4 M3 M5
                        }
                        enum Professional {
                            P5 P4 P6 P3
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.singleOfType<MissingUnderlyingInputField>()
            assert(error.service.name == "test")
            assert(error.parentType.overall.name == "Role")
            assert(error.parentType.underlying.name == error.parentType.overall.name)
            assert(error.overallField.name == "m")
            assert(error.subject == error.overallField)
        }
    }
})
