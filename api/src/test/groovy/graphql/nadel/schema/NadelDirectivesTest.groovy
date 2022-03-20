package graphql.nadel.schema


import graphql.language.AstPrinter
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

class NadelDirectivesTest extends Specification {

    def commonDefs = """
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getHydratedDirectiveDefinition())}
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getNadelHydrationArgumentDefinition())}

        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getNadelHydrationComplexIdentifiedBy())}
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getNadelHydrationFromArgumentDefinition())}
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getNadelHydrationTemplateEnumDefinition())}
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getHydratedFromDirectiveDefinition())}
        ${AstPrinter.printAst(NadelDirectives.INSTANCE.getHydratedTemplateDirectiveDefinition())}
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
        def serviceHydration = NadelDirectives.INSTANCE.createUnderlyingServiceHydration(fieldDef, schema)

        then:

        def hydration = serviceHydration[0]
        hydration.serviceName == "IssueService"
        hydration.pathToActorField == ["jira", "issueById"]
        hydration.batchSize == 50
        hydration.timeout == 100
        hydration.arguments.size() == 2

        hydration.arguments[0].name == "fieldVal"
        hydration.arguments[0].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField
        hydration.arguments[0].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[1].name == "argVal"
        hydration.arguments[1].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument
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
        def serviceHydration = NadelDirectives.INSTANCE.createUnderlyingServiceHydration(fieldDef, schema)

        then:

        def hydration = serviceHydration[0]
        hydration.serviceName == "IssueService"
        hydration.pathToActorField == ["jira", "issueById"]
        hydration.batchSize == 50
        hydration.timeout == 100
        hydration.arguments.size() == 4

        hydration.arguments[0].name == "fieldVal"
        hydration.arguments[0].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField
        hydration.arguments[0].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[1].name == "argVal"
        hydration.arguments[1].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument
        hydration.arguments[1].remoteArgumentSource.argumentName == "cloudId"

        hydration.arguments[2].name == "fieldValLegacy"
        hydration.arguments[2].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.ObjectField
        hydration.arguments[2].remoteArgumentSource.pathToField == ["namespace", "issueId"]

        hydration.arguments[3].name == "argValLegacy"
        hydration.arguments[3].remoteArgumentSource.sourceType == RemoteArgumentSource.SourceType.FieldArgument
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
            NadelDirectives.INSTANCE.createUnderlyingServiceHydration(fieldDef, schema)
            assert false, "We expected an assertion"
        } catch (IllegalArgumentException assertException) {
            assert assertException.message == msg
        }

        where:
        argDecl                                                                 | msg
        '{name: "fieldVal"}'                                                    | "NadelHydrationFromArgument requires one of valueFromField or valueFromArg to be set"
        '{name: "fieldVal" valueFromField: "issueId" valueFromArg: "cloudId" }' | "NadelHydrationFromArgument can not have both valueFromField and valueFromArg set"
    }

    def "print out SDL"() {
        when:
        println(commonDefs)

        then:
        true
    }
}
