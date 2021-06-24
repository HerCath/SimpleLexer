?main[]=+aggAddOp;
aggAddOp[]=+aggMultOp (+'+'||'-' +aggMultOp)*;
aggMultOp[]=+aggTermOp (+'*'||'/' +aggTermOp)*;
aggTermOp[]=
    +aggMethod +aggDims '(' +xml '::' (+star|+col) | +aggAddOp ')'
    | '(' +aggAddOp ')';
aggMethod= +'COUNT' | +'SUM' | +'MIN' | +'MAX';
aggDims[]= '[' +col (',' +col)* ']';
xml = +'a'..'z'||'/'||'A'..'Z'+ +'.xml';
star = '*';
col = +'a'..'z'||'_'||'A'..'Z'+;