package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.SelectionSetContainer;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * A {@link graphql.nadel.engine.transformation.FieldTransformation} that runs a list of other transformation
 */
public class ComposedFieldTransformation implements FieldTransformation {

    final List<FieldTransformation> transformations;

    public ComposedFieldTransformation(FieldTransformation... transformations) {
        this.transformations = Arrays.stream(transformations)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private List<FieldTransformation> getReversedTransformations() {
        return transformations.stream().collect(collectingAndThen(toList(), l -> {
            Collections.reverse(l);
            return l;
        }));
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        for (FieldTransformation transformation : transformations) {
            transformation.apply(environment);
            Field changedNode = (Field) environment.getTraverserContext().thisNode();
            environment = wrappedEnv(environment, changedNode);
        }
        return TraversalControl.CONTINUE;
    }

    @Override
    public MergedField unapplyMergedField(MergedField mergedField) {
        for (FieldTransformation transformation : getReversedTransformations()) {
            mergedField = transformation.unapplyMergedField(mergedField);
        }
        return mergedField;
    }

    @Override
    public <T extends ExecutionResultNode> T unapplyResultNode(T executionResultNode) {
        for (FieldTransformation transformation : getReversedTransformations()) {
            executionResultNode = transformation.unapplyResultNode(executionResultNode);
        }
        return executionResultNode;
    }

    private QueryVisitorFieldEnvironment wrappedEnv(QueryVisitorFieldEnvironment environment, Field changedNode) {
        return new QueryVisitorFieldEnvironment() {

            @Override
            public boolean isTypeNameIntrospectionField() {
                return environment.isTypeNameIntrospectionField();
            }

            @Override
            public Field getField() {
                return changedNode;
            }

            @Override
            public GraphQLFieldDefinition getFieldDefinition() {
                return environment.getFieldDefinition();
            }

            @Override
            public GraphQLOutputType getParentType() {
                return environment.getParentType();
            }

            @Override
            public GraphQLFieldsContainer getFieldsContainer() {
                return environment.getFieldsContainer();
            }

            @Override
            public QueryVisitorFieldEnvironment getParentEnvironment() {
                return environment.getParentEnvironment();
            }

            @Override
            public Map<String, Object> getArguments() {
                return environment.getArguments();
            }

            @Override
            public SelectionSetContainer getSelectionSetContainer() {
                return environment.getSelectionSetContainer();
            }

            @Override
            public TraverserContext<Node> getTraverserContext() {
                return environment.getTraverserContext();
            }
        };
    }
}
