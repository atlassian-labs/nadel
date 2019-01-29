package graphql.nadel;

public enum OperationType {
    QUERY("query", "Query"),
    MUTATION("mutation", "Mutation"),
    SUBSCRIPTION("subscription", "Subscription");

    private String opsType;
    private String displayName;
    private OperationType(String opsType, String displayName){
        this.displayName = displayName;
        this.opsType = opsType;
    }

    public String getOpsType() {
        return opsType;
    }

    public String getDisplayName() {
        return displayName;
    }

}
