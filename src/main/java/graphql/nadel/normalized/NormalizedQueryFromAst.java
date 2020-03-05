package graphql.nadel.normalized;

import graphql.Internal;
import graphql.execution.MergedField;

import java.util.List;
import java.util.Map;

@Internal
public class NormalizedQueryFromAst {

    private final List<NormalizedQueryField> rootFields;
    private final Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId;
    private final Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedFields;

    public NormalizedQueryFromAst(List<NormalizedQueryField> rootFields,
                                  Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId,
                                  Map<NormalizedQueryField, MergedField> mergedFieldByNormalizedFields) {
        this.rootFields = rootFields;
        this.normalizedFieldsByFieldId = normalizedFieldsByFieldId;
        this.mergedFieldByNormalizedFields = mergedFieldByNormalizedFields;
    }

    public List<NormalizedQueryField> getRootFields() {
        return rootFields;
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
}
