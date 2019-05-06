package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;

import java.util.List;
import java.util.Map;

public class QueryTransformationResult {

    private final Document document;
    private final OperationDefinition operationDefinition;

    private final List<MergedField> transformedMergedFields;
    private final List<String> referencedVariables;

    private final Map<Field, FieldTransformation> transformationByResultField;
    private final Map<String, FragmentDefinition> transformedFragments;

    public QueryTransformationResult(Document document,
                                     OperationDefinition operationDefinition,
                                     List<MergedField> transformedMergedFields,
                                     List<String> referencedVariables,
                                     Map<Field, FieldTransformation> transformationByResultField,
                                     Map<String, FragmentDefinition> transformedFragments) {
        this.document = document;
        this.operationDefinition = operationDefinition;
        this.transformedMergedFields = transformedMergedFields;
        this.referencedVariables = referencedVariables;
        this.transformationByResultField = transformationByResultField;
        this.transformedFragments = transformedFragments;
    }

    public Document getDocument() {
        return document;
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public List<MergedField> getTransformedMergedFields() {
        return transformedMergedFields;
    }

    public List<String> getReferencedVariables() {
        return referencedVariables;
    }

    public Map<Field, FieldTransformation> getTransformationByResultField() {
        return transformationByResultField;
    }

    public Map<String, FragmentDefinition> getTransformedFragments() {
        return transformedFragments;
    }
}

