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

public class HydrationArgumentValue extends AbstractNode<HydrationArgumentValue> {
    public enum ValueType {OBJECT_FIELD, FIELD_ARGUMENT, CONTEXT}

    private final String name;
    // for OBJECT_FIELD
    private final List<String> path;

    private final ValueType valueType;

    public HydrationArgumentValue(String name, List<String> path, ValueType valueType, SourceLocation sourceLocation) {
        super(sourceLocation, emptyList(), IgnoredChars.EMPTY);
        this.name = name;
        this.path = path;
        this.valueType = valueType;
    }

    public List<String> getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return null;
    }

    @Override
    public HydrationArgumentValue withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public HydrationArgumentValue deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
