package graphql.nadel.engine;

import graphql.Internal;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.util.Util;
import graphql.schema.GraphQLOutputType;

import java.util.UUID;

import static graphql.Assert.assertNotNull;
import static graphql.language.Field.newField;

/**
 * Interfaces and unions require that __typename be put on queries so we can work out what type they are on he other side.
 * We also add __typename field when all fields were deleted from a selection set to avoid producing non valid graphql
 * with empty selection sets.
 */
@Internal
public class ArtificialFieldUtils {

    private static final String UNDERSCORE_TYPENAME = Introspection.TypeNameMetaFieldDef.getName();

    public static Field maybeAddUnderscoreTypeName(NadelContext nadelContext, Field field, GraphQLOutputType fieldType) {
        if (Util.isInterfaceOrUnionField(fieldType)) {
            return addUnderscoreTypeName(nadelContext, field);
        }
        if (Util.isScalar(fieldType)) {
            return field;
        }
        boolean selectionSetIsEmpty = field.getSelectionSet() == null
                || field.getSelectionSet().getSelections() == null
                || field.getSelectionSet().getSelections().isEmpty();
        if (selectionSetIsEmpty) {
            return addUnderscoreTypeName(nadelContext, field);
        } else {
            return field;
        }
    }

    private static Field addUnderscoreTypeName(NadelContext nadelContext, Field field) {
        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        assertNotNull(underscoreTypeNameAlias, () -> "We MUST have a generated __typename alias in the request context");

        // check if we have already added it
        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null) {
            for (Field fld : selectionSet.getSelectionsOfType(Field.class)) {
                if (underscoreTypeNameAlias.equals(fld.getAlias())) {
                    return field;
                }
            }
        }

        Field underscoreTypeNameAliasField = newField(UNDERSCORE_TYPENAME)
                .alias(underscoreTypeNameAlias)
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .build();
        if (selectionSet == null) {
            selectionSet = SelectionSet.newSelectionSet().selection(underscoreTypeNameAliasField).build();
        } else {
            selectionSet = selectionSet.transform(builder -> builder.selection(underscoreTypeNameAliasField));
        }
        SelectionSet newSelectionSet = selectionSet;
        field = field.transform(builder -> builder.selectionSet(newSelectionSet));
        return field;
    }

    public static Field addObjectIdentifier(NadelContext nadelContext, Field field, String objectIdentifier) {
        Field idField = newField()
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .alias(nadelContext.getObjectIdentifierAlias())
                .name(objectIdentifier)
                .build();
        SelectionSet selectionSet = field.getSelectionSet().transform(builder -> builder.selection(idField));
        return field.transform(builder -> builder.selectionSet(selectionSet));
    }

    public static boolean isArtificialField(NadelContext nadelContext, String alias) {
        return nadelContext.getUnderscoreTypeNameAlias().equals(alias) || nadelContext.getObjectIdentifierAlias().equals(alias);
    }

}
