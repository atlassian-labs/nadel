package graphql.nadel.engine;

import graphql.language.Field;
import graphql.nadel.util.FpKit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.nadel.util.FpKit.filter;

public class FieldMetadataUtil {

    private static final String NADEL_FIELD_METADATA = "NADEL_FIELD_METADATA";

    private static class FieldMetadata implements Serializable {
        private final String id;
        private final boolean rootOfTransformation;

        public FieldMetadata(String id, boolean rootOfTransformation) {
            this.id = id;
            this.rootOfTransformation = rootOfTransformation;
        }

        public String getId() {
            return id;
        }

        public boolean isRootOfTransformation() {
            return rootOfTransformation;
        }

    }

    public static List<String> getRootOfTransformationIds(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_METADATA);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<FieldMetadata> fieldMetadata = readMetadata(serialized);

        return FpKit.filterAndMap(fieldMetadata, FieldMetadata::isRootOfTransformation, FieldMetadata::getId);
    }

//    public static boolean skipTraversing(Field field) {
//        String serialized = field.getAdditionalData().get(NADEL_FIELD_METADATA);
//        if (serialized == null) {
//            return false;
//        }
//        List<FieldMetadata> fieldMetadata = readMetadata(serialized);
//        return fieldMetadata.stream().anyMatch(FieldMetadata::isSkipTraversing);
//    }

    public static List<String> getFieldIds(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_METADATA);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<FieldMetadata> fieldMetadata = readMetadata(serialized);
        return graphql.util.FpKit.map(fieldMetadata, FieldMetadata::getId);
    }

    public static Field addFieldMetadata(Field field, String id, boolean rootOfTransformation) {
        assertNotNull(id);
        String serialized = field.getAdditionalData().get(NADEL_FIELD_METADATA);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();
        if (serialized != null) {
            fieldMetadata = readMetadata(serialized);
        }

        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        String newSerializedValue = writeMetadata(fieldMetadata);
        return field.transform(builder -> builder.additionalData(NADEL_FIELD_METADATA, newSerializedValue));
    }

    public static String getUniqueRootFieldId(Field field) {
        String serialized = assertNotNull(field.getAdditionalData().get(NADEL_FIELD_METADATA), "nadel field id expected");
        List<FieldMetadata> fieldMetadata = readMetadata(serialized);
        List<FieldMetadata> rootFieldMetadata = filter(fieldMetadata, FieldMetadata::isRootOfTransformation);
        assertTrue(rootFieldMetadata.size() == 1, "exactly one root nadel infos expected");
        return rootFieldMetadata.get(0).id;
    }

    public static void setFieldMetadata(Field.Builder builder, String id, boolean rootOfTransformation) {
        assertNotNull(id);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();

        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        String newSerializedValue = writeMetadata(fieldMetadata);
        builder.additionalData(NADEL_FIELD_METADATA, newSerializedValue);

    }

    private static String writeMetadata(List<FieldMetadata> fieldMetadata) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(fieldMetadata);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<FieldMetadata> readMetadata(String serialized) {
        try {
            byte[] decoded = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (List<FieldMetadata>) ois.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
