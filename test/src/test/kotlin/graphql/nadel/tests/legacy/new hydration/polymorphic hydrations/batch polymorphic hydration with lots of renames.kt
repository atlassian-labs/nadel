package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `batch polymorphic hydration with lots of renames` : NadelLegacyIntegrationTest(query =
    """
|query {
|  foo {
|    __typename
|    id
|    data {
|      ... on Animal {
|        __typename
|        id
|        breed
|      }
|      ... on Person {
|        __typename
|        id
|        name
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="bar", overallSchema="""
    |type Query {
    |  animalById(ids: [ID]): [Animal] @renamed(from: "petById")
    |  petById(ids: [ID]): [Animal]
    |  personById(ids: [ID]): [Person] @renamed(from: "humanById")
    |  humanById(ids: [ID]): [Person]
    |}
    |
    |type Animal @renamed(from: "Pet") {
    |  id: ID @renamed(from: "identifier")
    |  breed: String @renamed(from: "kind")
    |}
    |
    |type Person @renamed(from: "Human") {
    |  id: ID @renamed(from: "identifier")
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  humanById(ids: [ID]): [Human]
    |  petById(ids: [ID]): [Pet]
    |}
    |
    |type Human {
    |  hiddenId: ID
    |  identifier: ID
    |  name: String
    |}
    |
    |type Pet {
    |  hiddenId: ID
    |  identifier: ID
    |  kind: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("humanById") { env ->
          if (env.getArgument<Any?>("ids") == listOf("HUMAN-0", "HUMAN-1")) {
            listOf(Bar_Human(hiddenId = "HUMAN-0", identifier = "PERSON-0", name =
                "Fanny Longbottom"), Bar_Human(hiddenId = "HUMAN-1", identifier = "PERSON-1", name =
                "John Doe"))}
          else {
            null}
        }

        .dataFetcher("petById") { env ->
          if (env.getArgument<Any?>("ids") == listOf("PET-0", "PET-1")) {
            listOf(Bar_Pet(hiddenId = "PET-0", identifier = "ANIMAL-0", kind = "Akita"),
                Bar_Pet(hiddenId = "PET-1", identifier = "ANIMAL-1", kind = "Labrador"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="foo", overallSchema="""
    |type Query {
    |  foo: [Foo]
    |}
    |
    |type Foo {
    |  id: ID
    |  dataId: ID
    |  data: Data
    |  @hydrated(
    |    service: "bar"
    |    field: "petById"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.dataId"}
    |    ]
    |    identifiedBy: "hiddenId"
    |  )
    |  @hydrated(
    |    service: "bar"
    |    field: "humanById"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.dataId"}
    |    ]
    |    identifiedBy: "hiddenId"
    |  )
    |}
    |
    |union Data = Animal | Person
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: [Foo]
    |}
    |
    |type Foo {
    |  id: ID
    |  dataId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          listOf(Foo_Foo(dataId = "PET-0", id = "FOO-0"), Foo_Foo(dataId = "HUMAN-0", id = "FOO-1"),
              Foo_Foo(dataId = "PET-1", id = "FOO-2"), Foo_Foo(dataId = "HUMAN-1", id = "FOO-3"))}
      }
    }
    )
)) {
  private data class Foo_Foo(
    public val id: String? = null,
    public val dataId: String? = null,
  )

  private data class Bar_Human(
    public val hiddenId: String? = null,
    public val identifier: String? = null,
    public val name: String? = null,
  )

  private data class Bar_Pet(
    public val hiddenId: String? = null,
    public val identifier: String? = null,
    public val kind: String? = null,
  )
}
