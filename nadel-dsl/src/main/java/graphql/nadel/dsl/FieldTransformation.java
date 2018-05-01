package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.Type;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.List;

public class FieldTransformation extends AbstractNode<FieldTransformation> {

    private String targetName;
    private Type targetType;


    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }


    public Type getTargetType() {
        return targetType;
    }

    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public List<Node> getChildren() {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public FieldTransformation deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
