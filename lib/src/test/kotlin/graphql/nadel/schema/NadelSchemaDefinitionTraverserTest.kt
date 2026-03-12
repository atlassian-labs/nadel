package graphql.nadel.schema

import graphql.nadel.util.AnyNamedNode
import graphql.nadel.util.AnySDLDefinition
import graphql.parser.Parser
import graphql.schema.idl.TypeUtil
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NadelSchemaDefinitionTraverserTest {
    @Test
    fun `traverse visits object types and their fields`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> name (FieldDefinition)",
            "Query (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "Query (ObjectType) -> user (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
            "User (ObjectType)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "User (ObjectType) -> name (FieldDefinition)",
            "User (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits interface type and its fields`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Node (InterfaceType)",
            "Node (InterfaceType) -> id (FieldDefinition)",
            "Node (InterfaceType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "Node (InterfaceType) -> title (FieldDefinition)",
            "Node (InterfaceType) -> title (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType)",
            "Query (ObjectType) -> node (FieldDefinition)",
            "Query (ObjectType) -> node (FieldDefinition) -> Node (TypeReference)",
            "Query (ObjectType) -> node (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> node (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
            "User (ObjectType)",
            "User (ObjectType) -> Node (TypeReference)",
            "User (ObjectType) -> email (FieldDefinition)",
            "User (ObjectType) -> email (FieldDefinition) -> String (TypeReference)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "User (ObjectType) -> title (FieldDefinition)",
            "User (ObjectType) -> title (FieldDefinition) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits field arguments`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> echo (FieldDefinition)",
            "Query (ObjectType) -> echo (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> echo (FieldDefinition) -> message (FieldArgument)",
            "Query (ObjectType) -> echo (FieldDefinition) -> message (FieldArgument) -> String (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "Query (ObjectType) -> user (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> limit (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> limit (FieldArgument) -> Int (TypeReference)",
            "User (ObjectType)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits applied directives on fields`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    oldField: String @deprecated(reason: "use newField")
                    newField: String
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> newField (FieldDefinition)",
            "Query (ObjectType) -> newField (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> oldField (FieldDefinition)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> deprecated (AppliedDirective)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> deprecated (AppliedDirective) -> reason (AppliedDirectiveArgument)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits applied directive arguments`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    oldField: String @deprecated(reason: "obsolete")
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> oldField (FieldDefinition)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> deprecated (AppliedDirective)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> deprecated (AppliedDirective) -> reason (AppliedDirectiveArgument)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits input object type and its fields`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> createUser (FieldDefinition)",
            "Query (ObjectType) -> createUser (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> createUser (FieldDefinition) -> input (FieldArgument)",
            "Query (ObjectType) -> createUser (FieldDefinition) -> input (FieldArgument) -> UserInput! (TypeReference)",
            "User (ObjectType)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "User (ObjectType) -> name (FieldDefinition)",
            "User (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
            "UserInput (InputObjectType)",
            "UserInput (InputObjectType) -> email (InputObjectField)",
            "UserInput (InputObjectType) -> email (InputObjectField) -> String (TypeReference)",
            "UserInput (InputObjectType) -> name (InputObjectField)",
            "UserInput (InputObjectType) -> name (InputObjectField) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits enum type and enum values`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> status (FieldDefinition)",
            "Query (ObjectType) -> status (FieldDefinition) -> Status (TypeReference)",
            "Status (EnumType)",
            "Status (EnumType) -> ACTIVE (EnumValueDefinition)",
            "Status (EnumType) -> INACTIVE (EnumValueDefinition)",
            "Status (EnumType) -> PENDING (EnumValueDefinition)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits custom scalar types`() {
        val document = Parser().parseDocument(
            """
                scalar DateTime
                scalar JSON
                type Query {
                    createdAt: DateTime
                    metadata: JSON
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "DateTime (ScalarType)",
            "JSON (ScalarType)",
            "Query (ObjectType)",
            "Query (ObjectType) -> createdAt (FieldDefinition)",
            "Query (ObjectType) -> createdAt (FieldDefinition) -> DateTime (TypeReference)",
            "Query (ObjectType) -> metadata (FieldDefinition)",
            "Query (ObjectType) -> metadata (FieldDefinition) -> JSON (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits custom directive and its arguments`() {
        val document = Parser().parseDocument(
            """
                directive @custom(description: String) on FIELD_DEFINITION
                type Query {
                    foo: String @custom(description: "bar")
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> foo (FieldDefinition)",
            "Query (ObjectType) -> foo (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> foo (FieldDefinition) -> custom (AppliedDirective)",
            "Query (ObjectType) -> foo (FieldDefinition) -> custom (AppliedDirective) -> description (AppliedDirectiveArgument)",
            "custom (Directive)",
            "custom (Directive) -> description (DirectiveArgument)",
            "custom (Directive) -> description (DirectiveArgument) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits schema directive definition and directive arguments`() {
        val document = Parser().parseDocument(
            """
                directive @auth(role: String!) on FIELD_DEFINITION
                type Query {
                    adminOnly: String @auth(role: "admin")
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> adminOnly (FieldDefinition)",
            "Query (ObjectType) -> adminOnly (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> adminOnly (FieldDefinition) -> auth (AppliedDirective)",
            "Query (ObjectType) -> adminOnly (FieldDefinition) -> auth (AppliedDirective) -> role (AppliedDirectiveArgument)",
            "auth (Directive)",
            "auth (Directive) -> role (DirectiveArgument)",
            "auth (Directive) -> role (DirectiveArgument) -> String! (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse full schema accumulates all expected coordinate kinds`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "DateTime (ScalarType)",
            "Node (InterfaceType)",
            "Node (InterfaceType) -> id (FieldDefinition)",
            "Node (InterfaceType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "Query (ObjectType)",
            "Query (ObjectType) -> deprecatedField (FieldDefinition)",
            "Query (ObjectType) -> deprecatedField (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> deprecatedField (FieldDefinition) -> deprecated (AppliedDirective)",
            "Query (ObjectType) -> deprecatedField (FieldDefinition) -> deprecated (AppliedDirective) -> reason (AppliedDirectiveArgument)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "Query (ObjectType) -> user (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
            "Role (EnumType)",
            "Role (EnumType) -> ADMIN (EnumValueDefinition)",
            "Role (EnumType) -> USER (EnumValueDefinition)",
            "User (ObjectType)",
            "User (ObjectType) -> Node (TypeReference)",
            "User (ObjectType) -> createdAt (FieldDefinition)",
            "User (ObjectType) -> createdAt (FieldDefinition) -> DateTime (TypeReference)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "User (ObjectType) -> name (FieldDefinition)",
            "User (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
            "UserInput (InputObjectType)",
            "UserInput (InputObjectType) -> name (InputObjectField)",
            "UserInput (InputObjectType) -> name (InputObjectField) -> String (TypeReference)",
            "custom (Directive)",
            "custom (Directive) -> msg (DirectiveArgument)",
            "custom (Directive) -> msg (DirectiveArgument) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits union type and its member types`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Node (UnionType)",
            "Node (UnionType) -> Post (TypeReference)",
            "Node (UnionType) -> User (TypeReference)",
            "Post (ObjectType)",
            "Post (ObjectType) -> id (FieldDefinition)",
            "Post (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "Post (ObjectType) -> title (FieldDefinition)",
            "Post (ObjectType) -> title (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType)",
            "Query (ObjectType) -> node (FieldDefinition)",
            "Query (ObjectType) -> node (FieldDefinition) -> Node (TypeReference)",
            "User (ObjectType)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "User (ObjectType) -> name (FieldDefinition)",
            "User (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse with explicit roots only traverses from specified roots`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .mapNotNull {
                if (it is AnyNamedNode && it.name == "Query") {
                    NadelSchemaDefinitionTraverserElement.from(it as AnySDLDefinition)
                } else {
                    null
                }
            }

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "Query (ObjectType) -> user (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(
            roots = roots,
            visitor = Visitor(collectTraversed),
        )

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `traverse visits object type with multiple interfaces`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Identifiable (InterfaceType)",
            "Identifiable (InterfaceType) -> id (FieldDefinition)",
            "Identifiable (InterfaceType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "Item (ObjectType)",
            "Item (ObjectType) -> Identifiable (TypeReference)",
            "Item (ObjectType) -> Named (TypeReference)",
            "Item (ObjectType) -> id (FieldDefinition)",
            "Item (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
            "Item (ObjectType) -> name (FieldDefinition)",
            "Item (ObjectType) -> name (FieldDefinition) -> String (TypeReference)",
            "Named (InterfaceType)",
            "Named (InterfaceType) -> name (FieldDefinition)",
            "Named (InterfaceType) -> name (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType)",
            "Query (ObjectType) -> item (FieldDefinition)",
            "Query (ObjectType) -> item (FieldDefinition) -> Item (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(roots, Visitor(collectTraversed))

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `returning false from visitGraphQLObjectType short-circuits traversal of that type and its fields`() {
        val document = Parser().parseDocument(
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

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "Query (ObjectType) -> user (FieldDefinition) -> User (TypeReference)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument)",
            "Query (ObjectType) -> user (FieldDefinition) -> id (FieldArgument) -> ID! (TypeReference)",
            "User (ObjectType)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(
            roots,
            object : Visitor(collectTraversed) {
                override fun visitGraphQLObjectType(element: NadelSchemaDefinitionTraverserElement.ObjectType): Boolean {
                    super.visitGraphQLObjectType(element)
                    return element.node.name == "Query"
                }
            },
        )

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `returning false from visitGraphQLFieldDefinition short-circuits traversal of field arguments and return type`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    user(id: ID!, limit: Int): User
                }
                type User {
                    id: ID!
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> user (FieldDefinition)",
            "User (ObjectType)",
            "User (ObjectType) -> id (FieldDefinition)",
            "User (ObjectType) -> id (FieldDefinition) -> ID! (TypeReference)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(
            roots,
            object : Visitor(collectTraversed) {
                override fun visitGraphQLFieldDefinition(element: NadelSchemaDefinitionTraverserElement.FieldDefinition): Boolean {
                    super.visitGraphQLFieldDefinition(element)
                    return !((element.parent.node as AnyNamedNode).name == "Query" && element.node.name == "user")
                }
            },
        )

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    @Test
    fun `returning false from visitGraphQLAppliedDirective short-circuits traversal of applied directive arguments`() {
        val document = Parser().parseDocument(
            """
                type Query {
                    oldField: String @deprecated(reason: "obsolete")
                }
            """.trimIndent()
        )

        val roots = document.definitions
            .filterIsInstance<AnySDLDefinition>()
            .mapNotNull(NadelSchemaDefinitionTraverserElement::from)

        val traversedElements = mutableListOf<String>()
        val collectTraversed = fun(element: NadelSchemaDefinitionTraverserElement) {
            traversedElements.add(mapToString(element))
        }

        val expectedElements = listOf(
            "Query (ObjectType)",
            "Query (ObjectType) -> oldField (FieldDefinition)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> String (TypeReference)",
            "Query (ObjectType) -> oldField (FieldDefinition) -> deprecated (AppliedDirective)",
        )

        // When
        NadelSchemaDefinitionTraverser().traverse(
            roots,
            object : Visitor(collectTraversed) {
                override fun visitGraphQLAppliedDirective(element: NadelSchemaDefinitionTraverserElement.AppliedDirective): Boolean {
                    super.visitGraphQLAppliedDirective(element)
                    return false
                }
            },
        )

        // Then
        traversedElements.sort()
        assertTrue(traversedElements == expectedElements)
    }

    private fun mapToString(element: NadelSchemaDefinitionTraverserElement): String {
        fun selfToString(element: NadelSchemaDefinitionTraverserElement): String {
            return when (element) {
                is NadelSchemaDefinitionTraverserElement.AppliedDirective -> element.node.name + " (AppliedDirective)"
                is NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument -> element.node.name + " (AppliedDirectiveArgument)"
                is NadelSchemaDefinitionTraverserElement.Directive -> element.node.name + " (Directive)"
                is NadelSchemaDefinitionTraverserElement.DirectiveArgument -> element.node.name + " (DirectiveArgument)"
                is NadelSchemaDefinitionTraverserElement.EnumType -> element.node.name + " (EnumType)"
                is NadelSchemaDefinitionTraverserElement.EnumValueDefinition -> element.node.name + " (EnumValueDefinition)"
                is NadelSchemaDefinitionTraverserElement.FieldArgument -> element.node.name + " (FieldArgument)"
                is NadelSchemaDefinitionTraverserElement.FieldDefinition -> element.node.name + " (FieldDefinition)"
                is NadelSchemaDefinitionTraverserElement.InputObjectField -> element.node.name + " (InputObjectField)"
                is NadelSchemaDefinitionTraverserElement.InputObjectType -> element.node.name + " (InputObjectType)"
                is NadelSchemaDefinitionTraverserElement.InterfaceType -> element.node.name + " (InterfaceType)"
                is NadelSchemaDefinitionTraverserElement.ObjectType -> element.node.name + " (ObjectType)"
                is NadelSchemaDefinitionTraverserElement.ScalarType -> element.node.name + " (ScalarType)"
                is NadelSchemaDefinitionTraverserElement.TypeReference -> TypeUtil.simplePrint(element.node) + " (TypeReference)"
                is NadelSchemaDefinitionTraverserElement.UnionType -> element.node.name + " (UnionType)"
            }
        }

        val parent = element.parent
        return if (parent == null) {
            selfToString(element)
        } else {
            mapToString(parent) + " -> " + selfToString(element)
        }
    }

    private fun toCodeString(elements: List<String>): String {
        val dq = "\""
        val strings = elements
            .joinToString("\n") {
                "$dq$it$dq,"
            }
            .replaceIndent("  ")

        return "listOf(\n$strings\n)"
    }

    private open class Visitor(
        private val onVisit: (NadelSchemaDefinitionTraverserElement) -> Unit,
    ) : NadelSchemaDefinitionTraverserVisitor {
        override fun visitGraphQLArgument(element: NadelSchemaDefinitionTraverserElement.Argument): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLUnionType(element: NadelSchemaDefinitionTraverserElement.UnionType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLInterfaceType(element: NadelSchemaDefinitionTraverserElement.InterfaceType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLEnumType(element: NadelSchemaDefinitionTraverserElement.EnumType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLEnumValueDefinition(element: NadelSchemaDefinitionTraverserElement.EnumValueDefinition): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLFieldDefinition(element: NadelSchemaDefinitionTraverserElement.FieldDefinition): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLInputObjectField(element: NadelSchemaDefinitionTraverserElement.InputObjectField): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLInputObjectType(element: NadelSchemaDefinitionTraverserElement.InputObjectType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLObjectType(element: NadelSchemaDefinitionTraverserElement.ObjectType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLScalarType(element: NadelSchemaDefinitionTraverserElement.ScalarType): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLDirective(element: NadelSchemaDefinitionTraverserElement.Directive): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLAppliedDirective(element: NadelSchemaDefinitionTraverserElement.AppliedDirective): Boolean {
            onVisit(element)
            return true
        }

        override fun visitGraphQLAppliedDirectiveArgument(element: NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument): Boolean {
            onVisit(element)
            return true
        }

        override fun visitTypeReference(element: NadelSchemaDefinitionTraverserElement.TypeReference): Boolean {
            onVisit(element)
            return true
        }
    }
}
