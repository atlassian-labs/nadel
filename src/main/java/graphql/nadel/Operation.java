package graphql.nadel;

public enum Operation {
    QUERY("query", "Query"),
    MUTATION("mutation", "Mutation"),
    SUBSCRIPTION("subscription", "Subscription");

    private String name;
    private String displayName;
    private Operation(String name, String displayName){
        this.displayName = displayName;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

}