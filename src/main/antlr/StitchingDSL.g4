grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL:
   commonDefinition? serviceDefinition+ ;

commonDefinition: 'common' '{' typeSystemDefinition* '}';

serviceDefinition:
   'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? directives? typeTransformation?  fieldsDefinition? ;

interfaceTypeDefinition : description? INTERFACE name directives? typeTransformation? fieldsDefinition?;

unionTypeDefinition : description? UNION name directives? typeTransformation? unionMembership?;

inputObjectTypeDefinition : description? INPUT name directives? typeTransformation? inputObjectValueDefinitions?;

enumTypeDefinition : description? ENUM name directives? typeTransformation? enumValueDefinitions?;

scalarTypeDefinition : description? SCALAR name directives? typeTransformation?;


fieldDefinition : description? name argumentsDefinition? ':' type directives? fieldTransformation?;

fieldTransformation : '=>' (fieldMappingDefinition | underlyingServiceHydration);

typeTransformation : '=>' typeMappingDefinition;

//
// renames
//
typeMappingDefinition : 'renamed from' name;

fieldMappingDefinition : 'renamed from' name ('.'name)?;

//
// hydration

underlyingServiceHydration: 'hydrated from' serviceName '.' topLevelField hydrationCallDefinition? objectIdentifier? batchSize?;

objectIdentifier: 'object identified by' name;

batchSize: 'batch size ' intValue;


hydrationCallDefinition : '(' argumentPair+ ')' ;

argumentPair : name ':' argumentValue ;

argumentValue :  sourceObjectValue | fieldArgumentValue | contextArgumentValue;


sourceObjectValue : '$source' '.' name ('.'name)*;

fieldArgumentValue : '$argument' '.' name ;

contextArgumentValue : '$context' '.' name ;

intValue: IntValue;

serviceName: NAME;

topLevelField: NAME;
