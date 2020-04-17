package graphql.nadel;

import graphql.Internal;


@Internal
public class NadelExecutionParams {

    private final String artificialFieldsUUID;

    public NadelExecutionParams(String artificialFieldsUUID) {
        this.artificialFieldsUUID = artificialFieldsUUID;
    }

    public String getArtificialFieldsUUID() {
        return artificialFieldsUUID;
    }
}
