package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.ObjectTypeDefinition;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.List;

public class FieldTransformation extends AbstractNode<FieldTransformation> {

    private String targetName;
    private String topLevelField;
    private String argumentName;

    private ObjectTypeDefinition parentDefinition;
    private boolean added;

    public ObjectTypeDefinition getParentDefinition() {
        return parentDefinition;
    }

    public void setParentDefinition(ObjectTypeDefinition parentDefinition) {
        this.parentDefinition = parentDefinition;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }


    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
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

    public String getTopLevelField() {
        return topLevelField;
    }

    public void setTopLevelField(String topLevelField) {
        this.topLevelField = topLevelField;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public void setArgumentName(String argumentName) {
        this.argumentName = argumentName;
    }
}
