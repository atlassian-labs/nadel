package graphql.nadel.engine.blueprint

import graphql.nadel.definition.coordinates.NadelDirectiveCoordinates
import graphql.nadel.definition.coordinates.NadelEnumCoordinates
import graphql.nadel.definition.coordinates.NadelInputObjectCoordinates
import graphql.nadel.definition.coordinates.NadelInterfaceCoordinates
import graphql.nadel.definition.coordinates.NadelObjectCoordinates
import graphql.nadel.definition.coordinates.NadelScalarCoordinates
import graphql.nadel.definition.coordinates.NadelSchemaMemberCoordinates
import graphql.nadel.definition.coordinates.NadelUnionCoordinates
import graphql.schema.idl.SchemaGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelSchemaTraverserTest {
    @Test
    fun `traverse visits object types and their fields`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    user(id: ID!): User
                    name: String
                }
                type User {
                    id: ID!
                    name: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("Query").field("name"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits interface type and its fields`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    node(id: ID!): Node
                }
                interface Node {
                    id: ID!
                    title: String
                }
                type User implements Node {
                    id: ID!
                    title: String
                    email: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("node"),
            NadelObjectCoordinates("Query").field("node").argument("id"),
            NadelInterfaceCoordinates("Node"),
            NadelInterfaceCoordinates("Node").field("id"),
            NadelInterfaceCoordinates("Node").field("title"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("title"),
            NadelObjectCoordinates("User").field("email"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits field arguments`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    echo(message: String): String
                    user(id: ID!, limit: Int): User
                }
                type User {
                    id: ID!
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("echo"),
            NadelObjectCoordinates("Query").field("echo").argument("message"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("Query").field("user").argument("limit"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelScalarCoordinates("String"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("Int"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits applied directives on fields`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    oldField: String @deprecated(reason: "use newField")
                    newField: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        val deprecatedOnOldField = NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated")
        assertTrue(deprecatedOnOldField in traversedCoordinates)

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("oldField"),
            NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated"),
            NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated").argument("reason"),
            NadelObjectCoordinates("Query").field("newField"),
            NadelScalarCoordinates("String"),
        )

        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits applied directive arguments`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    oldField: String @deprecated(reason: "obsolete")
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("oldField"),
            NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated"),
            NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated").argument("reason"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits input object type and its fields`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    createUser(input: UserInput!): User
                }
                input UserInput {
                    name: String
                    email: String
                }
                type User {
                    id: ID!
                    name: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("createUser"),
            NadelObjectCoordinates("Query").field("createUser").argument("input"),
            NadelInputObjectCoordinates("UserInput"),
            NadelInputObjectCoordinates("UserInput").field("name"),
            NadelInputObjectCoordinates("UserInput").field("email"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits enum type and enum values`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    status: Status
                }
                enum Status {
                    ACTIVE
                    INACTIVE
                    PENDING
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("status"),
            NadelEnumCoordinates("Status"),
            NadelEnumCoordinates("Status").enumValue("ACTIVE"),
            NadelEnumCoordinates("Status").enumValue("INACTIVE"),
            NadelEnumCoordinates("Status").enumValue("PENDING"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits custom scalar types`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                scalar DateTime
                scalar JSON
                type Query {
                    createdAt: DateTime
                    metadata: JSON
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelScalarCoordinates("DateTime"),
            NadelScalarCoordinates("JSON"),
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("createdAt"),
            NadelObjectCoordinates("Query").field("metadata"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits custom directive and its arguments`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @custom(description: String) on FIELD_DEFINITION
                type Query {
                    foo: String @custom(description: "bar")
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelDirectiveCoordinates("custom"),
            NadelDirectiveCoordinates("custom").argument("description"),
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("foo"),
            NadelObjectCoordinates("Query").field("foo").appliedDirective("custom"),
            NadelObjectCoordinates("Query").field("foo").appliedDirective("custom").argument("description"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits schema directive definition and directive arguments`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @auth(role: String!) on FIELD_DEFINITION
                type Query {
                    adminOnly: String @auth(role: "admin")
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelDirectiveCoordinates("auth"),
            NadelDirectiveCoordinates("auth").argument("role"),
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("adminOnly"),
            NadelObjectCoordinates("Query").field("adminOnly").appliedDirective("auth"),
            NadelObjectCoordinates("Query").field("adminOnly").appliedDirective("auth").argument("role"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse full schema accumulates all expected coordinate kinds`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                directive @custom(msg: String) on FIELD_DEFINITION
                scalar DateTime
                type Query {
                    user(id: ID!): User
                    deprecatedField: String @deprecated(reason: "use user")
                }
                interface Node {
                    id: ID!
                }
                type User implements Node {
                    id: ID!
                    name: String
                    createdAt: DateTime
                }
                input UserInput {
                    name: String
                }
                enum Role {
                    ADMIN
                    USER
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelDirectiveCoordinates("custom"),
            NadelDirectiveCoordinates("custom").argument("msg"),
            NadelScalarCoordinates("DateTime"),
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("Query").field("deprecatedField"),
            NadelObjectCoordinates("Query").field("deprecatedField").appliedDirective("deprecated"),
            NadelObjectCoordinates("Query").field("deprecatedField").appliedDirective("deprecated").argument("reason"),
            NadelInterfaceCoordinates("Node"),
            NadelInterfaceCoordinates("Node").field("id"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelObjectCoordinates("User").field("createdAt"),
            NadelInputObjectCoordinates("UserInput"),
            NadelInputObjectCoordinates("UserInput").field("name"),
            NadelEnumCoordinates("Role"),
            NadelEnumCoordinates("Role").enumValue("ADMIN"),
            NadelEnumCoordinates("Role").enumValue("USER"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits union type and its member types`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    node: Node
                }
                union Node = User | Post
                type User {
                    id: ID!
                    name: String
                }
                type Post {
                    id: ID!
                    title: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("node"),
            NadelUnionCoordinates("Node"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelObjectCoordinates("Post"),
            NadelObjectCoordinates("Post").field("id"),
            NadelObjectCoordinates("Post").field("title"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse with explicit roots only traverses from specified roots`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    user(id: ID!): User
                }
                type User {
                    id: ID!
                    name: String
                }
                type Orphan {
                    x: Int
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()
        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelObjectCoordinates("User").field("name"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(
            schema = schema,
            roots = listOf("Query"),
            visitor = AccumulatingVisitor(traversedCoordinates),
        )

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `traverse visits object type with multiple interfaces`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    item: Item
                }
                interface Identifiable {
                    id: ID!
                }
                interface Named {
                    name: String
                }
                type Item implements Identifiable & Named {
                    id: ID!
                    name: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()

        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("item"),
            NadelInterfaceCoordinates("Identifiable"),
            NadelInterfaceCoordinates("Identifiable").field("id"),
            NadelInterfaceCoordinates("Named"),
            NadelInterfaceCoordinates("Named").field("name"),
            NadelObjectCoordinates("Item"),
            NadelObjectCoordinates("Item").field("id"),
            NadelObjectCoordinates("Item").field("name"),
            NadelScalarCoordinates("ID"),
            NadelScalarCoordinates("String"),
        )

        // When
        NadelSchemaTraverser().traverse(schema, AccumulatingVisitor(traversedCoordinates))

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `returning false from visitGraphQLObjectType short-circuits traversal of that type and its fields`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    user(id: ID!): User
                }
                type User {
                    id: ID!
                    name: String
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()
        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("Query").field("user").argument("id"),
            NadelObjectCoordinates("User"),
            NadelScalarCoordinates("ID"),
        )

        // When
        NadelSchemaTraverser().traverse(
            schema,
            object : AccumulatingVisitor(traversedCoordinates) {
                override fun visitGraphQLObjectType(element: NadelSchemaTraverserElement.ObjectType): Boolean {
                    super.visitGraphQLObjectType(element)
                    return element.node.name == "Query"
                }
            },
        )

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `returning false from visitGraphQLFieldDefinition short-circuits traversal of field arguments and return type`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    user(id: ID!, limit: Int): User
                }
                type User {
                    id: ID!
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()
        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("user"),
            NadelObjectCoordinates("User"),
            NadelObjectCoordinates("User").field("id"),
            NadelScalarCoordinates("ID"),
        )

        // When
        NadelSchemaTraverser().traverse(
            schema,
            object : AccumulatingVisitor(traversedCoordinates) {
                override fun visitGraphQLFieldDefinition(element: NadelSchemaTraverserElement.FieldDefinition): Boolean {
                    super.visitGraphQLFieldDefinition(element)
                    return !(element.coordinates().parent.name == "Query" && element.node.name == "user")
                }
            },
        )

        // Then
        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    @Test
    fun `returning false from visitGraphQLAppliedDirective short-circuits traversal of applied directive arguments`() {
        val schema = SchemaGenerator.createdMockedSchema(
            """
                type Query {
                    oldField: String @deprecated(reason: "obsolete")
                }
            """.trimIndent()
        )

        val traversedCoordinates = mutableSetOf<NadelSchemaMemberCoordinates>()
        val expectedCoordinates = setOf(
            NadelObjectCoordinates("Query"),
            NadelObjectCoordinates("Query").field("oldField"),
            NadelObjectCoordinates("Query").field("oldField").appliedDirective("deprecated"),
            NadelScalarCoordinates("String")
        )

        // When
        NadelSchemaTraverser().traverse(
            schema,
            object : AccumulatingVisitor(traversedCoordinates) {
                override fun visitGraphQLAppliedDirective(element: NadelSchemaTraverserElement.AppliedDirective): Boolean {
                    super.visitGraphQLAppliedDirective(element)
                    return false
                }
            },
        )

        assertTrue(traversedCoordinates == expectedCoordinates)
    }

    private open class AccumulatingVisitor(
        private val coordinates: MutableSet<NadelSchemaMemberCoordinates>,
    ) : NadelSchemaTraverserVisitor {
        override fun visitGraphQLArgument(element: NadelSchemaTraverserElement.Argument): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLUnionType(element: NadelSchemaTraverserElement.UnionType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLUnionMemberType(element: NadelSchemaTraverserElement.UnionMemberType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLInterfaceType(element: NadelSchemaTraverserElement.InterfaceType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLEnumType(element: NadelSchemaTraverserElement.EnumType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLEnumValueDefinition(element: NadelSchemaTraverserElement.EnumValueDefinition): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLFieldDefinition(element: NadelSchemaTraverserElement.FieldDefinition): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLInputObjectField(element: NadelSchemaTraverserElement.InputObjectField): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLInputObjectType(element: NadelSchemaTraverserElement.InputObjectType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLObjectType(element: NadelSchemaTraverserElement.ObjectType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLScalarType(element: NadelSchemaTraverserElement.ScalarType): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLDirective(element: NadelSchemaTraverserElement.Directive): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLAppliedDirective(element: NadelSchemaTraverserElement.AppliedDirective): Boolean {
            coordinates.add(element.coordinates())
            return true
        }

        override fun visitGraphQLAppliedDirectiveArgument(element: NadelSchemaTraverserElement.AppliedDirectiveArgument): Boolean {
            coordinates.add(element.coordinates())
            return true
        }
    }
}
