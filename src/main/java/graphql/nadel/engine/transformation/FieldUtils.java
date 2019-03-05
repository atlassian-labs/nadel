package graphql.nadel.engine.transformation;

import graphql.language.Field;

public final class FieldUtils {
    public static String resultKeyForField(Field field) {
        return field.getAlias() != null ? field.getAlias() : field.getName();
    }

}
