grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: serviceDefinition+;

serviceDefinition:
'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? typeTransformation? directives? fieldsDefinition? ;

fieldDefinition : description? name argumentsDefinition? ':' type fieldTransformation? directives?;

// fixme: this allows for an empty arrow -- first shot at fixing ( target remote? | remote ) failed
fieldTransformation : '<=' inputMappingDefinition? innerServiceTransformation? ;

typeTransformation : '<=' innerTypeTransformation ;

inputMappingDefinition : '$input.' name ;

innerTypeTransformation: '$innerTypes.' name;

innerServiceTransformation: '$innerQuery.' name remoteCallDefinition?;

remoteCallDefinition : '(' remoteArgumentList ')' ;

remoteArgumentList : remoteArgumentPair ( ',' remoteArgumentPair )* ;

remoteArgumentPair : name remoteArgumentInput? ;

remoteArgumentInput : ':' inputMappingDefinition ;



