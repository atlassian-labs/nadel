package graphql.nadel.engine;

import java.util.List;

public abstract class ExecutionAction {

    private final List<ExecutionAction> nextActions;

    public ExecutionAction(List<ExecutionAction> nextActions) {
        this.nextActions = nextActions;
    }


    public List<ExecutionAction> getNextActions() {
        return nextActions;
    }
}
