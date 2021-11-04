package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes
import graphql.nadel.validation.util.assertSingleOfType
import io.kotest.core.spec.style.DescribeSpec

class NadelInterfaceValidationTest : DescribeSpec({
    describe("validate") {
        it("passes if there are valid implementations of the interface") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                            name: String
                            age: Int
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                            name: String
                            age: Int
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("it fails if there are no concrete implementations of interface") {
            // Although this is technically valid schema
            // The AGG cannot send down queries to your underlying service without a concrete type
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingConcreteTypes>()
            assert(error.interfaceType.overall.name == "Entity")
            assert(error.interfaceType.service.name == "entities")
        }

        it("it passes if shared interfaces have concrete types on all services") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Table implements Entity {
                            id: ID!
                            location: Coordinates
                            sides: Int
                            legs: Int
                        }
                    """.trimIndent(),
                    "humans" to """
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    // Has no implementations
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Table implements Entity {
                            id: ID!
                            location: Coordinates
                            sides: Int
                            legs: Int
                        }
                    """.trimIndent(),
                    "humans" to """
                        type Query {
                            echo: String
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                            name: String
                            age: Int
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }

        it("it fails if shared interface does not have concrete implementation in one service") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                    "humans" to """
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    // Has no implementations
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                    "humans" to """
                        type Query {
                            echo: String
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                            name: String
                            age: Int
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            val error = errors.assertSingleOfType<MissingConcreteTypes>()
            assert(error.interfaceType.overall.name == "Entity")
            assert(error.interfaceType.service.name == "entities")
        }

        it("it fails if shared interface does not have concrete implementation in multiple service") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "shared" to """
                        type QueryError {
                            errors: [QueryErrorExtension]
                        }
                        interface QueryErrorExtension {
                            message: String
                        }
                    """.trimIndent(),
                    "humans" to """
                        type Query {
                            human(id: ID!): HumanResult
                        }
                        union HumanResult = Human | QueryError
                        type Human {
                            id: ID!
                        }
                    """.trimIndent(),
                    "cats" to """
                        type Query {
                            cat(id: ID!): CatResult
                        }
                        union CatResult = Cat | QueryError
                        type Cat {
                            id: ID!
                        }
                    """.trimIndent(),
                    "dogs" to """
                        type Query {
                            dog(id: ID!): DogResult
                        }
                        union DogResult = Dog | QueryError
                        type Dog {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "shared" to """
                        type Query {
                            echo: String
                        }
                    """.trimIndent(),
                    "humans" to """
                        type QueryError {
                            errors: [QueryErrorExtension]
                        }
                        interface QueryErrorExtension {
                            message: String
                        }
                        type Query {
                            human(id: ID!): HumanResult
                        }
                        union HumanResult = Human | QueryError
                        type Human {
                            id: ID!
                        }
                    """.trimIndent(),
                    "cats" to """
                        type QueryError {
                            errors: [QueryErrorExtension]
                        }
                        interface QueryErrorExtension {
                            message: String
                        }
                        type MissingCatErrorExtension implements QueryErrorExtension {
                            message: String
                            lastSeenAt: String
                        }
                        type Query {
                            cat(id: ID!): CatResult
                        }
                        union CatResult = Cat | QueryError
                        type Cat {
                            id: ID!
                        }
                    """.trimIndent(),
                    "dogs" to """
                        type QueryError {
                            errors: [QueryErrorExtension]
                        }
                        interface QueryErrorExtension {
                            message: String
                        }
                        type Query {
                            dog(id: ID!): DogResult
                        }
                        union DogResult = Dog | QueryError
                        type Dog {
                            id: ID!
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isNotEmpty())

            // Cats is the only valid service
            assert(errors.filterIsInstance<MissingConcreteTypes>()
                .map { it.interfaceType.service.name }
                .none { it == "cats" })

            val error = errors.assertSingleOfType<MissingConcreteTypes> { it.interfaceType.service.name == "dogs" }
            assert(error.interfaceType.overall.name == "QueryErrorExtension")

            val error2 = errors.assertSingleOfType<MissingConcreteTypes> { it.interfaceType.service.name == "humans" }
            assert(error2.interfaceType.overall.name == "QueryErrorExtension")
        }

        it("it does not fail if there is concrete implementation but not exposed") {
            val fixture = NadelValidationTestFixture(
                overallSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                    """.trimIndent(),
                ),
                underlyingSchema = mapOf(
                    "entities" to """
                        type Query {
                            entity(id: ID!): Entity
                        }
                        scalar Coordinates
                        interface Entity {
                            id: ID!
                            location: Coordinates
                        }
                        type Human implements Entity {
                            id: ID!
                            location: Coordinates
                            name: String
                            age: Int
                        }
                    """.trimIndent(),
                ),
            )

            val errors = validate(fixture)
            assert(errors.map { it.message }.isEmpty())
        }
    }
})
