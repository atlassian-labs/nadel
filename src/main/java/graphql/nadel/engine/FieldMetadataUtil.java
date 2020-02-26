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
        List<FieldMetadata> fieldMetadata = readMetadata(field);
        return FpKit.filterAndMap(fieldMetadata, FieldMetadata::isRootOfTransformation, FieldMetadata::getId);
    }

    public static List<String> getFieldIds(Field field) {
        List<FieldMetadata> fieldMetadata = readMetadata(field);
        return graphql.util.FpKit.map(fieldMetadata, FieldMetadata::getId);
    }

    public static Field addFieldMetadata(Field field, String id, boolean rootOfTransformation) {
        List<FieldMetadata> fieldMetadata = readMetadata(field);
        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        return writeMetadata(field, fieldMetadata);
    }

    public static Field copyFieldMetadata(Field from, Field to) {
        List<FieldMetadata> fromMetadata = readMetadata(from);
        if (fromMetadata.isEmpty()) {
            return to;
        }
        List<FieldMetadata> toMetadata = readMetadata(to);
        toMetadata.addAll(fromMetadata);
        return writeMetadata(to, toMetadata);
    }

    public static String getUniqueRootFieldId(Field field) {
        List<FieldMetadata> fieldMetadata = readMetadata(field);
        List<FieldMetadata> rootFieldMetadata = filter(fieldMetadata, FieldMetadata::isRootOfTransformation);
        assertTrue(rootFieldMetadata.size() == 1, "exactly one root nadel infos expected");
        return rootFieldMetadata.get(0).id;
    }

    public static void setFieldMetadata(Field.Builder builder, String id, List<String> additionalIds, boolean rootOfTransformation) {
        assertNotNull(id);
        List<FieldMetadata> fieldMetadata = new ArrayList<>();

        FieldMetadata newFieldMetadata = new FieldMetadata(id, rootOfTransformation);
        fieldMetadata.add(newFieldMetadata);
        for (String additionalId : additionalIds) {
            fieldMetadata.add(new FieldMetadata(additionalId, false));
        }
        writeMetadata(builder, fieldMetadata);
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

    private static Field writeMetadata(Field field, List<FieldMetadata> newMetadata) {
        return field.transform(builder -> writeMetadata(builder, newMetadata));
    }

    private static Field.Builder writeMetadata(Field.Builder builder, List<FieldMetadata> newMetadata) {
        String newSerializedValue = writeMetadata(newMetadata);
        return builder.additionalData(NADEL_FIELD_METADATA, newSerializedValue);
    }

    private static List<FieldMetadata> readMetadata(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_METADATA);
        return serialized != null ? readMetadata(serialized) : new ArrayList<>();
    }


}
