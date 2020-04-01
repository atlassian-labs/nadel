package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.execution.ExecutionPath;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Metadata {

    private List<NormalizedFieldAndError> removedFields = new ArrayList<>();

    private Map<String, List<FieldMetadata>> metadataByFieldId = new LinkedHashMap<>();

    private final Set<ExecutionPath> processedTransformation = Collections.synchronizedSet(new LinkedHashSet<>());

    public static class NormalizedFieldAndError {
        private final NormalizedQueryField normalizedField;
        private final GraphQLError error;

        public NormalizedFieldAndError(NormalizedQueryField normalizedField, GraphQLError error) {
            this.normalizedField = normalizedField;
            this.error = error;
        }

        public NormalizedQueryField getNormalizedField() {
            return normalizedField;
        }

        public GraphQLError getError() {
            return error;
        }
    }

    public void add(List<NormalizedQueryField> fields, GraphQLError error) {
        for (NormalizedQueryField field : fields) {
            removedFields.add(new NormalizedFieldAndError(field, error));
        }
    }

    public List<NormalizedFieldAndError> getRemovedFieldsForParent(NormalizedQueryField parent) {
        List<NormalizedFieldAndError> result = new ArrayList<>();
        for (NormalizedFieldAndError fieldAndError : removedFields) {
            if (fieldAndError.normalizedField.getParent() == parent) {
                result.add(fieldAndError);
            }
        }
        return result;
    }

    public Map<String, List<FieldMetadata>> getMetadataByFieldId() {
        return metadataByFieldId;
    }

    public void addProcessedTransformation(ExecutionPath executionPath) {
//        System.out.println("processed path: " + executionPath);
        processedTransformation.add(executionPath);
    }

    public boolean isTransformationProcessed(ExecutionPath path) {
//        System.out.println(" checking : " + path);
        return processedTransformation.contains(path);
    }
}

