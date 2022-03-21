package graphql.nadel.validation

import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

class NadelInputValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if all input values exist in underlying type and input types match") {
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

        it("passes if all input values exist in underlying type and input types match accounting for rename") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: Boss
                        }
                        enum Boss @renamed(from: "Manager") {
                            M1
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: Manager
                        }
                        enum Manager {
                            M1
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

            val error = errors.assertSingleOfType<MissingUnderlyingInputField>()
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

            val error = errors.assertSingleOfType<MissingUnderlyingInputField>()
            assert(error.service.name == "test")
            assert(error.parentType.overall.name == "Role")
            assert(error.parentType.underlying.name == error.parentType.overall.name)
            assert(error.overallField.name == "m")
            assert(error.subject == error.overallField)
        }

        it("fails if input types dont match") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: Boss
                        }
                        enum Boss {
                            M1
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: Manager
                        }
                        enum Manager {
                            M1
                        }
                        enum Boss {
                            M1
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleFieldInputType>()
            assert(error.service.name == "test")
            assert(error.parentType.overall.name == "Role")
            assert(error.parentType.underlying.name == error.parentType.overall.name)
            assert(error.overallInputField.type.unwrapAll().name == "Boss")
            assert(error.underlyingInputField.type.unwrapAll().name == "Manager")
            assert(error.subject == error.overallInputField)
        }

        it("fails if overall input is less strict") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: String
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: String!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<IncompatibleFieldInputType>()
            assert(error.service.name == "test")
            assert(error.parentType.overall.name == "Role")
            assert(error.parentType.underlying.name == error.parentType.overall.name)
            assert(!error.overallInputField.type.isNonNull)
            assert(error.underlyingInputField.type.isNonNull)
            assert(error.subject == error.overallInputField)
        }

        it("passes if underlying input type is less strict") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: String!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: String
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("passes if underlying input type is less strict with more wrappings") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: [[String!]!]
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: [[String]]
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        context("fails if argument type list wrappings are not equal") {
            withData(
                nameFn = { (underlying, overall) -> "Underlying=$underlying, overall=$overall" },
                "String" to "[String]",
                "String!" to "[String]",
                "[String]" to "String",
                "[String]" to "String!",
                "[[String]]" to "[String]",
                "[String]" to "[[String]]",
                "[String!]" to "[[String]]",
                "[[String]]" to "[String!]",
            ) { (underlying, overall) ->
                val fixture = NadelValidationTestFixture(
                    overallSchema = mapOf(
                        "test" to """
                        $renameDirectiveDef
 
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: $overall
                        }
                    """.trimIndent(),
                    ),
                    underlyingSchema = mapOf(
                        "test" to """
                        type Query {
                            pay(role: Role): Int
                        }
                        input Role {
                            m: $underlying
                        }
                    """.trimIndent(),
                    ),
                )

                val errors = validate(fixture)

                val error = errors.assertSingleOfType<IncompatibleFieldInputType>()
                assert(error.service.name == "test")
                assert(error.parentType.overall.name == "Role")
                assert(error.parentType.underlying.name == error.parentType.overall.name)
                assert(error.overallInputField.type.unwrapAll().name == error.underlyingInputField.type.unwrapAll().name)
                assert(error.overallInputField.name == "m")
            }
        }
    }
})
