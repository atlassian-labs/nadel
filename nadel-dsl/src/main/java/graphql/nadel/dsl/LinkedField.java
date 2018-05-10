package graphql.nadel.dsl;

public class LinkedField {

    private String fieldName;
    private String topLevelQueryField;
    private String variableName;
    private String parentType;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTopLevelQueryField() {
        return topLevelQueryField;
    }

    public void setTopLevelQueryField(String topLevelQueryField) {
        this.topLevelQueryField = topLevelQueryField;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    @Override
    public String toString() {
        return "LinkedField{" +
                "fieldName='" + fieldName + '\'' +
                ", topLevelQueryField='" + topLevelQueryField + '\'' +
                ", variableName='" + variableName + '\'' +
                '}';
    }
}
