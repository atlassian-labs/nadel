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
    private static final String TYPE_NAME_ALIAS_PREFIX_FOR_EMPTY_SELECTION_SETS = "empty_selection_set_";
    public static final String TYPE_NAME_ALIAS_PREFIX_FOR_INTERFACES_AND_UNIONS = "type_hint_";

    public static Field maybeAddUnderscoreTypeName(NadelContext nadelContext, Field field, GraphQLOutputType fieldType) {
        if (Util.isScalar(fieldType)) {
            return field;
        }
        if (Util.isInterfaceOrUnionField(fieldType)) {
            return addUnderscoreTypeName(field, nadelContext, TYPE_NAME_ALIAS_PREFIX_FOR_INTERFACES_AND_UNIONS);
        }
        boolean selectionSetIsEmpty = field.getSelectionSet() == null || field.getSelectionSet().getSelections().isEmpty();
        if (selectionSetIsEmpty) {
            return addUnderscoreTypeName(field, nadelContext, TYPE_NAME_ALIAS_PREFIX_FOR_EMPTY_SELECTION_SETS);
        } else {
            return field;
        }
    }

    private static Field addUnderscoreTypeName(Field field, NadelContext nadelContext, String aliasPrefix) {
        // check if we have already added it
        String typeNameAliasFromContext = getTypeNameAliasFromContext(nadelContext);
        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null) {
            for (Field fld : selectionSet.getSelectionsOfType(Field.class)) {
                if ((TYPE_NAME_ALIAS_PREFIX_FOR_INTERFACES_AND_UNIONS + typeNameAliasFromContext).equals(fld.getAlias()) ||
                        (TYPE_NAME_ALIAS_PREFIX_FOR_EMPTY_SELECTION_SETS + typeNameAliasFromContext).equals(fld.getAlias())) {
                    return field;
                }
            }
        }

        Field underscoreTypeNameAliasField = newField(UNDERSCORE_TYPENAME)
                .alias(aliasPrefix + typeNameAliasFromContext)
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

    private static String getTypeNameAliasFromContext(NadelContext nadelContext) {
        String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        assertNotNull(underscoreTypeNameAlias, () -> "We MUST have a generated __typename alias in the request context");
        return underscoreTypeNameAlias;
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
        String typeNameAlias = nadelContext.getUnderscoreTypeNameAlias();
        return (TYPE_NAME_ALIAS_PREFIX_FOR_INTERFACES_AND_UNIONS + typeNameAlias).equals(alias)
                || (TYPE_NAME_ALIAS_PREFIX_FOR_EMPTY_SELECTION_SETS + typeNameAlias).equals(alias)
                || nadelContext.getObjectIdentifierAlias().equals(alias);
    }

}
