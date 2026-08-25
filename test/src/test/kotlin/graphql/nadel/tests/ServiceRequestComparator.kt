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
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil.changeNode

/**
 * Compares service requests while allowing variables to be consistently renamed.
 *
 * Variable names are paired by their first use in the sorted queries. The same
 * pairing is then applied to the actual query and its variables map before both
 * are compared exactly.
 */
internal fun serviceRequestsMatchIgnoringVariableNames(
    expectedDocument: Document,
    expectedVariables: JsonMap,
    actualDocument: Document,
    actualVariables: JsonMap,
): Boolean {
    val expected = prepareServiceRequest(expectedDocument, expectedVariables)
    val actual = prepareServiceRequest(actualDocument, actualVariables)

    if (expected.variableNames.size != actual.variableNames.size) {
        return false
    }

    val actualToExpectedNames = actual.variableNames
        .zip(expected.variableNames)
        .toMap()
    val renamedActualDocument = actual.document.renameVariables(actualToExpectedNames)
    val renamedActualVariables = actual.variables
        .mapKeys { (name, _) -> actualToExpectedNames.getValue(name) }

    return AstPrinter.printAstCompact(expected.document) ==
        AstPrinter.printAstCompact(AstSorter().sort(renamedActualDocument)) &&
        compareJsonObject(
            expected = expected.variables,
            actual = renamedActualVariables,
        ).passed()
}

private data class PreparedServiceRequest(
    val document: Document,
    val variableNames: List<String>,
    val variables: JsonMap,
)

private fun prepareServiceRequest(
    document: Document,
    variables: JsonMap,
): PreparedServiceRequest {
    val sortedDocument = AstSorter().sort(document)
    val operation = sortedDocument.definitions
        .filterIsInstance<OperationDefinition>()
        .single()
    val definedVariableNames = operation.variableDefinitions
        .map { it.name }

    require(definedVariableNames.distinct().size == definedVariableNames.size) {
        "Service query contains duplicate variable definitions"
    }

    val referencedVariableNames = linkedSetOf<String>()
    NodeTraverser().preOrder(
        object : NodeVisitorStub() {
            override fun visitVariableReference(
                node: VariableReference,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                referencedVariableNames += node.name
                return TraversalControl.CONTINUE
            }
        },
        sortedDocument,
    )

    val definedVariableNameSet = definedVariableNames.toSet()
    require(referencedVariableNames == definedVariableNameSet) {
        "Service query variable definitions and references must match"
    }
    require(variables.keys == definedVariableNameSet) {
        "Service query variable definitions and variables map keys must match"
    }

    return PreparedServiceRequest(
        document = sortedDocument,
        variableNames = referencedVariableNames.toList(),
        variables = variables,
    )
}

private fun Document.renameVariables(names: Map<String, String>): Document {
    return AstTransformer().transform(
        this,
        object : NodeVisitorStub() {
            override fun visitVariableDefinition(
                node: VariableDefinition,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                return changeNode(
                    context,
                    node.transform { builder ->
                        builder.name(names.getValue(node.name))
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
                        builder.name(names.getValue(node.name))
                    },
                )
            }
        },
    ) as Document
}
