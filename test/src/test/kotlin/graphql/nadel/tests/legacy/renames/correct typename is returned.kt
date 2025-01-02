package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class `correct typename is returned` : NadelLegacyIntegrationTest(query = """
|query {
|  typenameTest {
|    __typename
|    object {
|      __typename
|    }
|    objects {
|      __typename
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello: World
    |  renameObject: ObjectOverall @renamed(from: "renameObjectUnderlying") # the field is renamed
    |  renameInterface: InterfaceOverall @renamed(from: "renameInterfaceUnderlying")
    |  renameUnion: UnionOverall @renamed(from: "renameUnionUnderlying")
    |  renameInput(arg1: InputOverall!, arg2: URL, arg3: EnumOverall): String
    |  renameString: String @renamed(from: "renameStringUnderlying")
    |  typenameTest: TypenameTest
    |}
    |type World {
    |  id: ID
    |  name: String
    |}
    |type ObjectOverall implements InterfaceOverall @renamed(from: "ObjectUnderlying") {
    |  name: String
    |}
    |interface InterfaceOverall @renamed(from: "InterfaceUnderlying") {
    |  name: String
    |}
    |union UnionOverall @renamed(from: "UnionUnderlying") = X | Y
    |type X @renamed(from: "XUnderlying") {
    |  x: Int
    |}
    |type Y @renamed(from: "YUnderlying") {
    |  y: Int
    |}
    |input InputOverall @renamed(from: "InputUnderlying") {
    |  inputVal: String
    |}
    |scalar URL @renamed(from: "String")
    |enum EnumOverall @renamed(from: "EnumUnderlying") {
    |  X, Y
    |}
    |type TypenameTest {
    |  object: ObjectOverall
    |  objects: [ObjectOverall]
    |}
    |""".trimMargin(), underlyingSchema="""
    |interface InterfaceUnderlying {
    |  name: String
    |}
    |
    |union UnionUnderlying = XUnderlying | YUnderlying
    |
    |type ObjectUnderlying implements InterfaceUnderlying {
    |  name: String
    |}
    |
    |type Query {
    |  hello: World
    |  renameInput(arg1: InputUnderlying!, arg2: String, arg3: EnumUnderlying): String
    |  renameInterfaceUnderlying: InterfaceUnderlying
    |  renameObjectUnderlying: ObjectUnderlying
    |  renameStringUnderlying: String
    |  renameUnionUnderlying: UnionUnderlying
    |  typenameTest: TypenameTest
    |}
    |
    |type TypenameTest {
    |  object: ObjectUnderlying
    |  objects: [ObjectUnderlying]
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |
    |type XUnderlying {
    |  x: Int
    |}
    |
    |type YUnderlying {
    |  y: Int
    |}
    |
    |enum EnumUnderlying {
    |  X
    |  Y
    |}
    |
    |input InputUnderlying {
    |  inputVal: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("typenameTest") { env ->
          MyService_TypenameTest(`object` = MyService_ObjectUnderlying(), objects =
              listOf(MyService_ObjectUnderlying(), MyService_ObjectUnderlying()))}
      }
      wiring.type("InterfaceUnderlying") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }

      wiring.type("UnionUnderlying") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
)) {
  private enum class MyService_EnumUnderlying {
    X,
    Y,
  }

  private data class MyService_InputUnderlying(
    public val inputVal: String? = null,
  )

  private interface MyService_InterfaceUnderlying {
    public val name: String?
  }

  private data class MyService_ObjectUnderlying(
    override val name: String? = null,
  ) : MyService_InterfaceUnderlying

  private data class MyService_TypenameTest(
    public val `object`: MyService_ObjectUnderlying? = null,
    public val objects: List<MyService_ObjectUnderlying?>? = null,
  )

  private sealed interface MyService_UnionUnderlying

  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class MyService_XUnderlying(
    public val x: Int? = null,
  ) : MyService_UnionUnderlying

  private data class MyService_YUnderlying(
    public val y: Int? = null,
  ) : MyService_UnionUnderlying
}
