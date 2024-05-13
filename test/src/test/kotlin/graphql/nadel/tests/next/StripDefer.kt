package graphql.nadel.tests.next

import graphql.language.AstPrinter
import graphql.language.Directive
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.language.SelectionSetContainer
import graphql.parser.Parser
import kotlin.test.Test
import kotlin.test.assertTrue

fun stripDefer(query: String): String {
    return Parser.parse(query)
        .children
        .map { child ->
            when (child) {
                is OperationDefinition -> child.transform { builder ->
                    builder.selectionSet(stripDefer(child.selectionSet))
                }
                is FragmentDefinition -> child.transform { builder ->
                    builder.selectionSet(stripDefer(child.selectionSet))
                }
                is SelectionSetContainer<*> -> throw UnsupportedOperationException()
                else -> child
            }
        }
        .joinToString("\n") {
            AstPrinter.printAst(it)
        }
}

private fun stripDefer(selectionSet: SelectionSet): SelectionSet {
    return selectionSet.transform {
        it.selections(selectionSet.selections.map(::stripDefer))
    }
}

private fun stripDefer(selection: Selection<*>): Selection<*> {
    return when (selection) {
        is Field -> selection.transform {
            it
                .directives(stripDefer(selection.directives))
                .selectionSet(selection.selectionSet?.let(::stripDefer))
        }
        is FragmentSpread -> selection.transform {
            it.directives(stripDefer(selection.directives))
        }
        is InlineFragment -> selection.transform {
            it
                .directives(stripDefer(selection.directives))
                .selectionSet(selection.selectionSet?.let(::stripDefer))
        }
        else -> throw UnsupportedOperationException()
    }
}

private fun stripDefer(directives: List<Directive>): List<Directive> {
    return directives
        .filterNot { it.name == "defer" }
}

class StripDeferTest {
    @Test
    fun removesDeferFromFields() {
        // language=GraphQL
        val query = """
            query {
              issues {
                __typename @defer
                assignee @defer
                included @include {
                  name @defer
                }
              }
              me @defer {
                name
              }
              ... {
                name
              }
              ... @defer {
                name
              }
            }
        """.trimIndent()

        val expected = AstPrinter.printAst(
            Parser().parseDocument(
                // language=GraphQL
                """
                    {
                      issues {
                        __typename
                        assignee
                        included @include {
                          name
                        }
                      }
                      me {
                        name
                      }
                      ... {
                        name
                      }
                      ... {
                        name
                      }
                    }
                """.trimIndent()
            ),
        ).trim()

        // When
        val stripped = stripDefer(query)

        // Then
        assertTrue(stripped == expected)
    }

    @Test
    fun removesDeferFromFragmentDefinition() {
        // language=GraphQL
        val query = """
            query {
              issues {
                __typename @defer
              }
            }
            fragment Defer on Query {
              ... @defer {
                issues @defer {
                  name @defer @include
                }
              }
            }
        """.trimIndent()

        val expected = AstPrinter.printAst(
            // language=GraphQL
            Parser().parseDocument(
                """
                    {
                      issues {
                        __typename
                      }
                    }
                    fragment Defer on Query {
                      ... {
                        issues {
                          name @include
                        }
                      }
                    }
                """.trimIndent()
            ),
        ).replace("\n{2}".toRegex(), "\n").trim()

        // When
        val stripped = stripDefer(query)

        // Then
        assertTrue(stripped == expected)
    }
}
