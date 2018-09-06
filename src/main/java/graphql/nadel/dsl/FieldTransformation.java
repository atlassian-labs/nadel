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

    private final FieldMappingDefinition fieldMappingDefinition;
    private final InnerServiceHydration innerServiceHydration;
    private final FieldDataFetcher fieldDataFetcher;


    public FieldTransformation(FieldMappingDefinition fieldMappingDefinition, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.fieldMappingDefinition = fieldMappingDefinition;
        this.innerServiceHydration = null;
        this.fieldDataFetcher = null;
    }

    public FieldTransformation(InnerServiceHydration innerServiceHydration, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.fieldMappingDefinition = null;
        this.innerServiceHydration = innerServiceHydration;
        this.fieldDataFetcher = null;
    }

    public FieldTransformation(FieldDataFetcher fieldDataFetcher, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.fieldMappingDefinition = null;
        this.innerServiceHydration = null;
        this.fieldDataFetcher = fieldDataFetcher;
    }

    public FieldMappingDefinition getFieldMappingDefinition() {
        return fieldMappingDefinition;
    }

    public InnerServiceHydration getInnerServiceHydration() {
        return innerServiceHydration;
    }

    public FieldDataFetcher getFieldDataFetcher() {
        return fieldDataFetcher;
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
