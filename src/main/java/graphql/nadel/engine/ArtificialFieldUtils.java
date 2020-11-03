package graphql.nadel.engine;

import graphql.Internal;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.transformation.FieldUtils;
import graphql.nadel.util.Util;
import graphql.schema.GraphQLOutputType;

import static graphql.Assert.assertNotNull;
import static graphql.language.Field.newField;

/**
 * Interfaces and unions require that __typename be put on queries so we can work out what type they are on he other side
 */
@Internal
public class ArtificialFieldUtils {

    private static final String UNDERSCORE_TYPENAME = Introspection.TypeNameMetaFieldDef.getName();

    public static Field maybeAddUnderscoreTypeName(NadelContext nadelContext, Field field, GraphQLOutputType fieldType) {
        if (!Util.isInterfaceOrUnionField(fieldType)) {
            return field;
        }
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
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
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
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
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
