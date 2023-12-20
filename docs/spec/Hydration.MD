# Hydration

Hydration is the process where references to pieces of data are resolved.
Think of it like SQL joins but in GraphQL land.

# Example

```graphql
type Query {
  issueById(id: ID!): Issue
  userById(id: ID!): User
}
type Issue {
  id: ID!
  title: String
  assigneeId: ID
  assignee: User
  @hydrated(
    field: "userById"
    arguments: [{name: "id" value: "$source.assigneeId"}]
  )
}
type User {
  id: ID!
  name: String
  email: String
}
```

Where the `assignee` field is the hydrated field, and does not exist in any service.
It's an artificial field made up by Nadel that fetches data from `userById` once the issue is loaded.

# Terminology

Borrowing from the example above.

* `Issue.assignee` is the _hydration source_ field
* `Issue.assigneeId` is the _source input_ field
* `Query.userById` is the _fulfillment_ field.
* `Query.userById.id` is the _fulfillment_ argument.

# How it works

Given a query

```graphql
query {
  issueById(id: 1) {
    id
    title
    assignee {
      name
    }
  }
}
```

Nadel will transform the `assignee` field to replace it with the source input fields.

i.e. Nadel will send a query like

```patch
query {
  issueById(id: 1) {
    id
    title
    assigneeId
  }
}
```

Given a response

```json
{
  "data": {
    "issueById": {
      "id": "1",
      "title": "Write Hydration Docs",
      "assigneeId": "user-256"
    }
  }
}
```

Then Nadel will retrieve the source input field(s) i.e.

```json
{
  "assigneeId": "user-256"
}
```

Then execute the hydration query

```graphql
query {
  userById(userId: "user-256") {
    name
  }
}
```

Which say yields

```json
{
  "data": {
    "userById": {
      "name": "2^8"
    }
  }
}
```

Then Nadel will insert that into the original response

```json
{
  "data": {
    "issueById": {
      "id": "1",
      "title": "Write Hydration Docs",
      "assignee": {
        "name": "2^8"
      }
    }
  }
}
```

# Batched Hydration

So far we have observed non-batched hydration.

Batched hydration is where multiple IDs are resolved in one go.

All the source input field values are gathered together, split into multiple batches according to configuration, and
then sent down to the fulfillment service.

e.g.

```graphql
type Query {
  issueById(id: ID!): Issue
  usersByIds(id: ID!): [User]
}
type Issue {
  id: ID!
  title: String
  collaboratorIds: [ID]
  collaborators: [User]
  @hydrated(
    field: "usersByIds"
    arguments: [{name: "id" value: "$source.assigneeId"}]
    inputIdentifiedBy: [
      {sourceId: "collaboratorIds" resultId: "id"}
    ]
    batchSize: 10
  )
}
type User {
  id: ID!
  name: String
  email: String
}
```

All the `collaboratorIds` are resolved in one go using the `usersByIds` field.

e.g. given a query

```graphql
query {
  issueById(id: 1) {
    collaborators {
      name
    }
  }
}
```

and response

```json
{
  "data": {
    "issueById": {
      "collaboratorIds": [
        "user-256",
        "user-8",
        "user-64"
      ]
    }
  }
}
```

Then the fulfillment query would look like

```
```

# Index Hydration

# Considerations

## Abstract Types
