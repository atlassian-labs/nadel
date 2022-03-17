package graphql.nadel.dsl;

import graphql.Internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static graphql.Assert.assertTrue;

@Internal
public class UnderlyingServiceHydration {

    public static class ObjectIdentifier {
        private final String sourceId;
        private final String resultId;

        public ObjectIdentifier(String sourceId, String resultId) {
            this.sourceId = sourceId;
            this.resultId = resultId;
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getResultId() {
            return resultId;
        }
    }

    private final String serviceName;
    private final String topLevelField;
    private final String syntheticField;
    private final List<RemoteArgumentDefinition> arguments;
    private final String objectIdentifier;
    private final List<ObjectIdentifier> objectIdentifiers;
    private final boolean objectIndexed;
    private final boolean batched;
    private final Integer batchSize;
    private final int timeout;

    public UnderlyingServiceHydration(
        String serviceName,
        String topLevelField,
        String syntheticField,
        List<RemoteArgumentDefinition> arguments,
        String objectIdentifier,
        List<ObjectIdentifier> objectIdentifiers,
        boolean objectIndexed,
        boolean batched,
        Integer batchSize,
        int timeout
    ) {
        assertTrue(!objectIndexed ^ objectIdentifier == null, () -> "An object identifier cannot be provided if the hydration is by index");

        this.serviceName = serviceName;
        this.topLevelField = topLevelField;
        this.arguments = arguments;
        this.objectIdentifier = objectIdentifier;
        this.objectIdentifiers = objectIdentifiers;
        this.objectIndexed = objectIndexed;
        this.batched = batched;
        this.batchSize = batchSize;
        this.timeout = timeout;
        this.syntheticField = syntheticField;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public int getTimeout() {
        return timeout;
    }

    @Deprecated
    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    /**
     * New complex hydration syntax.
     */
    public List<ObjectIdentifier> getObjectIdentifiers() {
        return objectIdentifiers;
    }

    public boolean isObjectMatchByIndex() {
        return objectIndexed;
    }

    public boolean isBatched() {
        return batched;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTopLevelField() {
        return topLevelField;
    }

    public List<RemoteArgumentDefinition> getArguments() {
        return new ArrayList<>(arguments);
    }

    @Override
    public String toString() {
        return "UnderlyingServiceHydration{" +
            "serviceName='" + serviceName + '\'' +
            ", topLevelField='" + topLevelField + '\'' +
            ", syntheticField='" + syntheticField + '\'' +
            ", arguments=" + arguments +
            ", objectIdentifier='" + objectIdentifier + '\'' +
            ", objectIdentifiers=" + objectIdentifiers +
            ", objectIndexed=" + objectIndexed +
            ", batched=" + batched +
            ", batchSize=" + batchSize +
            ", timeout=" + timeout +
            '}';
    }

    public String getSyntheticField() {
        return syntheticField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnderlyingServiceHydration that = (UnderlyingServiceHydration) o;
        return objectIndexed == that.objectIndexed
            && batched == that.batched
            && timeout == that.timeout
            && Objects.equals(serviceName, that.serviceName)
            && Objects.equals(topLevelField, that.topLevelField)
            && Objects.equals(syntheticField, that.syntheticField)
            && Objects.equals(arguments, that.arguments)
            && Objects.equals(objectIdentifier, that.objectIdentifier)
            && Objects.equals(objectIdentifiers, that.objectIdentifiers)
            && Objects.equals(batchSize, that.batchSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, topLevelField, syntheticField, arguments, objectIdentifier, objectIdentifiers, objectIndexed, batched, batchSize, timeout);
    }
}
