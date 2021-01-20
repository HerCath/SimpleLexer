LETTER = +'a'..'z'||'A'..'Z';
DIGIT = +'0'..'9';
integer = +DIGIT+;

main[] = WS* (+rule WS*)+ ;
rule[] = +ruleName +asBranch? WS* '=' WS* +ruleOr WS* ';' ;

ruleName = +LETTER +'a'..'z'||'A'..'Z'||'0'..'9'||'_'* ;
asBranch = '[]' ;

ruleOr[] = +ruleAnd (WS* '|' WS* +ruleAnd)* ;
ruleAnd[] = +ruleTerm (WS+ +ruleTerm)* ;
ruleTerm[] = (+capture? (+string | +charClassOr | +ruleName) | '(' WS* +ruleOr WS* ')' ) +cardinality? ;

capture = '+';

cardinality[] = (+'?'||'+'||'*' | '{' WS* +integer WS* ',' WS* +integer? WS* '}' ) +'?'? ;

charClassOr[] = +charClassAnd (WS* '||' WS* +charClassAnd)* ;
charClassAnd[] = +charClassNot (WS* '&&' WS* +charClassNot)* ;
charClassNot[] = +not? (+range | +char | '(' WS* +charClassOr WS* ')') ;

not = '!';

string = '\'' (+!('\''||'\\') | '\\' +'\''||'\\')+ '\'' ;
range[] = +char '..' +char ;
char = '\'' (+!('\''||'\\') | '\\' +'\''||'\\') '\'';