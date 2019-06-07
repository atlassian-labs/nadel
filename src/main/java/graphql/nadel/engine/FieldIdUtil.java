package graphql.nadel.engine;

import graphql.language.Field;

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
    }

    public static List<String> getRootOfTransformationIds(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_ID);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);

        return nadelInfos.stream()
                .filter(nadelInfo -> nadelInfo.rootOfTransformation)
                .map(nadelInfo -> nadelInfo.id)
                .collect(Collectors.toList());
    }

    public static List<String> getFieldIds(Field field) {
        String serialized = field.getAdditionalData().get(NADEL_FIELD_ID);
        if (serialized == null) {
            return Collections.emptyList();
        }
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);
        return nadelInfos.stream()
                .map(nadelInfo -> nadelInfo.id)
                .collect(Collectors.toList());
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

    public static String getUniqueFieldId(Field field) {
        String serialized = assertNotNull(field.getAdditionalData().get(NADEL_FIELD_ID), "nadel field id expected");
        List<NadelInfo> nadelInfos = readNadelInfos(serialized);
        assertTrue(nadelInfos.size() == 1, "exactly one nadel infos expected");
        NadelInfo nadelInfo = nadelInfos.get(0);
        return nadelInfo.id;
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
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(nadelInfos);
            oos.close();
            baos.close();
            String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
            return encoded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static List<NadelInfo> readNadelInfos(String serialized) {
        try {
            byte[] decoded = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bais);
            List<NadelInfo> nadelInfos = (List<NadelInfo>) ois.readObject();
            return nadelInfos;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
