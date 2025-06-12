package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

class StubExampleTest : NadelIntegrationTest(
    query = """
        query {
          myExistingType {
            existingField
            otherExistingField {
              field1
              field2
            }
            newFieldWithExistingType {
              field3
            }
            newFieldWithComplexType {
              field4
              field5
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  myExistingType: MyExistingType
                }
                type MyExistingType {
                  existingField: String
                  otherExistingField: OtherExistingType
                  newFieldWithExistingType: ExistingType @stubbed
                  newFieldWithComplexType: NewType
                }
                type OtherExistingType {
                  field1: String
                  field2: String @stubbed
                }
                type ExistingType {
                  field3: String
                }
                type NewType @stubbed {
                  field4: String
                  field5: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  myExistingType: MyExistingType
                }
                type MyExistingType {
                  existingField: String
                  otherExistingField: OtherExistingType
                }
                type OtherExistingType {
                  field1: String
                }
                type ExistingType {
                  field3: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class OtherExistingType(
                    val field1: String,
                )

                data class ExistingField(
                    val field3: String,
                )

                data class MyExistingType(
                    val existingField: String,
                    val otherExistingField: OtherExistingType,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("myExistingType") { env ->
                                MyExistingType(
                                    existingField = "prod-data",
                                    otherExistingField = OtherExistingType(
                                        field1 = "otherProdData"
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
