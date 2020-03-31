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
        return FpKit.filterAndMap(fieldMetadata, FieldMetadata::isRootOfTransformation, FieldMetadata::getId);
    }

    public static List<String> getFieldIds(Field field, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        return graphql.util.FpKit.map(fieldMetadata, FieldMetadata::getId);
    }

    public static void addFieldMetadata(Field field, String id, boolean rootOfTransformation, Map<String, List<FieldMetadata>> metadataByFieldId) {
        List<FieldMetadata> fieldMetadata = readMetadata(field, metadataByFieldId);
        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
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
        assertTrue(rootFieldMetadata.size() == 1, "exactly one root nadel infos expected");
        return rootFieldMetadata.get(0).getId();
    }

    public static void setFieldMetadata(Field field,
                                        String id,
                                        List<String> additionalIds,
                                        boolean rootOfTransformation,
                                        Map<String, List<FieldMetadata>> metadataByFieldId) {
        assertNotNull(id);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();

        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        for (String additionalId : additionalIds) {
            fieldMetadata.add(new FieldMetadata(additionalId, false));
        }
        writeMetadata(field, fieldMetadata, metadataByFieldId);
    }

    public static void setFieldMetadata(String fieldId,
                                        String id,
                                        List<String> additionalIds,
                                        boolean rootOfTransformation,
                                        Map<String, List<FieldMetadata>> metadataByFieldId) {
        assertNotNull(id);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();

        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
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
