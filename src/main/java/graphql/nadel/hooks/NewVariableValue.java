package graphql.nadel.hooks;

public class NewVariableValue {

    private final String name;
    private final Object value;

    public NewVariableValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
