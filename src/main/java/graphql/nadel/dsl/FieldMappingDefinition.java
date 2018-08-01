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

public class FieldMappingDefinition extends AbstractNode<FieldMappingDefinition> {

    private final String inputName;

    public FieldMappingDefinition(String inputName, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.inputName = inputName;
    }

    public String getInputName() {
        return inputName;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
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
}
