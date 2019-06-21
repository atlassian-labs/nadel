# WARNING: STILL WIP AND NOT RELEASED YET

# Nadel: A GraphQL DSL and execution engine for distributed schemas

This is a Java library to combine several graphql services together.

This is achieved by declaring the services, which should be combined, in a DSL which 
is build on top of the GraphQL SDL (Schema Definition Language). 

While this library is in Java, the underlying services only need to provide a standard
GraphQL API and no Java Code is needed to combine services together. It is all done declarative as part of the DSL.


## Example:

Lets assume we have two Services: "Issues" and "Users". One has Issues referencing Users. 

This is the schema for the Issues service:
```graphql
type Query {
    issues: [Issue]
}
type Issue {
    id: ID
    authorIds: [ID]
}
```

And this is the User service:
```graphql
type Query {
    usersById(id: [ID]): [User]
}
type User {
    id: ID
    fullName: String
}
```
This is how you would combine these two services together with Nadel while also renaming a field at the same time.

```
service Issues {
    type Query {
        issues: [Issue]
    }
    type Issue {
        id: ID
        authors: [User] => hydrated from UserService.usersById(id: $source.authorIds) object identified by id, batch size 10
    }
}
service User {
    type Query {
        usersById(id: [ID]): [User]
    }
    type User {
        id: ID
        name: String => renamed from $source.fullName
    }
}

```

The result is a new GraphQL API which combines the two services in one and has the following schema:

```graphql
type Query {
    issues: [Issue]
    usersById(id: [ID]): [User]
}       
type Issue {
    id: ID
    authors: [User]
}
type User {
    id: ID
    name: String
}
``` 

## Features

- Explicit overall schema design: every field for the overall schema needs to be declared in the Nadel DSL. If it doesnt' show up it will not be exposed
- Hydration: resolving referencing to other objects by calling another service
- Field rename: renaming field from the underlying schema to a new name in the overall schema
- Type rename: renaming a type from the underlying schema to a new name in the overall schema

