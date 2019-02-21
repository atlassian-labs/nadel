package graphql.nadel.engine;

import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldTransformation;

import java.util.List;
import java.util.Map;

public class QueryTransformationResult {

    private final Document document;

    private final List<MergedField> transformedMergedFields;
    private final Field transformedField;

    private final Map<Field, FieldTransformation> transformationByResultField;

    public QueryTransformationResult(Document document, List<MergedField> transformedMergedFields, Field transformedField, Map<Field, FieldTransformation> transformationByResultField) {
        this.document = document;
        this.transformedMergedFields = transformedMergedFields;
        this.transformedField = transformedField;
        this.transformationByResultField = transformationByResultField;
    }

    public Document getDocument() {
        return document;
    }

    public List<MergedField> getTransformedMergedFields() {
        return transformedMergedFields;
    }

    public Field getTransformedField() {
        return transformedField;
    }

    public Map<Field, FieldTransformation> getTransformationByResultField() {
        return transformationByResultField;
    }
}

