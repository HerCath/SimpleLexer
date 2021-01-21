CR = '\u000D';
LF = '\u000A';
SEP = ',';

main[] = +row*;
row[] = +field (SEP +field)* CR? LF;
field = +quotedField | +unquotedField | +emptyField;

quotedField = +'"' (+!'"' | +'""')* +'"';
unquotedField = +!('"'||SEP||CR||LF) +!(SEP||CR||LF)*;
emptyField = '';