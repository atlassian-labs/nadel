grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL:
   serviceDefinition+;

serviceDefinition:
   'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? directives? typeTransformation?  fieldsDefinition? ;

interfaceTypeDefinition : description? INTERFACE name directives? typeTransformation? fieldsDefinition?;

unionTypeDefinition : description? UNION name directives? typeTransformation? unionMembership?;

fieldDefinition : description? name argumentsDefinition? ':' type directives? fieldTransformation?;

fieldTransformation : '=>' (fieldMappingDefinition | innerServiceHydration);

typeTransformation : '=>' typeMappingDefinition;

//
// renames
//
typeMappingDefinition : 'renamed from' name;

fieldMappingDefinition : 'renamed from' name;

//
// hydration

innerServiceHydration: 'hydrated from' serviceName '.' topLevelField remoteCallDefinition? objectIdentifier? batchSize?;

objectIdentifier: 'object identified by' name;

batchSize: 'batch size ' intValue;

remoteArgumentSource :  sourceObjectReference | fieldArgumentReference | contextArgumentReference;

remoteCallDefinition : '(' remoteArgumentPair+ ')' ;

remoteArgumentPair : name ':' remoteArgumentSource ;


sourceObjectReference : '$source' '.' name ;

fieldArgumentReference : '$argument' '.' name ;

contextArgumentReference : '$context' '.' name ;

intValue: IntValue;

serviceName: NAME;

topLevelField: NAME;
