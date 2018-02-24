# Nadel: A GraphQL Stitching DSL (WORK IN PROGRESS)

This is a Java library to combine several graphql services together (called stitching).

This is achieved by declaring the services, which should be stitched together, in a DSL which 
is build on top of the GraphQL SDL (Schema Definition Language). 

While this library is in Java, the underlying services only need to provide a standard
GraphQL API and no Java Code is needed to stitch services together. It is all done declarative as part of the DSL.


## Example:

Lets assume we have two Services: "Posts" and "User". One provides some kind of blog posts and the other
is the user management service. Every Post references a User via ID.

This is how you would combine these two services together with Nadel:

Nadel DSL Example:
```
service Posts {
    url: "http://posts-service"
    
    type Query{
       posts: [Post]
    }
    type Post {
        id: ID!
        content: String
        authorID: ID => author: User // This transforms the authorId from a Post into a full User field
    }
}

service Users {
    url: "http://user-service"
    
    type Query {
        user(id: ID!): User
    }
    type User {
        id: ID!
        name: String
    }
}

```

The result is a new GraphQL API which combines the two services in one and has the following schema:

```graphql
// Combines the queries of the underlying services
type Query {
    posts: [Post]
    user(id: ID!): User
}       
type Post {
    id: ID!
    content: String
    author: User // The author is internally resolved by the user service 
}
type User {
    id: ID!
    name: String
}
``` 

## Usage

You create a `Nadel` instance and execute queries on it:

```java
        // A GraphqlCaller actually executes a request against a service
        GraphqlCallerFactory graphqlCallFactory = ....  
        String dsl = "<Nadel stitching DSL>";
        Nadel nadel = new Nadel(dsl, callerFactory);
        String query = "<some query on the stitched schema>"
        ExecutionResult executionResult = nadel.executeAsync(ExecutionInput.newExecutionInput().query(query).build()).get()
```

## Library Boundaries

This library doesn't deal with JSON parsing or any form of (HTTP) connections.

## DSL design 

The main element of the DSL is a service definition:

```service MyService {
    url: "http://example.com/graphql"
    type Query {
        ....
    } 
}
```
The url should point to the graphql endpoint of the service.
The rest of service must be valid GraphQL SDL elements: object type definitions, interfaces etc

### Query fields are merged 
Every service has to define a `Query` object type, which describes the top level type of the service.
All fields of all `Query` types get merged together in the `Query` of the stitched schema.

### Type Ownership and type extension (TODO)
Every type is owned by specifically one service. Other services can extend the types.

```
service UserService {
    ...
    type User {
        id: String
        name: String
    }
}

service OtherService {

    extend type User {
        otherUserField => {}
    }
    
}

```

## Field Transformations
It is possible to combine two services together through a specific field and a "field transformation":

```
service Posts {
    ...
    type Post {
        ...
        authorID: ID => author: User 
    }
}

service Users {
    ...
    type User {
        id: ID!
        ...
    }
}

```


