package graphql.nadel

import graphql.GraphQL
import graphql.nadel.testutils.TestUtil
import graphql.schema.DataFetcher
import graphql.schema.TypeResolver
import spock.lang.Ignore
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.schema
import static graphql.nadel.testutils.TestUtil.serviceFactory
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring

class NadelInterfaceAndUnionHandlingTest extends Specification {

    def ndsl = '''
         service PetService {
         
            type Query{
                pet(isLoyal : Boolean) : Pet
                raining(isLoyal : Boolean) : CatsAndDogs 
            } 
            
            interface Pet {
                name : String
                owner: Owner <= $innerQueries.OwnerService.ownerById(id: $source.ownerId)
            }
            
            type Cat implements Pet {
                name : String
                wearsBell : Boolean
                owner: Owner 
            }

            type Dog implements Pet {
                name : String
                wearsCollar : Boolean
                owner: Owner 
            }

            union CatsAndDogs = Cat | Dog
         }
         
         service OwnerService {
            type Query {
                owner(id : String) : Owner
            }
            
            interface Owner {
                name : String
            }
            
            type CaringOwner implements Owner {
                name : String
                givesPats : Boolean
            }
    
            type CruelOwner implements Owner {
                name : String
                givesSmacks : Boolean
            }
        }
        '''

    def underlyingSchemaSpecPets = '''
            type Query{
                hello: World 
                pet(isLoyal : Boolean) : Pet
                raining(isLoyal : Boolean) : CatsAndDogs 
            } 
            
            type World {
                id: ID
                name: String
            }
            
            type Mutation{
                hello: String  
            }
            
            interface Pet {
                ownerId : String
                name : String
            }
            
            type Cat implements Pet {
                ownerId : String
                name : String
                wearsBell : Boolean
            }

            type Dog implements Pet {
                ownerId : String
                name : String
                wearsCollar : Boolean
            }

            union CatsAndDogs = Cat | Dog
        '''

    def underlyingSchemaSpecOwners = '''
        type Query {
            ownerById(id : String) : Owner
        }
        
        interface Owner {
            name : String
        }
        
        type CaringOwner implements Owner {
            name : String
            givesPats : Boolean
        }

        type CruelOwner implements Owner {
            name : String
            givesSmacks : Boolean
        }
    '''

    GraphQL buildUnderlyingImplementationPets(String spec) {
        def sparkyTheDog = [name: "Sparky", wearsCollar: true, ownerId: "dearly"] // dogs are loyal
        def whiskersTheCat = [name: "Whiskers", wearsBell: false, ownerId: "cruella"] // cats eat birds

        DataFetcher petDF = { env ->
            if (env.getArgument("isLoyal") == true) {
                return sparkyTheDog
            } else {
                return whiskersTheCat
            }
        }
        DataFetcher rainingDF = { env ->
            if (env.getArgument("isLoyal") == true) {
                return sparkyTheDog
            } else {
                return whiskersTheCat
            }
        }
        TypeResolver petTR = { env ->
            if (env.object["name"] == "Sparky") {
                return env.schema.getObjectType("Dog")
            } else {
                return env.schema.getObjectType("Cat")
            }
        }
        TypeResolver catsAndDogsTR = { env ->
            if (env.object["name"] == "Sparky") {
                return env.schema.getObjectType("Dog")
            } else {
                return env.schema.getObjectType("Cat")
            }
        }
        def runtimeWiring = newRuntimeWiring()
                .type(newTypeWiring("Query")
                .dataFetcher("pet", petDF)
                .dataFetcher("raining", rainingDF))
                .type(newTypeWiring("Pet").typeResolver(petTR))
                .type(newTypeWiring("CatsAndDogs").typeResolver(catsAndDogsTR))
                .build()
        def graphQLSchema = schema(spec, runtimeWiring)
        return GraphQL.newGraphQL(graphQLSchema).build()
    }

    GraphQL buildUnderlyingImplementationOwners(String spec) {
        def caringOwner = [id: "dearly", name: "Mr Dearly", givesPats: true]
        def cruelOwner = [id: "cruella", name: "Cruella De Vil", givesSmacks: true]

        DataFetcher ownerDF = { env ->
            if (env.getArgument("id") == "cruella") {
                return cruelOwner
            } else {
                return caringOwner
            }
        }
        TypeResolver ownerTR = { env ->
            if (env.object["id"] == "cruella") {
                return env.schema.getObjectType("CruelOwner")
            } else {
                return env.schema.getObjectType("CaringOwner")
            }
        }
        def runtimeWiring = newRuntimeWiring()
                .type(newTypeWiring("Query")
                .dataFetcher("ownerById", ownerDF))
                .type(newTypeWiring("Owner").typeResolver(ownerTR))
                .build()
        def graphQLSchema = schema(spec, runtimeWiring)
        return GraphQL.newGraphQL(graphQLSchema).build()
    }

    def underlyingImplementationPets = buildUnderlyingImplementationPets(underlyingSchemaSpecPets)
    def underlyingImplementationOwners = buildUnderlyingImplementationOwners(underlyingSchemaSpecOwners)

    ServiceExecution serviceExecutionPets = TestUtil.serviceExecutionImpl(underlyingImplementationPets)
    ServiceExecution serviceExecutionOwners = TestUtil.serviceExecutionImpl(underlyingImplementationOwners)

