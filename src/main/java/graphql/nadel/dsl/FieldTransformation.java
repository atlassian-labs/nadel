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

public class FieldTransformation extends AbstractNode<FieldTransformation> {

    private final InputMappingDefinition inputMappingDefinition;
    private final InnerServiceTransformation innerServiceTransformation;

    public FieldTransformation(InputMappingDefinition inputMappingDefinition, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.inputMappingDefinition = inputMappingDefinition;
        this.innerServiceTransformation = null;
    }

    public FieldTransformation(InnerServiceTransformation innerServiceTransformation, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.inputMappingDefinition = null;
        this.innerServiceTransformation = innerServiceTransformation;
    }

    public InputMappingDefinition getInputMappingDefinition() {
        return inputMappingDefinition;
    }

    public InnerServiceTransformation getInnerServiceTransformation() {
        return innerServiceTransformation;
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
    public FieldTransformation deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
