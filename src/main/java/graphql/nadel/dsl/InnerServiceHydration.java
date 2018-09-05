package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

public class InnerServiceHydration extends AbstractNode<InnerServiceHydration> {

    private final String serviceName;
    private final String topLevelField;
    private final List<RemoteArgumentDefinition> arguments;

    public InnerServiceHydration(SourceLocation sourceLocation,
                                 List<Comment> comments,
                                 String serviceName,
                                 String topLevelField,
                                 List<RemoteArgumentDefinition> arguments) {
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

    public List<RemoteArgumentDefinition> getArguments() {
        return new ArrayList<>(arguments);
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
