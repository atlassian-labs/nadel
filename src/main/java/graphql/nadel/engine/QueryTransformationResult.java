package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.RemovedFieldData;

import java.util.List;
import java.util.Map;

public class QueryTransformationResult {

    private final Document document;
    // will be provided to the ServiceExecution
    private final OperationDefinition operationDefinition;
    // will be provided to the ServiceExecution
    private final Map<String, FragmentDefinition> transformedFragments;
    // used to only pass down the references variables to ServiceExecution
    private final List<String> referencedVariables;

    // used when the underlying raw result is converted into a tree
    private final List<MergedField> transformedMergedFields;

    // needed when the underlying result tree is mapped back
    private final Map<String, FieldTransformation> transformationByResultField;
    // needed when the underlying result tree is mapped back
    private final Map<String, String> typeRenameMappings;

    private final Map<String, Object> variableValues;

    private final RemovedFieldData removedFieldMap;

    public QueryTransformationResult(Document document,
                                     OperationDefinition operationDefinition,
                                     List<MergedField> transformedMergedFields,
                                     Map<String, String> typeRenameMappings,
                                     List<String> referencedVariables,
                                     Map<String, FieldTransformation> transformationByResultField,
                                     Map<String, FragmentDefinition> transformedFragments,
                                     Map<String, Object> variableValues,
                                     RemovedFieldData removedFieldMap) {
        this.document = document;
        this.operationDefinition = operationDefinition;
        this.transformedMergedFields = transformedMergedFields;
        this.referencedVariables = referencedVariables;
        this.transformationByResultField = transformationByResultField;
        this.transformedFragments = transformedFragments;
        this.typeRenameMappings = typeRenameMappings;
        this.variableValues = variableValues;
        this.removedFieldMap = removedFieldMap;
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

    public Map<String, FieldTransformation> getTransformationByResultField() {
        return transformationByResultField;
    }

    public Map<String, FragmentDefinition> getTransformedFragments() {
        return transformedFragments;
    }

    public Map<String, String> getTypeRenameMappings() {
        return typeRenameMappings;
    }

    public Map<String, Object> getVariableValues() {
        return variableValues;
    }

    public RemovedFieldData getRemovedFieldMap() {
        return removedFieldMap;
    }
}

