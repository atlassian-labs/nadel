package graphql.nadel.engine;

import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldMetadata;
import graphql.nadel.util.FpKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.nadel.util.FpKit.filter;

public class FieldMetadataUtil {


    public static List<String> getRootOfTransformationIds(Field field, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        return FpKit.filterAndMap(fieldMetadata, FieldMetadata::isRootOfTransformation, FieldMetadata::getTransformationId);
    }

    public static List<String> getTransformationIds(Field field, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        return graphql.util.FpKit.map(fieldMetadata, FieldMetadata::getTransformationId);
    }

    public static void addFieldMetadata(Field field, String transformationId, boolean rootOfTransformation, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        FieldMetadata newFieldMetadata = new FieldMetadata(transformationId, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        writeMetadata(field, fieldMetadata, metadataByFieldId);
    }

    public static void copyFieldMetadata(Field from, Field to, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fromMetadata = readMetadata(from, metadataByFieldId);
        if (fromMetadata.isEmpty()) {
            return;
        }
        List<FieldMetadata> toMetadata = readMetadata(to, metadataByFieldId);
        toMetadata.addAll(fromMetadata);
        writeMetadata(to, toMetadata, metadataByFieldId);
    }

    public static String getUniqueRootFieldId(Field field, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        List<FieldMetadata> rootFieldMetadata = filter(fieldMetadata, FieldMetadata::isRootOfTransformation);
        assertTrue(rootFieldMetadata.size() == 1, "exactly one root info expected");
        return rootFieldMetadata.get(0).getTransformationId();
    }

    public static void setFieldMetadata(String fieldId,
                                        String transformationId,
                                        List<String> additionalIds,
                                        boolean rootOfTransformation,
                                        Map<String, List<FieldMetadata>> metadataByFieldId) {
        assertNotNull(transformationId);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();

        FieldMetadata newFieldMetadata = new FieldMetadata(transformationId, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        for (String additionalId : additionalIds) {
            fieldMetadata.add(new FieldMetadata(additionalId, false));
        }
        metadataByFieldId.put(fieldId, fieldMetadata);
    }


    private static void writeMetadata(Field field, List<FieldMetadata> newMetadata, Map<String, List<FieldMetadata>> metadataByFieldId) {
        metadataByFieldId.put(getId(field), newMetadata);
    }


    private static List<FieldMetadata> readMetadata(Field field, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> result = metadataByFieldId.get(getId(field));
        return result != null ? result : new ArrayList<>();
    }


}
