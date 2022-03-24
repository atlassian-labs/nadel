# Field Renames

Here we take a deep dive into _field_ renames in Nadel. We will consider everything from the validation, blueprint, to
its transform.

Note this is talking about how an internal transform works. Custom transforms outside the Nadel codebase will not store
information in the blueprint. For now, we don't have a great mechanism for transforms to cache information beforehand.
We may start feeding in the schema & services into the transform so that upon instantiation transforms can precompute
information too.

## Declaration

A rename has a simple GraphQL declaration.

```graphql
directive @renamed(from: String) on FIELD_DEFINITION

# Example usage in User.nadel
type User {
    id: ID @renamed(from: "identifier")
}

# in User.graphqls
type User {
    id: ID
}
```

At runtime this is parsed into:

```kotlin
data class FieldMappingDefinition(
    val inputPath: List<String>,
)
```

## Validation

A rename is a Nadel feature that is _not_ built into the GraphQL spec, that means we have to do custom validation
because GraphQL Java won't do it for us.

So here we need to inspect that the `from` argument references a valid field in the underlying schema.

Remember from the [primer](./Primer.MD) that validation collects type pairs for overall and underlying schema.

So given the schema:

```graphql
# in Issues.nadel
type Dog { #ref1
    name: String @renamed(from: "doggo")
}

# in Issues.graphqls
type Dog { #ref2
    doggo: String
}
```

We expect the type pair

```
NadelServiceSchemaElement(service=Issues, overall=Dog#ref1, underlying=Dog#ref2)
```

And the validation will on that to check whether all fields in Dog#ref1 with a `@renamed` directive reference an
existing field in Dog#ref2 and whether those fields are compatible.

Here is the actual code from `NadelRenameValidation` but with lots more documentation.

```kotlin
internal class NadelRenameValidation(
    private val fieldValidation: NadelFieldValidation,
) {
    // This is invoked by NadelFieldValidation for all fields
    fun validate(
        // This would be the NadelServiceSchemaElement type pair we described earlier
        parent: NadelServiceSchemaElement,
        // This is the Dog#ref1.name field definition
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        // Hydrations and renames cannot be declared on the same field
        if (hasHydration(overallField)) {
            return listOf(
                CannotRenameHydratedField(parent, overallField),
            )
        }

        // This gets the `FieldMappingDefinition` runtime representation we mentioned earlier
        val rename = getRename(overallField)

        // If there are no fields there are no errors for this specific validation
        return if (rename == null) {
            listOf()
        } else {
            // This gets Dog#ref2
            val underlyingFieldContainer = parent.underlying as GraphQLFieldsContainer
            // This checks that Dog#ref2 actually has a field at "doggo"
            val underlyingField = underlyingFieldContainer.getFieldAt(rename.inputPath)
            // If there is no field, then we return an error
            if (underlyingField == null) {
                listOf(
                    MissingRename(parent, overallField, rename),
                )
            } else {
                // We need to validate that the fields are compatible
                // e.g. we can't have name: String @renamed(from: "friend")
                // where friend is an object type Dog but name is a scalar String
                fieldValidation.validate(parent, overallField, underlyingField)
            }
        }
    }
}
```

## Blueprint

So a `@renamed` is stored in the [blueprint](./Primer.MD#blueprint-creation) as a `NadelFieldInstruction`.

```kotlin
sealed class NadelFieldInstruction {
    abstract val location: FieldCoordinates
}
data class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction() {
    val overallName: String get() = location.fieldName
}
```

So the `NadelExecutionBlueprintFactory` invokes a function `makeFieldInstruction` which checks if `@renamed` exists.

Here is the code but with more comments.

```kotlin
private fun makeFieldInstruction(
    type: NadelServiceSchemaElement,
    field: GraphQLFieldDefinition,
): List<NadelFieldInstruction> {
    // Try to create the runtime object for the applied directive @renamed - returns null if there is no @renamed
    val mappingDefinition = getFieldMappingDefinition(field)
    if (mappingDefinition != null) {
        return listOf(
            when (mappingDefinition.inputPath.size) {
                1 -> makeRenameInstruction(type, field, mappingDefinition)
                else -> makeDeepRenameFieldInstruction(type, field, mappingDefinition)
            },
        )
    }

    // Hydrations work the same way
    val hydrations = getUnderlyingServiceHydrations(field)
    if (hydrations.isNotEmpty()) {
        return hydrations
            .map {
                makeHydrationFieldInstruction(type, field, it)
            }
    }

    return emptyList()
}
```

Note that we also allow `@renamed(from: "collar.name")` where you can reach "deeper" and extract a value from inside the
collar object for the rename. We call this a deep rename.

The result of `makeFieldInstruction` is then stored in a `Map` that associates field instructions by their location. The
location of a field is a pair of its parent type and field name. This pair uniquely identifies the field in a schema
object, and is referred to as the field coordinates or location.

## Transform

For `NadelRenameTransform` let's take a look at how it works.

First we begin with the `isApplicable` transform.

[ref1 - Forgetting about object type names](./Pitfalls.MD#forgetting-about-object-type-names-after-transforming)

```kotlin
// So there's quite a few parameters here, you don't need to worry about them all
// There are additional parameters here just for completeness / so you have all the info you need
// But the main parameters you should care about are overallField and service
override suspend fun isApplicable(
    executionContext: NadelExecutionContext,
    services: Map<String, Service>,
    service: Service, // This is the service we're going to send the request to
    overallField: ExecutableNormalizedField, // This is the field we're going to inspect for a @renamed
    hydrationDetails: ServiceExecutionHydrationDetails?,
): State? {
    // We look at the blueprint for this service and check whether the overall field has any rename instructions
    val renameInstructions: Map<GraphQLObjectTypeName, NadelRenameFieldInstruction> =
        service.blueprint.fieldInstructions
            // The generics here is reified and is used to filter the instructions to only that type
            .getTypeNameToInstructionMap<NadelRenameFieldInstruction>(overallField)

    // Do nothing
    if (renameInstructions.isEmpty()) {
        return null
    }

    // This gets the object type names in the field without a @renamed on it see ref1 above
    val objectsWithoutRename = overallField.objectTypeNames
        .asSequence()
        .filterNot { it in renameInstructions }
        .toHashSet()

    // Return a state object that is passed to both transformField and transformResult
    return State(
        renameInstructions,
        objectsWithoutRename,
        NadelAliasHelper.forField(tag = "rename", overallField),
        overallField,
        service,
    )
}
```

And here is the function to transform the field

```kotlin
// Again, quite a few parameters here for completeness, only worry about field and state
override suspend fun transformField(
    executionContext: NadelExecutionContext,
    transformer: NadelQueryTransformer,
    service: Service,
    field: ExecutableNormalizedField,
    state: State,
): NadelTransformFieldResult {
    return NadelTransformFieldResult(
        newField = if (state.objectTypesWithoutRename.isNotEmpty()) {
            field.toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(field.objectTypeNames.filter { it in state.objectTypesWithoutRename })
                .build()
        } else {
            null
        },
        artificialFields = makeRenamedFields(state, transformer, field).let {
            when (val typeNameField = makeTypeNameField(state, field)) {
                null -> it
                else -> it + typeNameField
            }
        },
    )
}
```
