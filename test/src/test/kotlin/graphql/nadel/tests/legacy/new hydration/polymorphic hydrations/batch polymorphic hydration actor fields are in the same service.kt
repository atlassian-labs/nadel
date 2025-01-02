package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `batch polymorphic hydration actor fields are in the same service` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    __typename
|    id
|    data {
|      ... on Pet {
|        __typename
|        id
|        breed
|      }
|      ... on Human {
|        __typename
|        id
|        name
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="foo", overallSchema="""
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
    |  )
    |  @hydrated(
    |    service: "bar"
    |    field: "humanById"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.dataId"}
    |    ]
    |  )
    |}
    |
    |union Data = Pet | Human
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
, Service(name="bar", overallSchema="""
    |type Query {
    |  petById(ids: [ID]): [Pet]
    |  humanById(ids: [ID]): [Human]
    |}
    |
    |type Human {
    |  id: ID
    |  name: String
    |}
    |
    |type Pet {
    |  id: ID
    |  breed: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  petById(ids: [ID]): [Pet]
    |  humanById(ids: [ID]): [Human]
    |}
    |
    |type Pet {
    |  id: ID
    |  breed: String
    |}
    |
    |type Human {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("petById") { env ->
          if (env.getArgument<Any?>("ids") == listOf("PET-0", "PET-1")) {
            listOf(Bar_Pet(breed = "Akita", id = "PET-0"), Bar_Pet(breed = "Labrador", id =
                "PET-1"))}
          else {
            null}
        }

        .dataFetcher("humanById") { env ->
          if (env.getArgument<Any?>("ids") == listOf("HUMAN-0", "HUMAN-1")) {
            listOf(Bar_Human(id = "HUMAN-0", name = "Fanny Longbottom"), Bar_Human(id = "HUMAN-1",
                name = "John Doe"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Foo_Foo(
    public val id: String? = null,
    public val dataId: String? = null,
  )

  private data class Bar_Human(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Bar_Pet(
    public val id: String? = null,
    public val breed: String? = null,
  )
}
