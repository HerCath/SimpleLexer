LETTER = 'a'..'z'||'A'..'Z';
DIGIT = '0'..'9';
HEXA = DIGIT||'a'..'f'||'A'..'F';
SQ = '\'';
BS = '\\';
SQ_or_BS = SQ||BS;

integer = +DIGIT+;

main[] = WS* ((+charClass | +rule) WS*)+ ;
charClass[] = +charClassName WS* '=' WS* +charClassOr WS* ';' ;
rule[] = +ruleName +asBranch? WS* '=' WS* +ruleOr WS* ';' ;

anyName = +LETTER +LETTER||DIGIT||'_'* ;
charClassName = +anyName;
ruleName = +anyName;
asBranch = '[]' ;

ruleOr[] = +ruleAnd (WS* '|' WS* +ruleAnd)* ;
ruleAnd[] = +ruleTerm (WS+ +ruleTerm)* ;
ruleTerm[] = (+capture? (+string | +anyName | +charClassOr) | '(' WS* +ruleOr WS* ')' ) +cardinality? ;

capture = '+';

cardinality[] = (+'?'||'+'||'*' | '{' WS* +integer WS* ',' WS* +integer? WS* '}' ) +'?'? ;

charClassOr[] = +charClassAnd (WS* '||' WS* +charClassAnd)* ;
charClassAnd[] = +charClassNot (WS* '&&' WS* +charClassNot)* ;
charClassNot[] = +not? (+range | +char | +charClassName | '(' WS* +charClassOr WS* ')') ;

not = '!';

innerChar = +!SQ_or_BS | +'\\' +SQ_or_BS | +'\\u' +HEXA{4,4};
char = SQ +innerChar SQ;
string = SQ +innerChar* SQ;
range[] = +char '..' +char;