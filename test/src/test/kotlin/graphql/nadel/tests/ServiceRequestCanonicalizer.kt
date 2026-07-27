package graphql.nadel.tests

import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.language.AstTransformer
import graphql.language.Document
import graphql.language.Node
import graphql.language.NodeTraverser
import graphql.language.NodeVisitorStub
import graphql.language.OperationDefinition
import graphql.language.VariableDefinition
import graphql.language.VariableReference
import graphql.nadel.engine.util.JsonMap
import graphql.parser.Parser
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil.changeNode

internal data class CanonicalServiceRequest(
    val query: String,
    val variables: JsonMap,
)

internal fun canonicalizeServiceRequest(
    query: String,
    variables: JsonMap,
): CanonicalServiceRequest {
    return canonicalizeServiceRequest(
        document = Parser().parseDocument(query),
        variables = variables,
    )
}

internal fun canonicalizeServiceRequest(
    document: Document,
    variables: JsonMap,
): CanonicalServiceRequest {
    val sortedDocument = AstSorter().sort(document)
    val variableNames = linkedSetOf<String>()

    NodeTraverser().preOrder(
        object : NodeVisitorStub() {
            override fun visitVariableReference(
                node: VariableReference,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                variableNames += node.name
                return TraversalControl.CONTINUE
            }
        },
        sortedDocument,
    )

    val operation = sortedDocument.definitions
        .filterIsInstance<OperationDefinition>()
        .single()
    val definedVariableNames = operation.variableDefinitions
        .mapTo(linkedSetOf()) { it.name }

    require(variableNames == definedVariableNames) {
        "Service query variable definitions and references must match"
    }
    require(variables.keys.all(definedVariableNames::contains)) {
        "Service variables must be declared by the service query"
    }

    val canonicalNames = variableNames
        .withIndex()
        .associate { (index, name) ->
            name to "v$index"
        }

    val renamedDocument = AstTransformer().transform(
        sortedDocument,
        object : NodeVisitorStub() {
            override fun visitVariableDefinition(
                node: VariableDefinition,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                return changeNode(
                    context,
                    node.transform { builder ->
                        builder.name(canonicalNames.getValue(node.name))
                    },
                )
            }

            override fun visitVariableReference(
                node: VariableReference,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                return changeNode(
                    context,
                    node.transform { builder ->
                        builder.name(canonicalNames.getValue(node.name))
                    },
                )
            }
        },
    ) as Document

    val canonicalVariables = linkedMapOf<String, Any?>()
    canonicalNames.forEach { (name, canonicalName) ->
        if (variables.containsKey(name)) {
            canonicalVariables[canonicalName] = variables[name]
        }
    }

    return CanonicalServiceRequest(
        query = AstPrinter.printAst(
            AstSorter().sort(renamedDocument),
        ),
        variables = canonicalVariables,
    )
}
