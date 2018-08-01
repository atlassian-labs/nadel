package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InnerServiceHydration extends AbstractNode<InnerServiceHydration> {

    private final String serviceName;
    private final String topLevelField;
    private final Map<String, FieldMappingDefinition> arguments;

    public InnerServiceHydration(SourceLocation sourceLocation,
                                 List<Comment> comments,
                                 String serviceName,
                                 String topLevelField,
                                 Map<String, FieldMappingDefinition> arguments) {
        super(sourceLocation, comments);
        this.serviceName = serviceName;
        this.topLevelField = topLevelField;
        this.arguments = arguments;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTopLevelField() {
        return topLevelField;
    }

    public Map<String, FieldMappingDefinition> getArguments() {
        return new LinkedHashMap<>(arguments);
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
    public InnerServiceHydration deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
