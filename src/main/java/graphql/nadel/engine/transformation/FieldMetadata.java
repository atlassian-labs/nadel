package graphql.nadel.engine.transformation;

public class FieldMetadata {
    private final String id;
    private final boolean rootOfTransformation;

    public FieldMetadata(String id, boolean rootOfTransformation) {
        this.id = id;
        this.rootOfTransformation = rootOfTransformation;
    }

    public String getId() {
        return id;
    }

    public boolean isRootOfTransformation() {
        return rootOfTransformation;
    }

}

