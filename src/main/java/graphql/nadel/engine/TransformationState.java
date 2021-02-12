package graphql.nadel.engine;

import graphql.nadel.engine.transformation.FieldTransformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransformationState {
    final Map<String, FieldTransformation> fieldIdToTransformation;
    final Map<String, String> typeRenameMappings;
    final List<String> hintTypenames;

    public TransformationState() {
        this.fieldIdToTransformation = new LinkedHashMap<>();
        this.typeRenameMappings = new LinkedHashMap<>();
        this.hintTypenames = new ArrayList<>();
    }

    public Map<String, FieldTransformation> getFieldIdToTransformation() {
        return fieldIdToTransformation;
    }

    public Map<String, String> getTypeRenameMappings() {
        return typeRenameMappings;
    }

    public List<String> getHintTypenames() {
        return hintTypenames;
    }

    public void putFieldIdToTransformation(String fieldId, FieldTransformation transformation) {
        fieldIdToTransformation.put(fieldId, transformation);
    }

    public void putTypeRenameMapping(String underlyingName, String overallName) {
        typeRenameMappings.put(underlyingName, overallName);
    }

    public void addHintTypename(String hintTypename) {
        hintTypenames.add(hintTypename);
    }
}
