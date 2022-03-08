package graphql.nadel;

import graphql.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public class NadelExecutionParams {

    @NotNull
    private final NadelExecutionHints nadelExecutionHints;

    public NadelExecutionParams(@NotNull NadelExecutionHints nadelExecutionHints) {
        this.nadelExecutionHints = nadelExecutionHints;
    }

    @NotNull
    public NadelExecutionHints getNadelExecutionHints() {
        return nadelExecutionHints;
    }
}
