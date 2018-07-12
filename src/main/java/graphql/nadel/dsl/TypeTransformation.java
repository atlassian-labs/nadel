//package graphql.nadel.dsl;
//
//import graphql.language.*;
//import graphql.util.TraversalControl;
//import graphql.util.TraverserContext;
//
//import java.util.List;
//
//public class TypeTransformation extends AbstractNode<TypeTransformation> {
//
//    private String targetName;
//
//    private ServiceDefinition parentDefinition;
//
//    public ServiceDefinition getParentDefinition() {
//        return parentDefinition;
//    }
//
//    public void setParentDefinition(ServiceDefinition parentDefinition) {
//        this.parentDefinition = parentDefinition;
//    }
//
//    public String getTargetName() {
//        return targetName;
//    }
//
//    public void setTargetName(String targetName) {
//        this.targetName = targetName;
//    }
//
//    @Override
//    public List<Node> getChildren() {
//        return null;
//    }
//
//    @Override
//    public boolean isEqualTo(Node node) {
//        return false;
//    }
//
//    @Override
//    public TypeTransformation deepCopy() {
//        return null;
//    }
//
//    @Override
//    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
//        return null;
//    }
//}
