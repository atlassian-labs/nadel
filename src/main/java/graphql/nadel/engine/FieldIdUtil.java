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
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;

public class FieldIdUtil {

    private static final String NADEL_FIELD_ID = "NADEL_FIELD_ID";

    private static class NadelInfo implements Serializable {
        private final String id;
        private final boolean rootOfTransformation;

        public NadelInfo(String id, boolean rootOfTransformation) {
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
        String serialized = field.getAdditionalData().get(NADEL_FIELD_ID);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);

        return FpKit.filterAndMap(nadelInfos, NadelInfo::isRootOfTransformation, NadelInfo::getId);
    }

    public static List<String> getFieldIds(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_ID);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);
        return graphql.util.FpKit.map(nadelInfos, NadelInfo::getId);
    }

    public static Field addFieldId(Field field, String id, boolean rootOfTransformation) {
        assertNotNull(id);
        String serialized = field.getAdditionalData().get(NADEL_FIELD_ID);
        List<NadelInfo> nadelInfos = new ArrayList<>();
        if (serialized != null) {
            nadelInfos = readNadelInfos(serialized);
        }

        NadelInfo newNadelInfo = new NadelInfo(id, rootOfTransformation);
        nadelInfos.add(newNadelInfo);
        String newSerializedValue = writeNadelInfos(nadelInfos);
        return field.transform(builder -> builder.additionalData(NADEL_FIELD_ID, newSerializedValue));
    }

    public static String getUniqueRootFieldId(Field field) {
        String serialized = assertNotNull(field.getAdditionalData().get(NADEL_FIELD_ID), "nadel field id expected");
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);
        List<NadelInfo> rootNadelInfos = nadelInfos.stream().filter(nadelInfo -> nadelInfo.rootOfTransformation).collect(Collectors.toList());
        assertTrue(rootNadelInfos.size() == 1, "exactly one root nadel infos expected");
        return rootNadelInfos.get(0).id;
    }

    public static void setFieldId(Field.Builder builder, String id, boolean rootOfTransformation) {
        assertNotNull(id);
        List<NadelInfo> nadelInfos = new ArrayList<>();

        NadelInfo newNadelInfo = new NadelInfo(id, rootOfTransformation);
        nadelInfos.add(newNadelInfo);
        String newSerializedValue = writeNadelInfos(nadelInfos);
        builder.additionalData(NADEL_FIELD_ID, newSerializedValue);

    }

    private static String writeNadelInfos(List<NadelInfo> nadelInfos) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(nadelInfos);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<NadelInfo> readNadelInfos(String serialized) {
        try {
            byte[] decoded = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (List<NadelInfo>) ois.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
