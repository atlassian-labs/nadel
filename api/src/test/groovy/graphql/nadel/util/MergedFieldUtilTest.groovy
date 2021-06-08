package graphql.nadel.util

import graphql.execution.ExecutionContext
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.execution.MergedField
import graphql.language.AstPrinter
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.NodeUtil
import graphql.language.OperationDefinition
import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import java.util.function.Predicate

class MergedFieldUtilTest extends Specification {

    def sdl = '''
        type Query {
            jira : JiraQuery
        }
        
        type JiraQuery {
            someIssues : [IssueDetails]
            allIssues : [IssueDetails]
            projects : [ProjectDetails]
        }
        
        type IssueDetails {
            id : ID
            key : String
        }
        
        type ProjectDetails {
            name : String
        }
    '''

    def schema = TestUtil.schema(sdl)


    class TestFieldAndContext {
        MergedField mergedField
        ExecutionContext executionContext

        TestFieldAndContext(String query, Map<String, Object> variables) {
            def document = TestUtil.parseQuery(query)
            NodeUtil.GetOperationResult getOperationResult = NodeUtil.getOperation(document, null)
            Map<String, FragmentDefinition> fragmentsByName = getOperationResult.fragmentsByName
            OperationDefinition operationDefinition = getOperationResult.operationDefinition

            def topField = operationDefinition.getSelectionSet().getSelectionsOfType(Field.class)[0] as Field
            this.mergedField = MergedField.newMergedField(topField).build()
            this.executionContext = ExecutionContextBuilder.newExecutionContextBuilder()
                    .graphQLSchema(schema)
                    .fragmentsByName(fragmentsByName)
                    .variables(variables).executionId(ExecutionId.from("id"))
                    .build()

        }
    }

    def complicatedQuery = '''
            query {
                jira {
                    ... JiraQueryFrag
                    ... on JiraQuery {
                        allIssues { key }
                    }
                    specific: someIssues { id, key }
                    projects { name }
                    aliasedProject : projects { name }
                }
            }
            
            fragment IssueDetailsFrag on IssueDetails {
                id
                key
            }

            fragment JiraQueryFrag on JiraQuery {
                someIssues {
                    ... IssueDetailsFrag
                }
            }
                
        '''


    def "'can edit to include only certain fields'"() {

        def testFieldAndContext = new TestFieldAndContext(complicatedQuery, [:])
        Predicate<MergedField> onlyIssueFields = { fld -> fld.name.toLowerCase().contains("issue") }

        def parentType = testFieldAndContext.executionContext.getGraphQLSchema().getObjectType("JiraQuery")
        when:
        MergedField actualMergedField = MergedFieldUtil.includeSubSelection(
                testFieldAndContext.mergedField, parentType, testFieldAndContext.executionContext, onlyIssueFields)
                .get()

        then:
        actualMergedField.getFields().size() == 1
        def actualField = actualMergedField.getSingleField()

        actualField.getName() == "jira"
        actualField.getSelectionSet().getSelections().size() == 3

        def subSelectedFields = actualField.getSelectionSet().getSelectionsOfType(Field.class)
        subSelectedFields.size() == 3

        subSelectedFields.collect { (it.alias == null ? "" : it.alias + ":") + it.name } ==
                ["someIssues", "allIssues", "specific:someIssues"]

        def printedAst = AstPrinter.printAstCompact(actualField)
        printedAst == 'jira {someIssues {...IssueDetailsFrag} allIssues {key} specific:someIssues {id key}}'
    }

    def "cant edit away all fields"() {
        def testFieldAndContext = new TestFieldAndContext(complicatedQuery, [:])
        Predicate<MergedField> noFieldsAlways = { fld -> false }

        def parentType = testFieldAndContext.executionContext.getGraphQLSchema().getObjectType("JiraQuery")
        when:
        def selection = MergedFieldUtil.includeSubSelection(
                testFieldAndContext.mergedField, parentType, testFieldAndContext.executionContext, noFieldsAlways)

        then:
        selection == Optional.empty()
    }
}
