package graphql.nadel.dsl;

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

import static java.util.Collections.emptyList;

public class RemoteArgumentSource extends AbstractNode<RemoteArgumentSource> {
    public enum SourceType {OBJECT_FIELD, FIELD_ARGUMENT, CONTEXT}

    private final String name;
    // for OBJECT_FIELD
    private final List<String> path;

    private final SourceType sourceType;

    public RemoteArgumentSource(String name, List<String> path, SourceType sourceType, SourceLocation sourceLocation) {
        super(sourceLocation, emptyList(), IgnoredChars.EMPTY);
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
