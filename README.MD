# Nadel: A GraphQL Stitching DSL 

This is a Java library to combine several graphql services together (called stitching).

This is achieved by declaring the services, which should be stitched together, in a DSL which 
is build on top of the GraphQL SDL (Schema Definition Language). 

While this library is in Java, the underlying services only need to provide a standard
GraphQL API and no Java Code is needed to stitch services together. It is all done declarative as part of the DSL.


Example:

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
        authorID: ID => author: User
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

The result is a new GraphQL API which combines the two services in one and has the following schema.

```graphql
type Query {
    posts: [Post]
    user(id: ID!): User
}       
type Post {
    id: ID!
    content: String
    author: User
}
type User {
    id: ID!
    name: String
}
``` 