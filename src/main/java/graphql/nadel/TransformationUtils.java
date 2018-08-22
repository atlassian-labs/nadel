package graphql.nadel;

import graphql.Internal;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.language.TypeDefinition;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Internal
public final class TransformationUtils {
    private TransformationUtils() {
    }

    public static List<FieldDefinitionWithParentType> collectFieldTransformations(ServiceDefinition serviceDefinition) {
        NodeTraverser traverser = new NodeTraverser();

        FieldTransformationVisitor visitor = new FieldTransformationVisitor();
        traverser.depthFirst(visitor, serviceDefinition.getChildren());
        return visitor.definitions();
    }

    public static class FieldDefinitionWithParentType {
        private final FieldDefinitionWithTransformation field;
        private final String parentType;

        private FieldDefinitionWithParentType(FieldDefinitionWithTransformation field, String parentType) {
            this.field = field;
            this.parentType = parentType;
        }

        public FieldDefinitionWithTransformation field() {
            return field;
        }

        public String parentType() {
            return parentType;
        }
    }

    private static class FieldTransformationVisitor extends NodeVisitorStub {
        private final Stack<String> typeStack = new Stack<>();
        private List<FieldDefinitionWithParentType> definitions = new ArrayList<>();

        @Override
        public TraversalControl visitFieldDefinition(FieldDefinition node, TraverserContext<Node> context) {
            if (context.getVar(NodeTraverser.LeaveOrEnter.class) != NodeTraverser.LeaveOrEnter.ENTER) {
                if (node instanceof FieldDefinitionWithTransformation) {
                    definitions.add(new FieldDefinitionWithParentType((FieldDefinitionWithTransformation) node, typeStack.peek()));
                }
            }
            return super.visitFieldDefinition(node, context);
        }

        @Override
        public TraversalControl visitInputValueDefinition(InputValueDefinition node, TraverserContext<Node> context) {
            // This is to go around NPE in traversal for InputValueDefinition in graphql-java (when default value is null)
            // fix was submitted
            return TraversalControl.ABORT;
        }

        @Override
        protected TraversalControl visitTypeDefinition(TypeDefinition<?> node, TraverserContext<Node> context) {
            switch (context.getVar(NodeTraverser.LeaveOrEnter.class)) {
                case ENTER:
                    typeStack.push(node.getName());
                    break;
                case LEAVE:
                    typeStack.pop();
                    break;
            }
            return super.visitTypeDefinition(node, context);
        }

        public List<FieldDefinitionWithParentType> definitions() {
            return definitions;
        }
    }

}
