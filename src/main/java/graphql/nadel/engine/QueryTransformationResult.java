package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;

import java.util.List;
import java.util.Map;

public class QueryTransformationResult {

    private final Document document;

    private final List<MergedField> transformedMergedFields;
    private final List<String> referencedVariables;
    private final Field transformedField;

    private final Map<Field, FieldTransformation> transformationByResultField;
    private final Map<String, FragmentDefinition> transformedFragments;

    public QueryTransformationResult(Document document,
                                     List<MergedField> transformedMergedFields,
                                     List<String> referencedVariables,
                                     Field transformedField,
                                     Map<Field, FieldTransformation> transformationByResultField,
                                     Map<String, FragmentDefinition> transformedFragments) {
        this.document = document;
        this.transformedMergedFields = transformedMergedFields;
        this.referencedVariables = referencedVariables;
        this.transformedField = transformedField;
        this.transformationByResultField = transformationByResultField;
        this.transformedFragments = transformedFragments;
    }

    public Document getDocument() {
        return document;
    }

    public List<MergedField> getTransformedMergedFields() {
        return transformedMergedFields;
    }

    public List<String> getReferencedVariables() {
        return referencedVariables;
    }

    public Field getTransformedField() {
        return transformedField;
    }

    public Map<Field, FieldTransformation> getTransformationByResultField() {
        return transformationByResultField;
    }

    public Map<String, FragmentDefinition> getTransformedFragments() {
        return transformedFragments;
    }
}

