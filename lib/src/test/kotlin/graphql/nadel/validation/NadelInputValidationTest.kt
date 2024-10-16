package graphql.nadel.validation

import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

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
            errors.map { it.message }.shouldBeEmpty()
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
            errors.map { it.message }.shouldBeEmpty()
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
            errors.map { it.message }.shouldBeEmpty()
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
            error.service.name.shouldBe("test")
            error.parentType.overall.name.shouldBe("Role")
            error.parentType.underlying.name.shouldBe("PayFilter")
            error.overallField.name.shouldBe("m")
            error.subject.shouldBe(error.overallField)
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
            error.service.name.shouldBe("test")
            error.parentType.overall.name.shouldBe("Role")
            error.parentType.underlying.name.shouldBe(error.parentType.overall.name)
            error.overallField.name.shouldBe("m")
            error.subject.shouldBe(error.overallField)
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
            error.service.name.shouldBe("test")
            error.parentType.overall.name.shouldBe("Role")
            error.parentType.underlying.name.shouldBe(error.parentType.overall.name)
            error.overallInputField.type.unwrapAll().name.shouldBe("Boss")
            error.underlyingInputField.type.unwrapAll().name.shouldBe("Manager")
            error.subject.shouldBe(error.overallInputField)
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
            error.service.name.shouldBe("test")
            error.parentType.overall.name.shouldBe("Role")
            error.parentType.underlying.name.shouldBe(error.parentType.overall.name)
            assert(!error.overallInputField.type.isNonNull)
            assert(error.underlyingInputField.type.isNonNull)
            error.subject.shouldBe(error.overallInputField)
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
            errors.map { it.message }.shouldBeEmpty()
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
            errors.map { it.message }.shouldBeEmpty()
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
                error.service.name.shouldBe("test")
                error.parentType.overall.name.shouldBe("Role")
                error.parentType.underlying.name.shouldBe(error.parentType.overall.name)
                error.overallInputField.type.unwrapAll().name.shouldBe(error.underlyingInputField.type.unwrapAll().name)
                error.overallInputField.name.shouldBe("m")
            }
        }
    }
})
