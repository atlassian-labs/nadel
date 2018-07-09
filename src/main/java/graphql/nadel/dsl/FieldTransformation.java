package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FieldTransformation extends AbstractNode<FieldTransformation> {

    private String targetName;
    private Type targetType;
    private String serviceName;

    //fixme: we probably also need a distinction between inner*, source and constant types
    private Map<String, FieldReference> arguments = new LinkedHashMap<>();

    private ObjectTypeDefinition parentDefinition;

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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, FieldReference> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, FieldReference> arguments) {
        this.arguments = arguments;
    }
}
