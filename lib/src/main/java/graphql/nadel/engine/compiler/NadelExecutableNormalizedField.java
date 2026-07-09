package graphql.nadel.engine.compiler;

import graphql.ExperimentalApi;
import graphql.Internal;
import graphql.Mutable;
import graphql.PublicApi;
import graphql.introspection.Introspection;
import graphql.language.Argument;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;
import graphql.normalized.incremental.NormalizedDeferredExecution;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnionType;
import graphql.util.FpKit;
import graphql.util.MutableRef;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.schema.GraphQLTypeUtil.simplePrint;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Nadel fork of graphql-java's {@link graphql.normalized.ExecutableNormalizedField} (pinned version
 * 0.0.0-2026-03-18T00-52-57-e53ab1a). Verbatim copy of the upstream field model plus ONE addition: a mutable
 * {@code forcePrintAsUnconditional} flag that lives ON the field. When set, the Nadel compiler emits the field
 * bare (no {@code ... on Type} inline fragment) even though {@link #isConditional(GraphQLSchema)} is true. This
 * is the "state on the field" alternative to carrying the signal in an external identity set (GQLGW-6377).
 *
 * <p>graphql-java's {@code ExecutableNormalizedField} has a private constructor, so it cannot be subclassed; the
 * whole type must be forked to add a field. The tree originates from graphql-java's factory as upstream
 * {@link ExecutableNormalizedField}; {@link #fromExecutableNormalizedField(ExecutableNormalizedField)} deep-copies
 * it into this type at the engine seam, after which the Nadel pipeline operates on this type exclusively.
 *
 * <p>Keep in sync when bumping graphql-java.
 */
@PublicApi
@Mutable
public class NadelExecutableNormalizedField {
    private final String alias;
    private final Map<String, NormalizedInputValue> normalizedArguments;
    private final LinkedHashMap<String, Object> resolvedArguments;
    private final List<Argument> astArguments;

    // Mutable List on purpose: it is modified after creation
    private final LinkedHashSet<String> objectTypeNames;
    private final ArrayList<NadelExecutableNormalizedField> children;
    private NadelExecutableNormalizedField parent;

    private final String fieldName;
    private final int level;

    // Nadel addition: when true, the compiler emits this field bare even when isConditional() is true.
    private boolean forcePrintAsUnconditional;

    // Nadel addition: the graphql-java field this was converted from (null for synthetic/rebuilt fields). Needed
    // to query the un-forked graphql.normalized.ExecutableNormalizedOperation maps (e.g. getMergedField), which
    // are keyed by graphql-java field identity. A cost of forking the field but not the operation.
    private final @Nullable ExecutableNormalizedField source;

    // Mutable List on purpose: it is modified after creation
    private final LinkedHashSet<NormalizedDeferredExecution> deferredExecutions;

    private NadelExecutableNormalizedField(Builder builder) {
        this.alias = builder.alias;
        this.resolvedArguments = builder.resolvedArguments;
        this.normalizedArguments = builder.normalizedArguments;
        this.astArguments = builder.astArguments;
        this.objectTypeNames = builder.objectTypeNames;
        this.fieldName = assertNotNull(builder.fieldName);
        this.children = builder.children;
        this.level = builder.level;
        this.parent = builder.parent;
        this.deferredExecutions = builder.deferredExecutions;
        this.forcePrintAsUnconditional = builder.forcePrintAsUnconditional;
        this.source = builder.source;
    }

    /**
     * Deep-copies a graphql-java {@link ExecutableNormalizedField} tree into a {@link NadelExecutableNormalizedField}
     * tree, wiring parent/child links. {@code forcePrintAsUnconditional} defaults to false on every node.
     *
     * @param src the graphql-java field (root of a subtree)
     *
     * @return an equivalent Nadel field tree
     */
    public static NadelExecutableNormalizedField fromExecutableNormalizedField(ExecutableNormalizedField src) {
        return fromExecutableNormalizedField(src, null);
    }

    private static NadelExecutableNormalizedField fromExecutableNormalizedField(ExecutableNormalizedField src,
                                                                               @Nullable NadelExecutableNormalizedField parent) {
        NadelExecutableNormalizedField copy = newNormalizedField()
                .alias(src.getAlias())
                .normalizedArguments(src.getNormalizedArguments())
                .resolvedArguments(src.getResolvedArguments())
                .astArguments(src.getAstArguments())
                .objectTypeNames(new ArrayList<>(src.getObjectTypeNames()))
                .fieldName(src.getFieldName())
                .level(src.getLevel())
                .parent(parent)
                .deferredExecutions(new LinkedHashSet<>(src.getDeferredExecutions()))
                .source(src)
                .build();

        List<NadelExecutableNormalizedField> convertedChildren = new ArrayList<>(src.getChildren().size());
        for (ExecutableNormalizedField child : src.getChildren()) {
            convertedChildren.add(fromExecutableNormalizedField(child, copy));
        }
        copy.children.addAll(convertedChildren);
        return copy;
    }

    /**
     * See {@link graphql.normalized.ExecutableNormalizedField#isConditional(GraphQLSchema)}. Identical logic.
     *
     * @param schema - the graphql schema in play
     *
     * @return true if the field is conditional
     */
    public boolean isConditional(@NonNull GraphQLSchema schema) {
        if (parent == null) {
            return false;
        }

        for (GraphQLInterfaceType commonParentOutputInterface : parent.getInterfacesCommonToAllOutputTypes(schema)) {
            List<GraphQLObjectType> implementations = schema.getImplementations(commonParentOutputInterface);
            // __typename
            if (fieldName.equals(Introspection.TypeNameMetaFieldDef.getName()) && implementations.size() == objectTypeNames.size()) {
                return false;
            }
            if (commonParentOutputInterface.getField(fieldName) == null) {
                continue;
            }
            if (implementations.size() == objectTypeNames.size()) {
                return false;
            }
        }

        // __typename is the only field in a union type that CAN be NOT conditional
        GraphQLFieldDefinition parentFieldDef = parent.getOneFieldDefinition(schema);
        if (unwrapAll(parentFieldDef.getType()) instanceof GraphQLUnionType) {
            GraphQLUnionType parentOutputTypeAsUnion = (GraphQLUnionType) unwrapAll(parentFieldDef.getType());
            if (fieldName.equals(Introspection.TypeNameMetaFieldDef.getName()) && objectTypeNames.size() == parentOutputTypeAsUnion.getTypes().size()) {
                return false; // Not conditional
            }
        }

        // This means there is no Union or Interface which could serve as unconditional parent
        if (objectTypeNames.size() > 1) {
            return true; // Conditional
        }
        if (parent.objectTypeNames.size() > 1) {
            return true;
        }

        GraphQLObjectType oneObjectType = (GraphQLObjectType) schema.getType(objectTypeNames.iterator().next());
        return unwrapAll(parentFieldDef.getType()) != oneObjectType;
    }

    /**
     * @return whether this field should be printed bare (unconditional) by the Nadel compiler regardless of
     * {@link #isConditional(GraphQLSchema)}.
     */
    public boolean isForcePrintAsUnconditional() {
        return forcePrintAsUnconditional;
    }

    /**
     * Sets the {@link #isForcePrintAsUnconditional()} flag on this field.
     */
    public void setForcePrintAsUnconditional(boolean forcePrintAsUnconditional) {
        this.forcePrintAsUnconditional = forcePrintAsUnconditional;
    }

    /**
     * @return the graphql-java {@link ExecutableNormalizedField} this was converted from, or null for synthetic
     * or rebuilt fields. Use to look up entries in the un-forked {@link graphql.normalized.ExecutableNormalizedOperation}.
     */
    public @Nullable ExecutableNormalizedField getSourceField() {
        return source;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public GraphQLOutputType getType(GraphQLSchema schema) {
        List<GraphQLFieldDefinition> fieldDefinitions = getFieldDefinitions(schema);
        Set<String> fieldTypes = fieldDefinitions.stream().map(fd -> simplePrint(fd.getType())).collect(toSet());
        assertTrue(fieldTypes.size() == 1, "More than one type ... use getTypes");
        return fieldDefinitions.get(0).getType();
    }

    public List<GraphQLOutputType> getTypes(GraphQLSchema schema) {
        return getFieldDefinitions(schema).stream().map(GraphQLFieldDefinition::getType).collect(Collectors.toList());
    }

    public void forEachFieldDefinition(GraphQLSchema schema, Consumer<GraphQLFieldDefinition> consumer) {
        var fieldDefinition = resolveIntrospectionField(schema, objectTypeNames, fieldName);
        if (fieldDefinition != null) {
            consumer.accept(fieldDefinition);
            return;
        }

        var fieldVisibility = schema.getCodeRegistry().getFieldVisibility();
        for (String objectTypeName : objectTypeNames) {
            GraphQLObjectType type = (GraphQLObjectType) assertNotNull(schema.getType(objectTypeName));
            // Use field visibility to allow custom visibility implementations to provide placeholder fields
            // for fields that don't exist on the local schema (e.g., in federated subgraphs)
            GraphQLFieldDefinition field = fieldVisibility.getFieldDefinition(type, fieldName);
            consumer.accept(assertNotNull(field, "No field %s found for type %s", fieldName, objectTypeName));
        }
    }

    public List<GraphQLFieldDefinition> getFieldDefinitions(GraphQLSchema schema) {
        List<GraphQLFieldDefinition> result = new ArrayList<>();
        forEachFieldDefinition(schema, result::add);
        return result;
    }

    /**
     * This is NOT public as it is not recommended usage.
     * <p>
     * Internally there are cases where we know it is safe to use this, so this exists.
     */
    private GraphQLFieldDefinition getOneFieldDefinition(GraphQLSchema schema) {
        var fieldDefinition = resolveIntrospectionField(schema, objectTypeNames, fieldName);
        if (fieldDefinition != null) {
            return fieldDefinition;
        }

        String objectTypeName = objectTypeNames.iterator().next();
        GraphQLObjectType type = (GraphQLObjectType) assertNotNull(schema.getType(objectTypeName));
        var fieldVisibility = schema.getCodeRegistry().getFieldVisibility();
        return assertNotNull(fieldVisibility.getFieldDefinition(type, fieldName), "No field %s found for type %s", fieldName, objectTypeName);
    }

    private static GraphQLFieldDefinition resolveIntrospectionField(GraphQLSchema schema, Set<String> objectTypeNames, String fieldName) {
        if (fieldName.equals(schema.getIntrospectionTypenameFieldDefinition().getName())) {
            return schema.getIntrospectionTypenameFieldDefinition();
        } else if (objectTypeNames.size() == 1 && objectTypeNames.iterator().next().equals(schema.getQueryType().getName())) {
            if (fieldName.equals(schema.getIntrospectionSchemaFieldDefinition().getName())) {
                return schema.getIntrospectionSchemaFieldDefinition();
            } else if (fieldName.equals(schema.getIntrospectionTypeFieldDefinition().getName())) {
                return schema.getIntrospectionTypeFieldDefinition();
            }
        }
        return null;
    }

    @Internal
    public void addObjectTypeNames(Collection<String> objectTypeNames) {
        this.objectTypeNames.addAll(objectTypeNames);
    }

    @Internal
    public void setObjectTypeNames(Collection<String> objectTypeNames) {
        this.objectTypeNames.clear();
        this.objectTypeNames.addAll(objectTypeNames);
    }

    @Internal
    public void addChild(NadelExecutableNormalizedField executableNormalizedField) {
        this.children.add(executableNormalizedField);
    }

    @Internal
    public void clearChildren() {
        this.children.clear();
    }

    @Internal
    public void setDeferredExecutions(Collection<NormalizedDeferredExecution> deferredExecutions) {
        this.deferredExecutions.clear();
        this.deferredExecutions.addAll(deferredExecutions);
    }

    public void addDeferredExecutions(Collection<NormalizedDeferredExecution> deferredExecutions) {
        this.deferredExecutions.addAll(deferredExecutions);
    }

    /**
     * All merged fields have the same name so this is the name of the {@link NadelExecutableNormalizedField}.
     * <p>
     * WARNING: This is not always the key in the execution result, because of possible field aliases.
     *
     * @return the name of this {@link NadelExecutableNormalizedField}
     *
     * @see #getResultKey()
     * @see #getAlias()
     */
    public String getName() {
        return getFieldName();
    }

    /**
     * @return the same value as {@link #getName()}
     *
     * @see #getResultKey()
     * @see #getAlias()
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the result key of this {@link NadelExecutableNormalizedField} within the overall result.
     * This is either a field alias or the value of {@link #getName()}
     *
     * @return the result key for this {@link NadelExecutableNormalizedField}.
     *
     * @see #getName()
     */
    public String getResultKey() {
        if (alias != null) {
            return alias;
        }
        return getName();
    }

    /**
     * @return the field alias used or null if there is none
     *
     * @see #getResultKey()
     * @see #getName()
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return a list of the {@link Argument}s on the field
     */
    public List<Argument> getAstArguments() {
        return astArguments;
    }

    /**
     * Returns an argument value as a {@link NormalizedInputValue} which contains its type name and its current value
     *
     * @param name the name of the argument
     *
     * @return an argument value
     */
    public NormalizedInputValue getNormalizedArgument(String name) {
        return normalizedArguments.get(name);
    }

    /**
     * @return a map of all the arguments in {@link NormalizedInputValue} form
     */
    public Map<String, NormalizedInputValue> getNormalizedArguments() {
        return normalizedArguments;
    }

    /**
     * @return a map of the resolved argument values
     */
    public LinkedHashMap<String, Object> getResolvedArguments() {
        return resolvedArguments;
    }


    /**
     * A {@link NadelExecutableNormalizedField} can sometimes (for non-concrete types like interfaces and unions)
     * have more than one object type it could be when executed.  There is no way to know what it will be until
     * the field is executed over data and the type is resolved via a {@link graphql.schema.TypeResolver}.
     * <p>
     * This method returns all the possible types a field can be which is one or more {@link GraphQLObjectType}
     * names.
     * <p>
     * Warning: This returns a Mutable Set. No defensive copy is made for performance reasons.
     *
     * @return a set of the possible type names this field could be.
     */
    public Set<String> getObjectTypeNames() {
        return objectTypeNames;
    }


    /**
     * This returns the first entry in {@link #getObjectTypeNames()}.  Sometimes you know a field cant be more than one
     * type and this method is a shortcut one to help you.
     *
     * @return the first entry from
     */
    public String getSingleObjectTypeName() {
        return objectTypeNames.iterator().next();
    }

    /**
     * @return a helper method show field details
     */
    public String printDetails() {
        StringBuilder result = new StringBuilder();
        if (getAlias() != null) {
            result.append(getAlias()).append(": ");
        }
        return result + objectTypeNamesToString() + "." + fieldName;
    }

    /**
     * @return a helper method to show the object types names as a string
     */
    public String objectTypeNamesToString() {
        if (objectTypeNames.size() == 1) {
            return objectTypeNames.iterator().next();
        } else {
            return objectTypeNames.toString();
        }
    }

    /**
     * This returns the list of the result keys (see {@link #getResultKey()} that lead from this field upwards to
     * its parent field
     *
     * @return a list of the result keys from this {@link NadelExecutableNormalizedField} to the top of the operation via parent fields
     */
    public List<String> getListOfResultKeys() {
        LinkedList<String> list = new LinkedList<>();
        NadelExecutableNormalizedField current = this;
        while (current != null) {
            list.addFirst(current.getResultKey());
            current = current.parent;
        }
        return list;
    }

    /**
     * @return the children of the {@link NadelExecutableNormalizedField}
     */
    public List<NadelExecutableNormalizedField> getChildren() {
        return children;
    }

    /**
     * Returns the list of child fields that would have the same result key
     *
     * @param resultKey the result key to check
     *
     * @return a list of all direct {@link NadelExecutableNormalizedField} children with the specified result key
     */
    public List<NadelExecutableNormalizedField> getChildrenWithSameResultKey(String resultKey) {
        return FpKit.filterList(children, child -> child.getResultKey().equals(resultKey));
    }

    public List<NadelExecutableNormalizedField> getChildren(int includingRelativeLevel) {
        assertTrue(includingRelativeLevel >= 1, "relative level must be >= 1");
        List<NadelExecutableNormalizedField> result = new ArrayList<>();

        this.getChildren().forEach(child -> {
            traverseImpl(child, result::add, 1, includingRelativeLevel);
        });
        return result;
    }

    /**
     * This returns the child fields that can be used if the object is of the specified object type
     *
     * @param objectTypeName the object type
     *
     * @return a list of child fields that would apply to that object type
     */
    public List<NadelExecutableNormalizedField> getChildren(String objectTypeName) {
        return children.stream()
                .filter(cld -> cld.objectTypeNames.contains(objectTypeName))
                .collect(toList());
    }

    /**
     * the level of the {@link NadelExecutableNormalizedField} in the operation hierarchy with top level fields
     * starting at 1
     *
     * @return the level of the {@link NadelExecutableNormalizedField} in the operation hierarchy
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return the parent of this {@link NadelExecutableNormalizedField} or null if it's a top level field
     */
    public NadelExecutableNormalizedField getParent() {
        return parent;
    }

    /**
     * @return the {@link NormalizedDeferredExecution}s associated with this {@link NadelExecutableNormalizedField}.
     *
     * @see NormalizedDeferredExecution
     */
    @ExperimentalApi
    public LinkedHashSet<NormalizedDeferredExecution> getDeferredExecutions() {
        return deferredExecutions;
    }

    @Internal
    public void replaceParent(NadelExecutableNormalizedField newParent) {
        this.parent = newParent;
    }


    @Override
    public String toString() {
        return "NormalizedField{" +
                objectTypeNamesToString() + "." + fieldName +
                ", alias=" + alias +
                ", level=" + level +
                ", children=" + children.stream().map(NadelExecutableNormalizedField::toString).collect(joining("\n")) +
                '}';
    }


    /**
     * Traverse from this {@link NadelExecutableNormalizedField} down into itself and all of its children
     *
     * @param consumer the callback for each {@link NadelExecutableNormalizedField} in the hierarchy.
     */
    public void traverseSubTree(Consumer<NadelExecutableNormalizedField> consumer) {
        this.getChildren().forEach(child -> {
            traverseImpl(child, consumer, 1, Integer.MAX_VALUE);
        });
    }

    private void traverseImpl(NadelExecutableNormalizedField root,
                              Consumer<NadelExecutableNormalizedField> consumer,
                              int curRelativeLevel,
                              int abortAfter) {
        if (curRelativeLevel > abortAfter) {
            return;
        }
        consumer.accept(root);
        root.getChildren().forEach(child -> {
            traverseImpl(child, consumer, curRelativeLevel + 1, abortAfter);
        });
    }

    /**
     * This tries to find interfaces common to all the field output types.
     * <p>
     * i.e. goes through {@link #getFieldDefinitions(GraphQLSchema)} and finds interfaces that
     * all the field's unwrapped output types are assignable to.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<GraphQLInterfaceType> getInterfacesCommonToAllOutputTypes(GraphQLSchema schema) {
        // Shortcut for performance
        if (objectTypeNames.size() == 1) {
            var fieldDef = getOneFieldDefinition(schema);
            var outputType = unwrapAll(fieldDef.getType());

            if (outputType instanceof GraphQLObjectType) {
                return new LinkedHashSet<>((List) ((GraphQLObjectType) outputType).getInterfaces());
            } else if (outputType instanceof GraphQLInterfaceType) {
                var result = new LinkedHashSet<>((List) ((GraphQLInterfaceType) outputType).getInterfaces());
                result.add(outputType);
                return result;
            } else {
                return Collections.emptySet();
            }
        }

        MutableRef<Set<GraphQLInterfaceType>> commonInterfaces = new MutableRef<>();
        forEachFieldDefinition(schema, (fieldDef) -> {
            var outputType = unwrapAll(fieldDef.getType());

            List<GraphQLInterfaceType> outputTypeInterfaces;
            if (outputType instanceof GraphQLObjectType) {
                outputTypeInterfaces = (List) ((GraphQLObjectType) outputType).getInterfaces();
            } else if (outputType instanceof GraphQLInterfaceType) {
                // This interface and superinterfaces
                List<GraphQLNamedOutputType> superInterfaces = ((GraphQLInterfaceType) outputType).getInterfaces();

                outputTypeInterfaces = new ArrayList<>(superInterfaces.size() + 1);
                outputTypeInterfaces.add((GraphQLInterfaceType) outputType);

                if (!superInterfaces.isEmpty()) {
                    outputTypeInterfaces.addAll((List) superInterfaces);
                }
            } else {
                outputTypeInterfaces = Collections.emptyList();
            }

            if (commonInterfaces.value == null) {
                commonInterfaces.value = new LinkedHashSet<>(outputTypeInterfaces);
            } else {
                commonInterfaces.value.retainAll(outputTypeInterfaces);
            }
        });

        return commonInterfaces.value;
    }

    /**
     * @return a {@link Builder} of {@link NadelExecutableNormalizedField}s
     */
    public static Builder newNormalizedField() {
        return new Builder();
    }

    /**
     * Allows this {@link NadelExecutableNormalizedField} to be transformed via a {@link Builder} consumer callback
     *
     * @param builderConsumer the consumer given a builder
     *
     * @return a new transformed {@link NadelExecutableNormalizedField}
     */
    public NadelExecutableNormalizedField transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static class Builder {
        private LinkedHashSet<String> objectTypeNames = new LinkedHashSet<>();
        private String fieldName;
        private ArrayList<NadelExecutableNormalizedField> children = new ArrayList<>();
        private int level;
        private NadelExecutableNormalizedField parent;
        private String alias;
        private Map<String, NormalizedInputValue> normalizedArguments = Collections.emptyMap();
        private LinkedHashMap<String, Object> resolvedArguments = new LinkedHashMap<>();
        private List<Argument> astArguments = Collections.emptyList();
        private boolean forcePrintAsUnconditional;
        private @Nullable ExecutableNormalizedField source;

        private LinkedHashSet<NormalizedDeferredExecution> deferredExecutions = new LinkedHashSet<>();

        private Builder() {
        }

        private Builder(NadelExecutableNormalizedField existing) {
            this.alias = existing.alias;
            this.normalizedArguments = existing.normalizedArguments;
            this.astArguments = existing.astArguments;
            this.resolvedArguments = existing.resolvedArguments;
            this.objectTypeNames = new LinkedHashSet<>(existing.getObjectTypeNames());
            this.fieldName = existing.getFieldName();
            this.children = new ArrayList<>(existing.children);
            this.level = existing.getLevel();
            this.parent = existing.getParent();
            this.deferredExecutions = existing.getDeferredExecutions();
            this.forcePrintAsUnconditional = existing.forcePrintAsUnconditional;
            this.source = existing.source;
        }

        public Builder clearObjectTypesNames() {
            this.objectTypeNames.clear();
            return this;
        }

        public Builder objectTypeNames(List<String> objectTypeNames) {
            this.objectTypeNames.addAll(objectTypeNames);
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder normalizedArguments(@Nullable Map<String, NormalizedInputValue> arguments) {
            this.normalizedArguments = arguments == null ? Collections.emptyMap() : new LinkedHashMap<>(arguments);
            return this;
        }

        public Builder resolvedArguments(@Nullable Map<String, Object> arguments) {
            this.resolvedArguments = arguments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(arguments);
            return this;
        }

        public Builder astArguments(@NonNull List<Argument> astArguments) {
            this.astArguments = new ArrayList<>(astArguments);
            return this;
        }


        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }


        public Builder children(List<NadelExecutableNormalizedField> children) {
            this.children.clear();
            this.children.addAll(children);
            return this;
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder parent(NadelExecutableNormalizedField parent) {
            this.parent = parent;
            return this;
        }

        public Builder deferredExecutions(LinkedHashSet<NormalizedDeferredExecution> deferredExecutions) {
            this.deferredExecutions = deferredExecutions;
            return this;
        }

        public Builder forcePrintAsUnconditional(boolean forcePrintAsUnconditional) {
            this.forcePrintAsUnconditional = forcePrintAsUnconditional;
            return this;
        }

        public Builder source(@Nullable ExecutableNormalizedField source) {
            this.source = source;
            return this;
        }

        public NadelExecutableNormalizedField build() {
            return new NadelExecutableNormalizedField(this);
        }
    }
}
