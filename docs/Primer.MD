# Nadel

Nadel can be broken up into these components:

1. [Validation](#validation)
2. [Execution](#Execution)
    1. [Normalised queries](#normalised-queries)
    2. [Blueprint creation](#blueprint-creation)
    3. [Transform API](#transform-api)
    4. [Execution planning](#execution-planning)
    5. [Query transforming](#query-transforming)
    6. [Result transforming](#result-transforming)

This document will attempt to give a primer on all these components.

## Validation

Nadel adds functionality on top of a normal GraphQL schema via directives. The Nadel schema must also make sense in
terms of the services Nadel is built on. Schemas must be validated so that Nadel has a sane schema to work with.

Validation starts in `graphql.nadel.validation.NadelSchemaValidation` where you must provide an overall schema and the
services. You can look at the tests for an up to date way to provide these objects.

Validation focuses on:

1. Ensuring all types in the Nadel schema exist in a service's underlying schema.
2. Ensuring all fields in the Nadel schema exist in the service's underlying type.
3. Ensuring enum values in the Nadel schema exist in the service's underlying enum type.
5. Ensuring field output types are compatible.
6. Ensuring renamed fields target a valid and existing field.
7. Ensuring hydrated fields are properly declared e.g. they reference a valid field to hydrate from etc.

etc.

### Shared Types

One thing to understand before diving into validation, is that types can be shared. For example:

```graphql
# in Users.nadel
type Query { #ref1
    user(id: ID!): User
}
type User { #ref2
    id: ID!
}

# in Issues.nadel
type Query { #ref3
    issue(id: ID!): Issue
}
type Issue { #ref4
    id: ID!
    assignee: User
}
```

The `Users` service defines a `User` type and that type is also used in the `Issues` service. In that case, we need to
validate that `User` is valid against both the `Users` and `Issues` services.

That is, the underlying service's schemas must be:

```graphql
# in Users.graphqls
type Query { #ref5
    user(id: ID!): User
}
type User { #ref6
    id: ID!
}

# in Issues.graphqls
type Query { # ref7
    issue(id: ID!): Issue
}
type Issue { #ref8
    id: ID!
    assignee: User
}
type User { #ref9
    id: ID!
}
```

So the first thing that the validation does, is pick up pairs of types. So in our example schema the pairs would be

```
NadelServiceSchemaElement(service=Users, overall=Query#ref1, underlying=Query#ref5)
NadelServiceSchemaElement(service=Users, overall=User#ref2, underlying=User#ref6)

NadelServiceSchemaElement(service=Issues, overall=User#ref2, underlying=User#ref9)
NadelServiceSchemaElement(service=Issues, overall=Query#ref3, underlying=Query#ref7)
NadelServiceSchemaElement(service=Issues, overall=Issue#ref4, underlying=Issue#ref8)
```

To do this, it traverses the `Issues.nadel` and `Users.nadel` files and picks up all types defined _and_
all types _referenced_ and finds their underlying schema equivalent.

Then it will iterate all these services and validate their type pairs and everything else. Aside from these details, the
code should itself be relatively straight forward, so best to visit the actual code and the tests for more details.

## Execution

To execute a request, you must have a `Nadel` instance. This document will not talk about how to actually create one.

Upon creating a Nadel instance, blueprints are created per service to store information about transformations on the
schema.

The general overview is that a GraphQL query `Document` will be traversed and a `Map` is created associating the GraphQL
fields to relevant transforms. The query is then transformed and fields are fed to the relevant transforms. Once the
transformed query is compiled, it is sent to the underlying service. The result is then transformed and fed back to the
caller.

### Normalised queries

Nadel operates on a concept called normalised queries. At runtime, a query `String` is parsed into a GraphQL
Java `Document` for validation. Upon successful validation, a normalised version of it is generated. This concept is
borrowed from
[GraphQL Java](https://github.com/graphql-java/graphql-java/blob/470811bb8bc6864035d7a3795a1a23d6a4952587/src/main/java/graphql/normalized/ExecutableNormalizedField.java)
.

Very simply put the construct is as follows:

```kotlin
data class NF(
    val objectTypeNames: Set<String>,
    val name: String,
    val alias: String?,
    val arguments: List,
    val children: List<NF>,
) {
    // Recursive structure needs the parent set afterwards
    lateinit var parent: NF
}
```

When working with this representation, the main difference is that the type conditions are resolved to object types and
are explicitly stated in `objectTypeNames`. Arguments are also resolved, so that variables are no longer a concern.

#### Normalised query example

```graphql
# Given the schema
type Query {
    pet: Pet
}
interface Pet {
    name: String
}
type Dog implements Pet {
    name: String @renamed(from: "doggo")
    collar: String
}
type Cat implements Pet {
    name: String
    meow: Boolean
}

# And given these queries
query MyPet {
    pet {
        name
        ... on Dog {
            collar
        }
        ... on Cat {
            meow
        }
    }
}

query MyDogName {
    pet {
        ... on Dog {
            name
        }
    }
}
```

The `MyPet` query wil generate:

```
NF(
   objectTypeNames = ["Query"]
   name = "pet"
   children = [
      NF(
         objectTypeNames = ["Cat", "Dog"]
         name = "name"
      )
      NF(
         objectTypeNames = ["Dog"]
         name = "collar"
      )
      NF(
         objectTypeNames = ["Cat"]
         name = "meow"
      )
   ]
)
```

And the `MyDogName` query will generate:

```
NF(
   objectTypeNames = ["Query"]
   name = "pet"
   children = [
      NF(
         objectTypeNames = ["Dog"]
         name = "name"
      )
   ]
)
```

Hopefully you're starting to see how it works.

This representation forces us to consider the abstract types properly as there can be multiple field definitions
depending on which `objectTypeNames` value you are using for the parent type. It is also very explicit about which type
conditions a field has.

For example in the query `Pet.name` is selected, and in the normalised form this is resolved to `[Dog, Cat].name` which
is important as we needed to consider `Dog.name` as it has a `@renamed` transform.

### Blueprint creation

In Nadel, a blueprint refers to the construct that stores information about the schema e.g. renames, hydrations etc.

Similar to validation, the blueprint factory will collect pairs of types that each service owns.
See [shared types](#shared-types). For services and their collected types, Nadel will construct an object holding all
information relevant to executing that transform e.g.

```graphql
type User {
    friend: User @hydrated(
        field: "users.friendById"
        arguments: [{ name: "id" value: "$source.friendId" }]
        timeout: 1000
    )
}
```

Is transformed to:

```kotlin
data class NadelHydrationFieldInstruction(
    override val location: FieldCoordinates, // User.friend
    val hydratedFieldDef: GraphQLFieldDefinition, // Schema definition for User.friend
    val backingService: Service, // Users service i.e. service owning users.friendById
    val queryPathToBackingField: NadelQueryPath, // users.friendById
    val backingFieldDef: GraphQLFieldDefinition, // Schema definition for users.friendById
    val actorInputValueDefs: List<NadelHydrationActorInputDef>, // [{ name: "ids" value: "$source.friendId" }]
    val timeout: Int, // 1000ms
    val sourceFields: List<NadelQueryPath>, // friendId
    val hydrationStrategy: NadelHydrationStrategy,
) : NadelFieldInstruction()
```

Which is then stored in the blueprint:

```kotlin
data class NadelServiceBlueprint internal constructor(
    val service: Service,
    val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    val typeRenames: NadelTypeRenameInstructions,
)
```

This is done to precompute information to save time during a request and as an extra layer that everything is valid.

### Transform API

The transform API currently has three functions:

1. Check whether the transform should run on a given field
2. Transform the field
3. Transform the result

In the next sections, we'll go over these in depth.

### Execution planning

The first function lets you return a non-null object if the transform should run. This object is then passed along to
steps 2 and 3 as a way to hold state. Here you can check whether the transform is applicable for the field e.g. check
whether the `GraphQLFieldDefinition`s for `overallField` have a specific directive or type etc.

If a non-null object is returned then an entry is created in the execution plan for this field, this transform and the
returned state object.

e.g.

```kotlin
data class State(
    val fieldsWithDirective: List<GraphQLFieldDefinition>,
    val fieldsNoDirective: List<GraphQLFieldDefinition>,
)

suspend fun isApplicable(
    executionContext: NadelExecutionContext,
    services: Map<String, Service>,
    service: Service,
    overallField: ExecutableNormalizedField,
): State? {
    val (fieldsWithDirective, fieldsNoDirective) = overallField.getFieldDefinitions(service.schema).partition {
        it.hasAppliedDirective("MyDirective")
    }
    // Do nothing if no fields have the special directive i.e. return null
    if (fieldsWithDirective.isEmpty()) {
        return null
    }
    // Activate the transform by returning something not-null, store relevant information
    return State(fieldsWithDirective, fieldsNoDirective)
}
```

### Query transforming

Once the transform is applicable for a field, we transform the field.

Here you have a few options:

1. Do nothing
2. Modify the `field` e.g. change arguments, field name, type conditions etc.
3. Delete the field
4. Ask for more fields

This lets you do practically anything with the field.

Note that any fields returned under `NadelTransformFieldResult.artificialFields` are automatically removed by Nadel
after the result is processed. As such, they should have a unique name. This name is usually generated
by `NadelAliasHelper`.

If you delete a field, then the subsequent transforms are _not_ run.

There are currently some weird rules regarding how a field should be referenced i.e. should I return a field in relation
to the overall or underlying schema? That should change for the better in the near future…

e.g.

```kotlin
suspend fun transformField(
    executionContext: NadelExecutionContext,
    transformer: NadelQueryTransformer,
    service: Service,
    field: ExecutableNormalizedField,
    state: State,
): NadelTransformFieldResult {
    // Option 1 - do nothing
    return NadelTransformFieldResult(newField = field)
    // Option 2 - modify field
    return NadelTransformFieldResult(newField = field.toBuilder().name("somethingElse").build())
    // Option 3 - delete field
    return NadelTransformFieldResult(newField = null)
    // Option 4 - ask for more information on the same object e.g. type name
    return NadelTransformFieldResult(
        newField = field,
        artificialFields = listOf(
            newNormalizedField()
                .objectTypeNames(field.objectTypeNames)
                .alias(aliasHelper.typeNameResultKey)
                .name("__typename")
                .build()
        ),
    )
}
```

### Result transforming

Lastly, is the capacity to transform the result.

Note that the `isApplicable` affects the information given to the transform. The transform result function is invoked
with the field from the original query and its parent field in the query sent to the underlying service.

One point to note is that the field's parent could have been transformed and that we cannot use the result keys of the
original field as a way to visit the result e.g.

```graphql
# Given a query
query MyUser {
    user {
        friends { # Say is defined as friends: [User] @renamed(from: "acquaintances")
            name # We want to transform this field
        }
    }
}

# Then the query to the underlying service could be
query MyUser {
    user {
        acquaintances_someAlias: acquaintances { # See how user.friends.name will no longer be valid in the result
            name
        }
    }
}
```

So the `underlyingParentField` parameter aims to solve this by supplying `user.acquaintances_someAlias` to the
transform.

e.g. given a response from a service

```json
{
  "data": {
    "user": {
      "acquaintances_someAlias": [
        {
          "name": "Franklin Wang"
        },
        {
          "name": "Artyom Emelyanenko"
        }
      ]
    }
  }
}
```

Then the following implementation will transform all `user.friends.name` fields to `"Hello World"`.

```kotlin
override suspend fun getResultInstructions(
    executionContext: NadelExecutionContext,
    service: Service,
    overallField: ExecutableNormalizedField, // user.friend.name
    underlyingParentField: ExecutableNormalizedField?, // user.acquaintance_someAlias
    result: ServiceExecutionResult, // Above JSON
    state: State,
    nodes: JsonNodes, // Use this to visit the response
): List<NadelResultInstruction> {
    val parentNodes = nodes.getNodesAt(
        queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root, // May be null if it's a top level field
        flatten = true,
    )

    // Note: parentNodes is a List because there may be arrays involved e.g. user.friendS.name can have multiple names
    return parentNodes.map { parentNode ->
        NadelResultInstruction.Set(
            subjectPath = parentNode.resultPath + overallField.resultKey,
            newValue = "Hello World",
        )
    }
}
```

The final result will be

```json
{
  "data": {
    "user": {
      "friends": [
        {
          "name": "Hello World"
        },
        {
          "name": "Hello World"
        }
      ]
    }
  }
}
```

If you do not wish to modify the result, simply return `emptyList()`.
