package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.util.Data;

import java.util.ArrayList;
import java.util.List;

public class RemovedFieldData {

    private List<Data> removedFields = new ArrayList<>();

    public void add(List<NormalizedQueryField> fields, GraphQLError error) {
        for (NormalizedQueryField field : fields) {
            removedFields.add(Data.newData().set(error).set(field).build());
        }
    }

    public List<NormalizedQueryField> getChildsForMergedField(MergedField mergedField) {
        for (Data data : removedFields) {
            NormalizedQueryField normalizedQueryField = data.get(NormalizedQueryField.class);
//            GraphQLError normalizedQueryField = data.get(NormalizedQueryField.class);
            List<Field> fields = normalizedQueryField.getParent().getMergedField().getFields();

        }
        return null;
    }
}
