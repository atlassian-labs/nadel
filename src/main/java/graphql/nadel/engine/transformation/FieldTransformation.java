package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static graphql.Assert.assertTrue;


/*
 * Created per field in the overall query.
 */
public abstract class FieldTransformation {


    public static class UnapplyResult {

        private final ExecutionResultNode node;
        private final TraversalControl traversalControl;

        public UnapplyResult(ExecutionResultNode node, TraversalControl traversalControl) {
            this.node = node;
            this.traversalControl = traversalControl;
        }

        public ExecutionResultNode getNode() {
            return node;
        }

        public TraversalControl getTraversalControl() {
            return traversalControl;
        }
    }

    public static class ApplyEnvironment {
        private final Field field;
        private final GraphQLFieldDefinition fieldDefinition;
        private final GraphQLFieldsContainer fieldsContainer;
        private final TraverserContext<Node> traverserContext;

        public ApplyEnvironment(Field field, GraphQLFieldDefinition fieldDefinition, GraphQLFieldsContainer fieldsContainer, TraverserContext<Node> traverserContext) {
            this.field = field;
            this.fieldDefinition = fieldDefinition;
            this.fieldsContainer = fieldsContainer;
            this.traverserContext = traverserContext;
        }

        public Field getField() {
            return field;
        }

        public GraphQLFieldDefinition getFieldDefinition() {
            return fieldDefinition;
        }

        public GraphQLFieldsContainer getFieldsContainer() {
            return fieldsContainer;
        }

        public TraverserContext<Node> getTraverserContext() {
            return traverserContext;
        }
    }

    private ApplyEnvironment environment;
    private String fieldId = UUID.randomUUID().toString();

    public TraversalControl apply(ApplyEnvironment environment) {
        this.environment = environment;
        return TraversalControl.CONTINUE;
    }

    /*
     * This is a bit strange method because n FieldTransformations map to one unapply method and we don't know the mapping until
     * this method is called. So we actually give all relevant transformations as a List
     */
    public abstract UnapplyResult unapplyResultNode(ExecutionResultNode executionResultNode,
                                                    List<FieldTransformation> allTransformations,
                                                    UnapplyEnvironment environment);

    public abstract AbstractNode getDefinition();

    public String getFieldId() {
        return fieldId;
    }

    public ApplyEnvironment getApplyEnvironment() {
        return environment;
    }

    public Field getOriginalField() {
        return getApplyEnvironment().getField();
    }

    public GraphQLOutputType getOriginalFieldType() {
        return getApplyEnvironment().getFieldDefinition().getType();
    }

    public GraphQLFieldsContainer getOriginalFieldsContainer() {
        return getApplyEnvironment().getFieldsContainer();
    }

    public GraphQLFieldDefinition getOriginalFieldDefinition() {
        return getApplyEnvironment().getFieldDefinition();
    }


    protected ExecutionStepInfo replaceFieldsAndTypesWithOriginalValues(List<FieldTransformation> allTransformations, ExecutionStepInfo esi, ExecutionStepInfo parentEsi) {
        MergedField underlyingMergedField = esi.getField();
        List<Field> underlyingFields = underlyingMergedField.getFields();
        assertTrue(allTransformations.size() == underlyingFields.size());

        List<Field> newFields = new ArrayList<>();
        for (FieldTransformation fieldTransformation : allTransformations) {
            newFields.add(fieldTransformation.getOriginalField());
        }
        MergedField newMergedField = MergedField.newMergedField(newFields).build();
        FieldTransformation fieldTransformation = allTransformations.get(0);
        GraphQLOutputType originalFieldType = fieldTransformation.getOriginalFieldType();

        ExecutionStepInfo esiWithMappedField = esi.transform(builder -> {
                    builder
                            .field(newMergedField)
                            .fieldDefinition(allTransformations.get(0).getOriginalFieldDefinition())
                            .type(originalFieldType);
                    if (parentEsi.getType() instanceof GraphQLObjectType) {
                        builder.fieldContainer((GraphQLObjectType) parentEsi.getType());
                    }
                }

        );
        return esiWithMappedField;
    }


}
