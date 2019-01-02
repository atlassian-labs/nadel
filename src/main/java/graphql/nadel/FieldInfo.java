package graphql.nadel;

public class FieldInfo {

    public enum FieldType {
        ARTIFICIAL,
        TOPLEVEL,
        HYDRATED,
    }

    private final FieldType fieldType;

    public FieldInfo(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType getFieldType() {
        return fieldType;
    }
}
