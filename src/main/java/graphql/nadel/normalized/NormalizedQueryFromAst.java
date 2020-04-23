package graphql.nadel.normalized;

import graphql.Internal;
import graphql.execution.MergedField;
import graphql.nadel.dsl.NodeId;

import java.util.List;
import java.util.Map;

@Internal
public class NormalizedQueryFromAst {

    private final List<NormalizedQueryField> topLevelFields;
    private final Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId;
    private final Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedFields;

    public NormalizedQueryFromAst(List<NormalizedQueryField> topLevelFields,
                                  Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId,
                                  Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedFields) {
        this.topLevelFields = topLevelFields;
        this.normalizedFieldsByFieldId = normalizedFieldsByFieldId;
        this.mergedFieldByNormalizedFields = mergedFieldByNormalizedFields;
    }

    public List<NormalizedQueryField> getTopLevelFields() {
        return topLevelFields;
    }

    public Map<String, List<NormalizedQueryField>> getNormalizedFieldsByFieldId() {
        return normalizedFieldsByFieldId;
    }

    public List<NormalizedQueryField> getNormalizedFieldsByFieldId(String astFieldId) {
        return normalizedFieldsByFieldId.get(astFieldId);
    }

    public Map<NormalizedQueryField, MergedField> getMergedFieldByNormalizedFields() {
        return mergedFieldByNormalizedFields;
    }

    public List<String> getFieldIds(NormalizedQueryField normalizedQueryField) {
        MergedField mergedField = mergedFieldByNormalizedFields.get(normalizedQueryField);
        return NodeId.getIds(mergedField);
    }
}
