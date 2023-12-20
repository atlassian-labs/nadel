* [Hydration](#hydration)
* [Example](#example)
* [Terminology](#terminology)
* [How it works](#how-it-works)
* [Directive Definition](#directive-definition)
* [Batched Hydration](#batched-hydration)
    * [Batch Size](#batch-size)
    * [Dimensions](#dimensions)
    * [Object Matching](#object-matching)
* [Index Hydration](#index-hydration)
* [Conditional Hydration](#conditional-hydration)
    * [Source Inputs](#source-inputs)
* [Argument Sources](#argument-sources)
* [Considerations](#considerations)
    * [Abstract Types](#abstract-types)

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

Where the `Issue.assignee` field is the hydrated field, and does not exist in any service.
It's an artificial field made up by Nadel that is backed by the `userById` field.

# Terminology

Borrowing from the example above.

* `Issue` is the `hydration source` type or object if referring to an instance.
* `Issue.assignee` is the _hydration source_ field.
* `Issue.assigneeId` is the _source input_ field.
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

```graphql
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

For full example, refer to test file
[spec-how-it-works-example.yml](/test/src/test/resources/fixtures/hydration/spec/spec-how-it-works-example.yml)

# Directive Definition

The most up-to-date definition is found
in [NadelDirectives.kt](/lib/src/main/java/graphql/nadel/schema/NadelDirectives.kt)

```graphql
"This allows you to hydrate new values into fields"
directive @hydrated(
  "The target service"
  service: String!
  "The target top level field"
  field: String!
  "How to identify matching results"
  identifiedBy: String! = "id"
  "How to identify matching results"
  inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
  "Are results indexed"
  indexed: Boolean! = false
  "Is querying batched"
  batched: Boolean! = false
  "The batch size"
  batchSize: Int! = 200
  "The timeout to use when completing hydration"
  timeout: Int! = -1
  "The arguments to the hydrated field"
  arguments: [NadelHydrationArgument!]!
  "Specify a condition for the hydration to activate"
  when: NadelHydrationCondition
) repeatable on FIELD_DEFINITION
```

# Batched Hydration

So far we have observed non-batched hydration.

Batched hydration is where multiple IDs are resolved in one go.

All the source input field values are gathered together, split into multiple batches according to configuration, and
then sent down to the fulfillment service.

The fulfilled objects are then pulled out of the response according to the `inputIdentifiedBy` i.e. objects are resolved
where the `sourceId` matches the `resultId` value.

e.g.

```graphql
type Query {
  issueById(id: ID!): Issue
  usersByIds(id: [ID!]!): [User]
}
type Issue {
  id: ID!
  title: String
  collaboratorIds: [ID]
  collaborators: [User]
  @hydrated(
    field: "usersByIds"
    arguments: [{name: "id" value: "$source.collaboratorIds"}]
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

```graphql
query {
  usersByIds(ids: ["user-256", "user-8", "user-64"]) {
    id
    name
  }
}
```

Notice that Nadel will inject the `id` field per the `inputIdentifiedBy` configuration.
This is required as the result _may not be ordered_. Nadel uses the `id` field to match the
`collaboratorIds` to know which object to put where.

And say the result is

```json
{
  "data": {
    "usersByIds": [
      {
        "id": "user-256",
        "name": "2^8"
      },
      {
        "id": "user-64",
        "name": "2^6"
      }
    ]
  }
}
```

Then the final response is

```json
{
  "data": {
    "issueById": {
      "collaborators": [
        {
          "name": "2^8"
        },
        null,
        {
          "name": "2^6"
        }
      ]
    }
  }
}
```

For full example, refer to test file
[spec-batch-hydration-example.yml](/test/src/test/resources/fixtures/hydration/spec/spec-batch-hydration-example.yml)

## Dimensions

Nadel can support hydrating multiple source objects and multiple source inputs.

e.g.

Here we have a schema with one or multiple source objects via the `Query.topIssue` (one) or `Query.myIssues` (many)
fields.
Then for source inputs we have either `Issue.assigneeId` (one) `Issue.collaboratorIds` (many).

```graphql
type Query {
  topIssue: Issue
  myIssues(n: Int! = 10): [Issue]

  usersByIds(ids: [ID]!): [User]
}
type Issue {
  assigneeId: ID
  assignee: User
  @hydrated(
    field: "usersByIds"
    arguments: [{name: "ids" value: "$source.assigneeId"}]
  )

  collaboratorIds: [ID!]
  collaborators: [User]
  @hydrated(
    field: "usersByIds"
    arguments: [{name: "ids" value: "$source.collaboratorIds"}]
  )
}
```

Any combination of source objects and source inputs will work i.e. all four queries below are valid

```graphql
query OneIssueOneAssignee {
  topIssue { assignee { name } }
}
query OneIssueManyCollaborators {
  topIssue { collaborators { name } }
}
query ManyIssuesOneAssignee {
  myIssues { assignee { name } }
}
query ManyIssuesManyCollaborators {
  myIssues { collaborators { name } }
}
```

For full examples, refer to the test files

* [spec-batch-hydration-example-many-issues-many-collaborators.yml](/test/src/test/resources/fixtures/hydration/spec/spec-batch-hydration-example-many-issues-many-collaborators.yml)
* [spec-batch-hydration-example-many-issues-one-assignee.yml](/test/src/test/resources/fixtures/hydration/spec/spec-batch-hydration-example-many-issues-one-assignee.yml)
* [spec-batch-hydration-example-one-issue-many-collaborators.yml](/test/src/test/resources/fixtures/hydration/spec/spec-batch-hydration-example-one-issue-many-collaborators.yml)
* [spec-batch-hydration-example-one-issue-one-assignee.yml](/test/src/test/resources/fixtures/hydration/spec/spec-batch-hydration-example-one-issue-one-assignee.yml)

## Batch Size

## Object Matching

# Index Hydration

# Conditional Hydration

## Source Inputs

# Argument Sources

# Considerations

## Abstract Types
