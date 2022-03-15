package graphql.nadel.schema


import graphql.AssertException
import graphql.language.AstPrinter
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD

class NadelDirectivesTest extends Specification {

    def commonDefs = """
            ${AstPrinter.printAst(NadelDirectives.HYDRATED_DIRECTIVE_DEFINITION)}
            ${AstPrinter.printAst(NadelDirectives.NADEL_HYDRATION_ARGUMENT_DEFINITION)}

            ${AstPrinter.printAst(NadelDirectives.NADEL_HYDRATION_COMPLEX_IDENTIFIED_BY)}
            ${AstPrinter.printAst(NadelDirectives.NADEL_HYDRATION_FROM_ARGUMENT_DEFINITION)}
            ${AstPrinter.printAst(NadelDirectives.NADEL_HYDRATION_TEMPLATE_ENUM_DEFINITION)}
            ${AstPrinter.printAst(NadelDirectives.HYDRATED_FROM_DIRECTIVE_DEFINITION)}
            ${AstPrinter.printAst(NadelDirectives.HYDRATED_TEMPLATE_DIRECTIVE_DEFINITION)}
        """

    def "can handle original @hydrated directives"() {

        def sdl = """
            ${commonDefs}

            type Query {
                field : String @hydrated(
                            service: "IssueService"
                            field: "jira.issueById"
                            batchSize : 50
                            timeout : 100
                            arguments: [
                                {name: "fieldVal" value: "\$source.namespace.issueId"}
                                {name: "argVal" value: "\$argument.cloudId"}
                            ])
            }
        """

        def schema = TestUtil.schema(sdl)
        def fieldDef = schema.getQueryType().getFieldDefinition("field")

        when:
        def serviceHydration = NadelDirectives.createUnderlyingServiceHydration(fieldDef, schema)

        then:

        def hydration = serviceHydration[0]
        hydration.serviceName == "IssueService"
        hydration.syntheticField == "jira"
        hydration.topLevelField == "issueById"
        hydration.batchSize == 50
        hydration.timeout == 100
        hydration.arguments.size() == 2

        hydration.arguments[0].name == "fieldVal"
        hydration.arguments[0].remoteArgumentSource.sourceType == OBJECT_FIELD
        hydration.arguments[0].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[1].name == "argVal"
        hydration.arguments[1].remoteArgumentSource.sourceType == FIELD_ARGUMENT
        hydration.arguments[1].remoteArgumentSource.argumentName == "cloudId"
    }

    def "can handle new @hydratedFrom directives"() {

        def sdl = """
            ${commonDefs}

            extend enum NadelHydrationTemplate {
                JIRA @hydratedTemplate(
                            service: "IssueService"
                            field: "jira.issueById"
                            batchSize : 50
                            timeout : 100
                    )
            }


            type Query {
                field : String @hydratedFrom(
                            template : JIRA
                            arguments: [
                                {name: "fieldVal" valueFromField: "namespace.issueId"}
                                {name: "argVal" valueFromArg: "cloudId"}

                                # for legacy confusion reasons

                                {name: "fieldValLegacy" valueFromField: "\$source.namespace.issueId"}
                                {name: "argValLegacy" valueFromArg: "\$argument.cloudId"}
                            ])
            }
        """

        def schema = TestUtil.schema(sdl)
        def fieldDef = schema.getQueryType().getFieldDefinition("field")

        when:
        def serviceHydration = NadelDirectives.createUnderlyingServiceHydration(fieldDef, schema)

        then:

        def hydration = serviceHydration[0]
        hydration.serviceName == "IssueService"
        hydration.syntheticField == "jira"
        hydration.topLevelField == "issueById"
        hydration.batchSize == 50
        hydration.timeout == 100
        hydration.arguments.size() == 4

        hydration.arguments[0].name == "fieldVal"
        hydration.arguments[0].remoteArgumentSource.sourceType == OBJECT_FIELD
        hydration.arguments[0].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[1].name == "argVal"
        hydration.arguments[1].remoteArgumentSource.sourceType == FIELD_ARGUMENT
        hydration.arguments[1].remoteArgumentSource.argumentName == "cloudId"

        hydration.arguments[2].name == "fieldValLegacy"
        hydration.arguments[2].remoteArgumentSource.sourceType == OBJECT_FIELD
        hydration.arguments[2].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[3].name == "argValLegacy"
        hydration.arguments[3].remoteArgumentSource.sourceType == FIELD_ARGUMENT
        hydration.arguments[3].remoteArgumentSource.argumentName == "cloudId"
    }

    def "throws exception for if valueFromField or valueFromArg both or none specified"() {

        expect:
        def sdl = """
            ${commonDefs}

            extend enum NadelHydrationTemplate {
                JIRA @hydratedTemplate(
                            service: "IssueService"
                            field: "jira.issueById"
                            batchSize : 50
                            timeout : 100
                    )
            }


            type Query {
                field : String @hydratedFrom(
                            template : JIRA
                            arguments: [
                                ${argDecl}
                            ])
            }
        """

        def schema = TestUtil.schema(sdl)
        def fieldDef = schema.getQueryType().getFieldDefinition("field")

        try {
            NadelDirectives.createUnderlyingServiceHydration(fieldDef, schema)
            assert false, "We expected an assertion"
        } catch (AssertException assertException) {
            assert assertException.message == "You must specify only one of valueForField or valueForArg in NadelHydrationFromArgument arguments"
        }

        where:

        argDecl                                                                 | _
        '{name: "fieldVal"}'                                                    | _
        '{name: "fieldVal" valueFromField: "issueId" valueFromArg: "cloudId" }' | _
    }

    def "print out SDL"() {
        when:
        println(commonDefs)

        then:
        true
    }
}
