package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class RemoteArgumentSource extends AbstractNode<RemoteArgumentSource> {
    private final String name;
    private final SourceType sourceType;

    public RemoteArgumentSource(String name, SourceType sourceType, SourceLocation sourceLocation) {
        super(sourceLocation, emptyList());
        this.name = name;
        this.sourceType = sourceType;
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

    public enum SourceType {OBJECT_FIELD, FIELD_ARGUMENT, CONTEXT}
}
