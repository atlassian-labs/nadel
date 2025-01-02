package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `solitary polymorphic hydration when hook returns null` :
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
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="pets", overallSchema="""
    |type Query {
    |  petById(id: ID): Pet
    |}
    |
    |type Pet {
    |  id: ID
    |  breed: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  petById(id: ID): Pet
    |}
    |
    |type Pet {
    |  id: ID
    |  breed: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
, Service(name="people", overallSchema="""
    |type Query {
    |  humanById(id: ID): Human
    |}
    |
    |type Human {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  humanById(id: ID): Human
    |}
    |
    |type Human {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("humanById") { env ->
          if (env.getArgument<Any?>("id") == "HUMAN-0") {
            People_Human(id = "HUMAN-0", name = "Fanny Longbottom")}
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
    |    service: "pets"
    |    field: "petById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.dataId"}
    |    ]
    |  )
    |  @hydrated(
    |    service: "people"
    |    field: "humanById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.dataId"}
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
          listOf(Foo_Foo(dataId = "NULL-0", id = "FOO-0"), Foo_Foo(dataId = "HUMAN-0", id =
              "FOO-1"))}
      }
    }
    )
)) {
  private data class Pets_Pet(
    public val id: String? = null,
    public val breed: String? = null,
  )

  private data class People_Human(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Foo_Foo(
    public val id: String? = null,
    public val dataId: String? = null,
  )
}
