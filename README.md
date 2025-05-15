# SimpleLexer
A simple lexer engine to write simple syntax recognition grammar patterns to macth complex expressions. It is fully runtime and does not need any code generation that need to be compiled afterward. The first implementation is 100% java and only in java.

# Main difference with other existing solutions
It does not really rely on tokens even though its output can be considered a token-stream. Its capabilities are more those you can expect from a regex engine. It can match "DDDC" using a grammar like 'D'* 'DC'. Lots of other lexers need more complex expression to hanlde the fact that the 1st token 'D' is a subset of the 2nd token 'DC'. Some lexer can't even hanlde such cases.

3 major syntax/capability differences:
* main : this is the entry point rule, like when programming
* [] : as branch, used so that a rule emits its result as a branch and not a leaf, see below for more details
* ? for debug : when defining a rule, you may precede its name by a '?' so that when this rule is evaluated, it will dump some debug infos into stdout

# Input/Output
The `Lexer` input is a `String`. Its output is a tree-like structure.

# The API
For most user, you will just need to use a very small API (4 classes) :
* tree-like structure, 3 classes:
    * `Node` is the root class with a `name` field.
    * `Leaf` is a `Node` with and additional `value` field
    * `Branch` is a `Node` with and additional `childs` field to store ordered 0 to many `Node`
* `Lexer` : has a single function `parse` which get a `String` and return a `Node` or `null` when it does not match.

# Create a Lexer
You can have a Lexer using 3 different ways :
1. the easy way : using the built-in simple lexer expression language from a String, just use the Utils helper to have it compiled 
2. the hard way : using the built-in implemented Lexer rule blocks, instanciate and connect them
3. the hardest way : create one yourself by hand with code

# built-in lexer expression language
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

The lexer entry point is the main rule. So at least one rule must be defined with that name.

The ruleName may be followed by [], in which case this rule will emit a Branch. It will emit a Leaf otherwise.

The ruleName is case sensitive and must be unique. If the same rule is defined several times, the last definition will be used. This can be used to load different concatenated grammars, using the last one to override some key definition, allowing you to almost achieve some kind of inheritance like in object oriented programming.

Rules may be defined in any order.

Inside ruleExpression: char classes, strings and references to another rule may be preceded by a + to indicate it is captured, otherwise it is discarded. In both cases it must still match.

Nothing is implicit, not even whitespaces.

# Exemples
the grammar `"main[] = +'a' +'A' ;"` matches `"aA"` and capture `<main><char>a</char><char>A</char></main>`

the grammar `"main = +'a' +'A' ;"` matches `"aA"` and capture `<main>aA</main>`

the grammar `"main = 'a' +'A' ;"` matches `"aA"` and capture only `<main>A</main>` because `'a'` does not have the `'+'` capture flag in front of it

the grammar `"?main = 'a' +'A' ;"` does the same as the last exemple but will dump on stdout what it is doing.

To run this last exemple, just do `System.out.println(Utils.toLexer("?main = 'a' +'A' ;").parse("aA"));`

# TODO
add a way to handle left-recursion, better test it
add a way to auto unnest branches with only one child and a way to control the auto unnest feature un the grammar (some kind of << operator, on either rule declaration and/or in rule uses)
