package graphql.nadel.engine.transformation;

public class FieldMetadata {
    private final String transformationId;
    private final boolean rootOfTransformation;

    public FieldMetadata(String transformationId, boolean rootOfTransformation) {
        this.transformationId = transformationId;
        this.rootOfTransformation = rootOfTransformation;
    }

    public String getTransformationId() {
        return transformationId;
    }

    public boolean isRootOfTransformation() {
        return rootOfTransformation;
    }


    @Override
    public String toString() {
        return "FieldMetadata{" +
                "transformationId='" + transformationId + '\'' +
                ", rootOfTransformation=" + rootOfTransformation +
                '}';
    }
}

