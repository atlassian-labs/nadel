package graphql.nadel.dsl;

public class FieldReference {
    private String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public FieldReference(String fieldName) {
        this.fieldName = fieldName;
    }
}