    def underlyingSchemaPets = typeDefinitions(underlyingSchemaSpecPets)
    def underlyingSchemaOwners = typeDefinitions(underlyingSchemaSpecOwners)

    def serviceFactory = serviceFactory([
            PetService  : new Tuple2(serviceExecutionPets, underlyingSchemaPets),
            OwnerService: new Tuple2(serviceExecutionOwners, underlyingSchemaOwners)]
    )

    def "query with pass through interfaces and unions that have __typename in them work as expected"() {

        given:
        def query = '''
        query petQ($isLoyal : Boolean) { 
            pet(isLoyal : $isLoyal) {
                name
                __typename
                ... on Dog {
                    wearsCollar
                    __typename
                }
                ... on Cat {
                    wearsBell 
                }
            }
            raining(isLoyal : $isLoyal) {
                __typename
                ... on Dog {
                    wearsCollar
                }
                ... on Cat {
                    wearsBell 
                }
            }
        }    
        '''

        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceDataFactory(serviceFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: true)).join()

        then:
        result.data == [
                pet    : [name: "Sparky", wearsCollar: true, __typename: "Dog"],
                raining: [wearsCollar: true, __typename: "Dog"],
        ]

        when:
        result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: false)).join()

        then:
        result.data == [
                pet    : [name: "Whiskers", wearsBell: false, __typename: "Cat"],
                raining: [wearsBell: false, __typename: "Cat"],
        ]
    }

    def "query with pass through interfaces and unions that DONT have __typename in them work as expected"() {

        given:
        def query = '''
        query petQ($isLoyal : Boolean) { 
            pet(isLoyal : $isLoyal) {
                name
                ... on Dog {
                    wearsCollar
                }
                ... on Cat {
                    wearsBell 
                }
            }
            raining(isLoyal : $isLoyal) {
                ... on Dog {
                    wearsCollar
                }
                ... on Cat {
                    wearsBell 
                }
            }
        }    
        '''

        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceDataFactory(serviceFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: true)).join()

        then:
        result.data == [
                pet    : [name: "Sparky", wearsCollar: true],
                raining: [wearsCollar: true],
        ]

        when:
        result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: false)).join()

        then:
        result.data == [
                pet    : [name: "Whiskers", wearsBell: false],
                raining: [wearsBell: false],
        ]
    }

    def "query with pass through interfaces and unions that have __typename in fragments work as expected"() {

        given:
        def query = '''
        query petQ($isLoyal : Boolean) { 
            pet(isLoyal : $isLoyal) {
                name
                ... DogFrag
                ... on Dog {
                    wearsCollar
                    __typename
                }
                ... on Cat {
                    wearsBell 
                    __typename
                }
                
            }
            raining(isLoyal : $isLoyal) {
                ... on Dog {
                    wearsCollar
                    __typename
                }
                ... on Cat {
                    wearsBell 
                    __typename
                }
            }
        }    
        fragment DogFrag on Dog {
            wearsCollar
            __typename
        }
        '''

        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceDataFactory(serviceFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: true)).join()

        then:
        result.data == [
                pet    : [name: "Sparky", wearsCollar: true, __typename: "Dog"],
                raining: [wearsCollar: true, __typename: "Dog"],
        ]

        when:
        result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: false)).join()

        then:
        result.data == [
                pet    : [name: "Whiskers", wearsBell: false, __typename: "Cat"],
                raining: [wearsBell: false, __typename: "Cat"],
        ]
    }


    def "query with pass through interfaces and unions that have aliased __typename in fragments work as expected"() {

        given:
        def query = '''
        query petQ($isLoyal : Boolean) { 
            pet(isLoyal : $isLoyal) {
                name
                ... DogFrag
                ... on Cat {
                    wearsBell 
                    aliasedCatTypeName : __typename
                }

            }
        }    
        
        fragment DogFrag on Dog {
            wearsCollar
            aliasedDogTypeName : __typename
        }

        '''

        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceDataFactory(serviceFactory)
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: true)).join()

        then:
        result.data == [
                pet: [name: "Sparky", wearsCollar: true, aliasedDogTypeName: "Dog"],
        ]

        when:
        result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: false)).join()

        then:
        result.data == [
                pet: [name: "Whiskers", wearsBell: false, aliasedCatTypeName: "Cat"],
        ]
    }

    @Ignore("Currently the Hydration code in general cant cope with interfaces - we need to fix this")
    def "query with hydrated interfaces work as expected"() {

        given:
        def query = '''
        query petQ($isLoyal : Boolean) { 
            pet(isLoyal : $isLoyal) {
                name
                owner {
                    name
                    ... on CaringOwner {
                        givesPats    
                    }
                    ... on CruelOwner {
                        givesSmacks    
                    }
                }
            }
        }    
        '''

        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceDataFactory(serviceFactory)
                .build()
        when:
        def result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: true)).join()

        then:
        result.data == [
                pet: [name: "Sparky", wearsCollar: true, owner: [name: "Mr Dearly", givesPats: true]],
        ]

        when:
        result = nadel.execute(newNadelExecutionInput().query(query).variables(isLoyal: false)).join()

        then:
        result.data == [
                pet: [name: "Whiskers", wearsBell: false, owner: [name: "Cruella De Vil", givesSmacks: true]],
        ]
    }
}
