package graphql.nadel.engine.transformation;

import graphql.Assert;
import graphql.execution.ExecutionPath;
import graphql.execution.MergedField;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static graphql.Assert.assertTrue;
import static java.util.UUID.randomUUID;

public abstract class FieldTransformation {


    private ApplyEnvironment environment;
    private String transformationId = getClass().getSimpleName() + "-" + randomUUID().toString();

    public abstract ApplyResult apply(ApplyEnvironment environment);

    /*
     * This is a bit strange method because n FieldTransformations map to one unapply method and we don't know the mapping until
     * this method is called. So we actually give all relevant transformations as a List
     */
    public abstract UnapplyResult unapplyResultNode(ExecutionResultNode executionResultNode,
                                                    List<FieldTransformation> allTransformations,
                                                    UnapplyEnvironment environment);

    public abstract AbstractNode getDefinition();

    public String getTransformationId() {
        return transformationId;
    }

    public void setEnvironment(ApplyEnvironment environment) {
        this.environment = environment;
    }

    public ApplyEnvironment getApplyEnvironment() {
        return environment;
    }


    public Field getOriginalField() {
        return getApplyEnvironment().getField();
    }

    public GraphQLOutputType getOriginalFieldType() {
        return getApplyEnvironment().getFieldDefinitionOverall().getType();
    }

    public GraphQLFieldsContainer getOriginalFieldsContainer() {
        return getApplyEnvironment().getFieldsContainerOverall();
    }

    public GraphQLFieldDefinition getOriginalFieldDefinition() {
        return getApplyEnvironment().getFieldDefinitionOverall();
    }

    protected NormalizedQueryField getMatchingNormalizedQueryFieldBasedOnParent(ExecutionResultNode parent) {
        List<NormalizedQueryField> normalizedFields = getApplyEnvironment().getNormalizedQueryFieldsOverall();
        if (parent instanceof RootExecutionResultNode) {
            Assert.assertTrue(normalizedFields.size() == 1, "only one normalized field expected");
            return normalizedFields.get(0);
        }
        ExecutionPath path = parent.getExecutionPath();
        List<String> parentQueryPath = executionPathToQueryPath(path);

        for (NormalizedQueryField normalizedField : normalizedFields) {
            NormalizedQueryField parentNormalizedField = normalizedField.getParent();
            if (!parentQueryPath.equals(parentNormalizedField.getPath())) {
                continue;
            }
            if (parentNormalizedField.getObjectType() == parent.getObjectType() &&
                    parentNormalizedField.getFieldDefinition() == parent.getFieldDefinition() &&
                    parentNormalizedField.getResultKey().equals(parent.getField().getResultKey())) {
                return normalizedField;
            }
        }
        return Assert.assertShouldNeverHappen("could not find matching normalized field");
    }

    private static List<String> executionPathToQueryPath(ExecutionPath executionPath) {
        return executionPath.toList()
                .stream()
                .filter(o -> o instanceof String)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    protected ExecutionResultNode replaceFieldWithOriginalValue(List<FieldTransformation> allTransformations,
                                                                ExecutionResultNode executionResultNode) {
        MergedField underlyingMergedField = executionResultNode.getField();
        List<Field> underlyingFields = underlyingMergedField.getFields();
        assertTrue(allTransformations.size() == underlyingFields.size());

        List<Field> newFields = new ArrayList<>();
        for (FieldTransformation fieldTransformation : allTransformations) {
            newFields.add(fieldTransformation.getOriginalField());
        }
        MergedField newMergedField = MergedField.newMergedField(newFields).build();

        FieldTransformation fieldTransformation = allTransformations.get(0);

        return executionResultNode.transform((builder -> builder.field(newMergedField)));
    }

    protected ExecutionResultNode mapToOverallFieldAndTypes(ExecutionResultNode node,
                                                            List<FieldTransformation> allTransformations,
                                                            NormalizedQueryField matchingNormalizedOverallField,
                                                            UnapplyEnvironment environment) {
        node = replaceFieldWithOriginalValue(allTransformations, node);
        node = node.transform(builder -> builder
                .objectType(matchingNormalizedOverallField.getObjectType())
                .fieldDefinition(matchingNormalizedOverallField.getFieldDefinition())
                .objectType(matchingNormalizedOverallField.getObjectType())
        );
        return node;
    }


}
