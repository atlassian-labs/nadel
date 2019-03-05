grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: serviceDefinition+;

serviceDefinition:
'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? typeTransformation? directives? fieldsDefinition? ;

fieldDefinition : description? name argumentsDefinition? ':' type directives? fieldTransformation?;

fieldTransformation : '<=' (fieldMappingDefinition | innerServiceHydration);

typeTransformation : '<=' '$innerTypes' '.' name;

fieldMappingDefinition : '$source' '.' name ;

innerServiceHydration: '$innerQueries' '.' serviceName '.' topLevelField remoteCallDefinition?;

serviceName: NAME;

topLevelField: NAME;

remoteArgumentSource :  sourceObjectReference | fieldArgumentReference | contextArgumentReference  ;

remoteCallDefinition : '(' remoteArgumentPair+ ')' ;

remoteArgumentPair : name ':' remoteArgumentSource ;


sourceObjectReference : '$source' '.' name ;

fieldArgumentReference : '$argument' '.' name ;

contextArgumentReference : '$context' '.' name ;


