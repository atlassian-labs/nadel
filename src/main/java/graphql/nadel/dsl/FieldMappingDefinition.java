package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.IgnoredChars;
import graphql.language.Node;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

public class FieldMappingDefinition extends AbstractNode<FieldMappingDefinition> {

    private final List<String> inputPath;

    public FieldMappingDefinition(List<String> inputPath, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments, IgnoredChars.EMPTY);
        this.inputPath = inputPath;
    }

    public List<String> getInputPath() {
        return inputPath;
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
    public FieldMappingDefinition withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public FieldMappingDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }

    @Override
    public String toString() {
        return "FieldMappingDefinition{" +
                "inputPath=" + inputPath +
                '}';
    }
}
