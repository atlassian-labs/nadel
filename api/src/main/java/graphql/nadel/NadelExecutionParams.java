package graphql.nadel;

import graphql.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public class NadelExecutionParams {

    private final String artificialFieldsUUID;
    @NotNull
    private final NadelExecutionHints nadelExecutionHints;

    public NadelExecutionParams(String artificialFieldsUUID,
                                @NotNull NadelExecutionHints nadelExecutionHints) {
        this.artificialFieldsUUID = artificialFieldsUUID;
        this.nadelExecutionHints = nadelExecutionHints;
    }

    public String getArtificialFieldsUUID() {
        return artificialFieldsUUID;
    }

    @NotNull
    public NadelExecutionHints getNadelExecutionHints() {
        return nadelExecutionHints;
    }
}
