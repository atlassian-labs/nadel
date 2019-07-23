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

public class HydrationArgumentDefinition extends AbstractNode<HydrationArgumentDefinition> {

    private final String name;
    private final HydrationArgumentValue hydrationArgumentValue;

    public HydrationArgumentDefinition(String name, HydrationArgumentValue hydrationArgumentValue,
                                       SourceLocation sourceLocation) {
        super(sourceLocation, emptyList(), IgnoredChars.EMPTY);
        this.name = name;
        this.hydrationArgumentValue = hydrationArgumentValue;
    }

    public String getName() {
        return name;
    }

    public HydrationArgumentValue getHydrationArgumentValue() {
        return hydrationArgumentValue;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return null;
    }

    @Override
    public HydrationArgumentDefinition withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public HydrationArgumentDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
