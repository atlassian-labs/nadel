grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL:
   commonDefinition? serviceDefinition+ ;

commonDefinition: 'common' '{' (typeSystemDefinition|typeSystemExtension)* '}';

serviceDefinition:
   SERVICE name '{' (typeSystemDefinition|typeSystemExtension)* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? directives? typeTransformation?  fieldsDefinition? ;

interfaceTypeDefinition : description? INTERFACE name implementsInterfaces? directives? typeTransformation? fieldsDefinition?;

unionTypeDefinition : description? UNION name directives? typeTransformation? unionMembership?;

inputObjectTypeDefinition : description? INPUT name directives? typeTransformation? inputObjectValueDefinitions?;

enumTypeDefinition : description? ENUM name directives? typeTransformation? enumValueDefinitions?;

scalarTypeDefinition : description? SCALAR name directives? typeTransformation?;


fieldDefinition : description? name argumentsDefinition? ':' type directives? addFieldInfo? fieldTransformation?;
addFieldInfo: defaultBatchSize;
defaultBatchSize:'default batch size' intValue;
fieldTransformation : '=>' (fieldMappingDefinition | underlyingServiceHydration);

typeTransformation : '=>' typeMappingDefinition;

//
// renames
//
typeMappingDefinition : 'renamed from' name;

fieldMappingDefinition : 'renamed from' name ('.'name)?;

//
// hydration

underlyingServiceHydration: 'hydrated from' serviceName '.' (syntheticField '.')? topLevelField remoteCallDefinition? objectIdentifier? batchSize?;

objectIdentifier: 'object identified by' name;

batchSize: 'batch size ' intValue;

remoteCallDefinition : '(' remoteArgumentPair+ ')' ;

remoteArgumentPair : name ':' remoteArgumentSource ;

remoteArgumentSource :  sourceObjectReference | fieldArgumentReference | contextArgumentReference;

sourceObjectReference : '$source' '.' name ('.'name)*;

fieldArgumentReference : '$argument' '.' name ;

contextArgumentReference : '$context' '.' name ;

intValue: IntValue;

serviceName: NAME;

topLevelField: NAME | SERVICE;

syntheticField: NAME;

baseName: NAME | FRAGMENT | QUERY | MUTATION | SUBSCRIPTION | SCHEMA | SCALAR | TYPE | INTERFACE | IMPLEMENTS | ENUM | UNION | INPUT | EXTEND | DIRECTIVE | SERVICE;

SERVICE: 'service';
