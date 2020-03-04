package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.ArrayList;
import java.util.List;

public class RemovedFieldData {

    private List<NormalizedFieldAndError> removedFields = new ArrayList<>();

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
}
