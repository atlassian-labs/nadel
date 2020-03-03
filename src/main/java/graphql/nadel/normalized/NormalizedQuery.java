package graphql.nadel.normalized;

import graphql.Internal;
import graphql.language.AstPrinter;
import graphql.language.Field;
import graphql.language.SelectionSet;

import java.util.List;
import java.util.Map;

import static graphql.util.FpKit.flatList;
import static graphql.util.FpKit.map;

@Internal
public class NormalizedQuery {

    private final List<NormalizedQueryField> rootFields;
    private final Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId;

    public NormalizedQuery(List<NormalizedQueryField> rootFields, Map<String, List<NormalizedQueryField>> normalizedFieldsByFieldId) {
        this.rootFields = rootFields;
        this.normalizedFieldsByFieldId = normalizedFieldsByFieldId;
    }

    public List<NormalizedQueryField> getRootFields() {
        return rootFields;
    }

    public Map<String, List<NormalizedQueryField>> getNormalizedFieldsByFieldId() {
        return normalizedFieldsByFieldId;
    }

    public List<NormalizedQueryField> getNormalizedFieldsByFieldId(String astFieldId) {
        return normalizedFieldsByFieldId.get(astFieldId);
    }

    public String printOriginalQuery() {
        List<Field> rootAstFields = flatList(map(rootFields, rootField -> rootField.getMergedField().getFields()));
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selections(rootAstFields).build();
        return AstPrinter.printAst(selectionSet);
    }
}
