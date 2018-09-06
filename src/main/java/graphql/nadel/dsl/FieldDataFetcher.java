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

public class FieldDataFetcher extends AbstractNode<FieldDataFetcher> {

    private final String dataFetcherName;

    public FieldDataFetcher(String dataFetcherName, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.dataFetcherName = dataFetcherName;
    }

    public String getDataFetcherName() {
        return dataFetcherName;
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
    public FieldDataFetcher deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
