package graphql.nadel.dsl;

import graphql.Internal;
import graphql.language.AbstractNode;
import graphql.language.IgnoredChars;
import graphql.language.Node;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Internal
public class RemoteArgumentSource extends AbstractNode<RemoteArgumentSource> {
    public enum SourceType {OBJECT_FIELD, FIELD_ARGUMENT}

    private final String name;
    // for OBJECT_FIELD
    private final List<String> path;

    private final SourceType sourceType;

    public RemoteArgumentSource(String name, List<String> path, SourceType sourceType, SourceLocation sourceLocation, Map<String, String> additionalData) {
        super(sourceLocation, emptyList(), IgnoredChars.EMPTY, additionalData);
        this.name = name;
        this.path = path;
        this.sourceType = sourceType;
    }

    public List<String> getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return null;
    }

    @Override
    public RemoteArgumentSource withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public RemoteArgumentSource deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
