package graphql.nadel;

import graphql.Internal;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
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

    public static List<TransformationWithParentType<ObjectTypeDefinitionWithTransformation>>
    collectObjectTypeDefinitionWithTransformations(ServiceDefinition serviceDefinition) {
        NodeTraverser traverser = new NodeTraverser();

        FieldTransformationVisitor visitor = new FieldTransformationVisitor();
        traverser.depthFirst(visitor, serviceDefinition.getChildren());
        return visitor.objectTypeDefinitions();
    }

    public static List<TransformationWithParentType<FieldDefinitionWithTransformation>>
    collectFieldTransformations(ServiceDefinition serviceDefinition) {
        NodeTraverser traverser = new NodeTraverser();

        FieldTransformationVisitor visitor = new FieldTransformationVisitor();
        traverser.depthFirst(visitor, serviceDefinition.getChildren());
        return visitor.fieldTransformDefinitions();
    }

    public static class TransformationWithParentType<T> {
        private final T field;
        private final String parentType;

        private TransformationWithParentType(T field, String parentType) {
            this.field = field;
            this.parentType = parentType;
        }

        public T field() {
            return field;
        }

        public String parentType() {
            return parentType;
        }
    }

    private static class FieldTransformationVisitor extends NodeVisitorStub {
        private final Stack<String> typeStack = new Stack<>();
        private List<TransformationWithParentType<FieldDefinitionWithTransformation>> fieldTransformDefinitions
                = new ArrayList<>();
        private List<TransformationWithParentType<ObjectTypeDefinitionWithTransformation>> objTypeTransformDefinitions
                = new ArrayList<>();


        @Override
        public TraversalControl visitFieldDefinition(FieldDefinition node, TraverserContext<Node> context) {
            if (context.getVar(NodeTraverser.LeaveOrEnter.class) != NodeTraverser.LeaveOrEnter.ENTER) {
                if (node instanceof FieldDefinitionWithTransformation) {
                    fieldTransformDefinitions.add(new TransformationWithParentType(node, typeStack.peek()));
                }
            }
            return super.visitFieldDefinition(node, context);
        }

        @Override
        public TraversalControl visitObjectTypeDefinition(ObjectTypeDefinition node, TraverserContext<Node> context) {
            if (context.getVar(NodeTraverser.LeaveOrEnter.class) != NodeTraverser.LeaveOrEnter.ENTER) {
                if (node instanceof ObjectTypeDefinitionWithTransformation) {
                    objTypeTransformDefinitions.add(new TransformationWithParentType(node, typeStack.peek()));
                }
            }
            return super.visitObjectTypeDefinition(node, context);
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

        public List<TransformationWithParentType<FieldDefinitionWithTransformation>> fieldTransformDefinitions() {
            return fieldTransformDefinitions;
        }

        public List<TransformationWithParentType<ObjectTypeDefinitionWithTransformation>> objectTypeDefinitions() {
            return objTypeTransformDefinitions;
        }
    }
}
