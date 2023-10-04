package graphql.nadel.dsl

import graphql.language.AstTransformer
import graphql.language.Node
import graphql.language.NodeVisitorStub
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

            override fun visitStringValue(
                node: StringValue,
                context: TraverserContext<Node<*>>,
            ): TraversalControl {
                val newAstValue = possiblyReplaceValue(node)
                if (newAstValue != null) {
                    return changeNode(context, newAstValue)
                }
                return TraversalControl.CONTINUE
            }
        }
        val newValue = AstTransformer().transform(value, nodeVisitor)
        return newValue as Value<*>
    }
}