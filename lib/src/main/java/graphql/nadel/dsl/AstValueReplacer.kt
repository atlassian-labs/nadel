package graphql.nadel.dsl

import graphql.language.Argument
import graphql.language.AstTransformer
import graphql.language.Node
import graphql.language.NodeVisitorStub
import graphql.language.ObjectField
import graphql.language.StringValue
import graphql.language.Value
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TreeTransformerUtil.changeNode

class AstValueReplacer {

    interface ValueReplacer {
        fun replaceString(strValue: String): Value<*>?
    }

    fun replaceValues(value: Value<*>, valueReplacer: ValueReplacer): Value<*> {
        val nodeVisitor = object : NodeVisitorStub() {

            private fun possiblyReplaceValue(value: Value<*>): Value<*>? {
                if (value is StringValue) {
                    if (value.value != null) {
                        return valueReplacer.replaceString(value.value)
                    }
                }
                return null;
            }

            override fun visitArgument(
                node: Argument,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                val newAstValue = possiblyReplaceValue(node.value)
                if (newAstValue != null) {
                    val transformedNode = node.transform { it.value(newAstValue) }
                    return changeNode(context, transformedNode)
                }
                return TraversalControl.CONTINUE
            }

            override fun visitObjectField(
                node: ObjectField,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                val newAstValue = possiblyReplaceValue(node.value)
                if (newAstValue != null) {
                    val transformedNode = node.transform { it.value(newAstValue) }
                    return changeNode(context, transformedNode)
                }
                return TraversalControl.CONTINUE
            }
        }
        val newValue = AstTransformer().transform(value, nodeVisitor)
        return newValue as Value<*>
    }
}