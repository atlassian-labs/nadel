# Nadel Coding Pitfalls

Here we document common mistakes made in Nadel changes. This is _not_ a comprehensive list - so don't assume your change
is bug free just because you've considered all the issues below.

1. [Not using execution hints](#not-using-execution-hints)
2. [Not considering all object type names](#not-considering-all-object-type-names)
3. [Forgetting about object type names after transforming](#forgetting-about-object-type-names-after-transforming)
4. [Deleting a field and leaving an empty selection set](#deleting-a-field-and-leaving-an-empty-selection-set)
5. [Not testing enough around renames and hydrations etc.](#not-testing-enough-around-renames-and-hydrations-etc)

## Not using execution hints

If your change is risky or large, you should consider using an execution hint where possible. This is Nadel API for
feature flagging a change.

e.g.

```kotlin
data class NadelExecutionHints constructor(
    val legacyOperationNames: LegacyOperationNamesHint,
)

fun interface LegacyOperationNamesHint {
    /**
     * Determines whether to use the old `nadel_2_{service}_{operationName}` operation names
     * for the given service.
     *
     * @param service the service we want to determine whether to send legacy operation names to.
     * @return true to use the old format
     */
    operator fun invoke(service: Service): Boolean
}
```

Where the Nadel consumer provides the implementation of `LegacyOperationNamesHint` which can be a feature flag etc.

## Not considering all object type names

For `ExecutableNormalizedField.objectTypeNames` make sure you consider _all_ values. It's almost always a mistake to
consider just the first object type name. Each type name can have a slightly different definition e.g.

```graphql
type Query {
    pet: Pet
}
interface Pet {
    name: String
    friends: [Pet]
}
type Dog implements Pet {
    name: String @renamed(from: "doggo")
    friends: [Dog]
}
type Cat implements Pet {
    name: String
    friends: [Cat]
}

# Given a query
query MyPet {
    pet {
        name
        friends {
            name
        }
    }
}
```

The given query will generate a normalised query:

```
NF(
    objectTypeNames = ["Query"]
    children = [
        NF(
            objectTypeNames = ["Dog", "Cat"]
            name = "name"
        )
        NF(
            objectTypeNames = ["Dog", "Cat"]
            name = "friends"
            children = [
                NF(
                    objectTypeNames = ["Dog", "Cat"]
                    name = "name"
                )
            ]
        )
    ]
)
```

You'll notice that what was `Pet.name` is now `[Dog, Cat].name` where each `name` definition is different
i.e. `Dog.name` has a `@renamed` directive but `Cat.name` does not.

The same with `Pet.friends` which resolves to `[Dog, Cat].friends` where `Dog.friends` has an output type of `[Dog]`
but `Cat.friends` has an output type of `[Cat]` instead.

When in doubt, it's always safer to play it safe and simply consider all types.

## Forgetting about object type names after transforming

Say you are given a schema

```graphql
type Query {
    pet: Pet
}
type Pet {
    name: String
}
type Dog implements Pet {
    name: String @renamed(from: "doggo")
}
type Pet implements Pet {
    name: String
}

# Given a query
query MyPet {
    pet {
        name
    }
}
```

You need to make sure you code creates

```graphql
query MyPet {
    pet {
        ... on Dog {
            doggo
        }
        ... on Cat {
            name
        }
    }
}
```

A mistake here would be to code the rename transform and forget about the non-special types.

i.e. `Pet.name` will resolve to `[Dog, Cat].name` but you can't just throw away `Cat.name` after you
transform `Dog.name`

A malformed query would be

```graphql
query MyPet {
    pet {
        ... on Dog {
            doggo
        }
    }
}
```

Where if your pet was a `Cat` then you would receive an empty object.

## Deleting a field and leaving an empty selection set

_Note_ this is something that can be detected and fixed by the Nadel engine, in the future this may no longer be a
problem.

Consider the following code

```kotlin
suspend fun transformField(
    executionContext: NadelExecutionContext,
    transformer: NadelQueryTransformer,
    service: Service,
    field: ExecutableNormalizedField,
    state: State,
): NadelTransformFieldResult {
    return NadelTransformFieldResult(newField = null)
}
```

Could you spot what potential problems could arise?

Say you were given a query

```graphql
query MyPet {
    pet {
        name
    }
}
```

If you were to delete the `name` field then the selection set for `pet` would now be empty.

```graphql
query MyPet {
    pet
}
```

We cannot call the underlying service with an empty selection set.

We generally elect to insert a `__typename` field when deleting a field to ensure the selection set is never empty.

## Not testing enough around renames and hydrations etc.

Your custom transform may have affect the subsequent transforms. Always make sure you test some combination of

1. Your transform with a renamed field as the parent
2. Your transform with a renamed field in the child selection set
3. Your transform with a hydrated field as the parent
4. Your transform with a hydrated field in the child selection set
5. Your transform _on_ a renamed field.
5. Your transform _on_ a hydrated field.

etc. Where applicable.
