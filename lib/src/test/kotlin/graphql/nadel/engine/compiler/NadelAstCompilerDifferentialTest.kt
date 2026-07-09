package graphql.nadel.engine.compiler

import graphql.execution.RawVariables
import graphql.language.AstPrinter
import graphql.language.OperationDefinition.Operation.QUERY
import graphql.normalized.ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler
import graphql.normalized.VariablePredicate
import graphql.parser.Parser
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * [NadelExecutableNormalizedOperationToAstCompiler] is a fork of graphql-java's
 * ExecutableNormalizedOperationToAstCompiler with one addition: it honours the forcePrintAsUnconditional flag on
 * [NadelExecutableNormalizedField]. This test pins that, with the flag unset on every field, it produces output
 * byte-identical to upstream across the bare, inline-fragment, argument and variable paths - so the fork stays a
 * faithful mirror when graphql-java is bumped.
 */
class NadelAstCompilerDifferentialTest {
    private val schema = SchemaGenerator().makeExecutableSchema(
        SchemaParser().parse(
            """
            type Query {
              animals: [Animal]
              issues(first: Int, filter: IssueFilter): [Issue]
            }
            interface Animal {
              id: ID
            }
            type Cat implements Animal {
              id: ID
              meow: String
            }
            type Dog implements Animal {
              id: ID
              woof: String
            }
            type Issue {
              id: ID
              title: String
            }
            input IssueFilter {
              text: String
            }
            """.trimIndent(),
        ),
        RuntimeWiring.newRuntimeWiring()
            .type("Animal") { it.typeResolver { null } }
            .build(),
    )

    private fun assertForkMatchesUpstream(query: String, makeVariables: Boolean) {
        val predicate = VariablePredicate { _, _, _ -> makeVariables }

        val operation = createExecutableNormalizedOperationWithRawVariables(
            schema,
            Parser().parseDocument(query),
            null,
            RawVariables.emptyVariables(),
        )
        val graphqlJavaTopLevelFields = operation.topLevelFields
        // The fork operates on NadelExecutableNormalizedField; convert the tree. No field is flagged
        // forcePrintAsUnconditional, so the output must match upstream exactly.
        val nadelTopLevelFields = graphqlJavaTopLevelFields
            .map(NadelExecutableNormalizedField::fromExecutableNormalizedField)

        val expected = ExecutableNormalizedOperationToAstCompiler.compileToDocument(
            schema, QUERY, null, graphqlJavaTopLevelFields, predicate,
        )
        val actual = NadelExecutableNormalizedOperationToAstCompiler.compileToDocument(
            schema, QUERY, null, nadelTopLevelFields, predicate,
        )

        assertEquals(AstPrinter.printAst(expected.document), AstPrinter.printAst(actual.document))
        assertEquals(expected.variables, actual.variables)
    }

    @Test
    fun `bare interface field, inline fragment and inlined arguments match upstream`() {
        assertForkMatchesUpstream(
            "query { animals { id ... on Cat { meow } } issues(first: 10, filter: { text: \"x\" }) { id title } }",
            makeVariables = false,
        )
    }

    @Test
    fun `arguments hoisted to variables match upstream`() {
        assertForkMatchesUpstream(
            "query { issues(first: 10, filter: { text: \"x\" }) { id title } }",
            makeVariables = true,
        )
    }
}
