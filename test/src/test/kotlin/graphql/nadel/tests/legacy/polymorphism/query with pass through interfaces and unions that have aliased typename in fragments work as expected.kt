package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query with pass through interfaces and unions that have aliased typename in fragments work as expected` :
    NadelLegacyIntegrationTest(
        query = """
            query petQ(${'$'}isLoyal: Boolean) {
              pets(isLoyal: ${'$'}isLoyal) {
                name
                ...DogFrag
                ... on Cat {
                  wearsBell
                  aliasedCatTypeName: __typename
                }
              }
            }
            fragment DogFrag on Dog {
              wearsCollar
              aliasedDogTypeName: __typename
            }
        """.trimIndent(),
        variables = mapOf("isLoyal" to true),
        services = listOf(
            Service(
                name = "PetService",
                overallSchema = """
                    type Query {
                      pets(isLoyal: Boolean): [Pet]
                      raining(isLoyal: Boolean): CatsAndDogs
                    }
                    interface Pet {
                      name: String
                      collar: Collar
                      collarToRenamed: Collar @renamed(from: "collar")
                    }
                    type Cat implements Pet {
                      name: String
                      wearsBell: Boolean
                      collar: Collar
                      collarToRenamed: Collar @renamed(from: "collar")
                    }
                    type Dog implements Pet {
                      name: String
                      wearsCollar: Boolean
                      collar: Collar
                      collarToRenamed: Collar @renamed(from: "collar")
                    }
                    union CatsAndDogs = Cat | Dog
                    interface Collar {
                      color: String
                      size: String
                    }
                    type DogCollar implements Collar {
                      color: String
                      size: String
                    }
                    type CatCollar implements Collar {
                      color: String
                      size: String
                    }
                """.trimIndent(),
                underlyingSchema = """
                    interface Collar {
                      color: String
                      size: String
                    }
                    interface Pet {
                      collar: Collar
                      name: String
                      ownerIds: [String]
                    }
                    union CatsAndDogs = Cat | Dog
                    type Cat implements Pet {
                      collar: Collar
                      name: String
                      ownerIds: [String]
                      wearsBell: Boolean
                    }
                    type CatCollar implements Collar {
                      color: String
                      size: String
                    }
                    type Dog implements Pet {
                      collar: Collar
                      name: String
                      ownerIds: [String]
                      wearsCollar: Boolean
                    }
                    type DogCollar implements Collar {
                      color: String
                      size: String
                    }
                    type Mutation {
                      hello: String
                    }
                    type Query {
                      hello: World
                      pets(isLoyal: Boolean): [Pet]
                      raining(isLoyal: Boolean): CatsAndDogs
                    }
                    type World {
                      id: ID
                      name: String
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type.dataFetcher("pets") { env ->
                            if (env.getArgument<Any?>("isLoyal") == true) {
                                listOf(
                                    PetService_Dog(name = "Sparky", wearsCollar = true),
                                    PetService_Cat(
                                        name = "Whiskers",
                                        wearsBell = false,
                                    ),
                                )
                            } else {
                                null
                            }
                        }
                    }
                    wiring.type("CatsAndDogs") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }

                    wiring.type("Collar") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }

                    wiring.type("Pet") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }
                },
            ),
            Service(
                name = "OwnerService",
                overallSchema = """
                    type Query {
                      owner(id: String): Owner
                    }
                    interface Owner {
                      name: String
                    }
                    type CaringOwner implements Owner {
                      name: String
                      givesPats: Boolean
                    }
                    type CruelOwner implements Owner {
                      name: String
                      givesSmacks: Boolean
                    }
                """.trimIndent(),
                underlyingSchema = """
                    interface Owner {
                      name: String
                    }
                    type CaringOwner implements Owner {
                      givesPats: Boolean
                      name: String
                    }
                    type CruelOwner implements Owner {
                      givesSmacks: Boolean
                      name: String
                    }
                    type Query {
                      owner(id: String): Owner
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Owner") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }
                },
            ),
        ),
    ) {
    private data class PetService_Cat(
        override val collar: PetService_Collar? = null,
        override val name: String? = null,
        override val ownerIds: List<String?>? = null,
        val wearsBell: Boolean? = null,
    ) : PetService_Pet,
        PetService_CatsAndDogs

    private data class PetService_CatCollar(
        override val color: String? = null,
        override val size: String? = null,
    ) : PetService_Collar

    private sealed interface PetService_CatsAndDogs

    private interface PetService_Collar {
        val color: String?

        val size: String?
    }

    private data class PetService_Dog(
        override val collar: PetService_Collar? = null,
        override val name: String? = null,
        override val ownerIds: List<String?>? = null,
        val wearsCollar: Boolean? = null,
    ) : PetService_Pet,
        PetService_CatsAndDogs

    private data class PetService_DogCollar(
        override val color: String? = null,
        override val size: String? = null,
    ) : PetService_Collar

    private interface PetService_Pet {
        val collar: PetService_Collar?

        val name: String?

        val ownerIds: List<String?>?
    }

    private data class PetService_World(
        val id: String? = null,
        val name: String? = null,
    )

    private data class OwnerService_CaringOwner(
        val givesPats: Boolean? = null,
        override val name: String? = null,
    ) : OwnerService_Owner

    private data class OwnerService_CruelOwner(
        val givesSmacks: Boolean? = null,
        override val name: String? = null,
    ) : OwnerService_Owner

    private interface OwnerService_Owner {
        val name: String?
    }
}
