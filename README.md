# SimpleLexer
A simple lexer engine to write simple syntax recognition grammar patterns to macth complex expressions. It is fully runtime and does not need any code generation that need to be compiled afterward. The first implementation is 100% java and only in java.

# Main difference with other existing solutions
It does not really rely on tokens even though its output can be considered a token-stream. Its capabilities are more those you can expect from a regex engine. It can match "DDDC" using a grammar like 'D'* 'DC'. Lots of other lexers need more complex expression to hanlde the fact that the 1st token 'D' is a subset of the 2nd token 'DC'. Some lexer can't even hanlde such cases.

# Input/Output
The `Lexer` input is a `String`. Its output is a tree-like structure.

TODO : make some antlr bridge by wrapping the returned tree into a token stream that can be used by antlr parser

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
    WS = ' '||'\t'||'\n'||\'r';
    LETTER = 'a'..'z'|'A'..'Z' ;
    DIGIT = '0'..'9' ;
    
    main[] = WS* (+rule WS*)* ;
    rule[] = +ruleName WS* '=' WS* +ruleOr WS* ';' ;
    
    ruleName = +LETTER (+LETTER|+DIGIT|+'_')* ;
    
    ruleOr[] = +ruleAnd (WS* '|' WS* +ruleAnd)* ;
    ruleAnd[] = +ruleTerm (WS+ +ruleTerm)* ;
    ruleTerm[] = +'+'? (+charClassOr | +string | +ruleRef) | '(' WS* +ruleOr WS* ')';
    ruleRef = +ruleName ;
    
    charClassOr[] = +charClassAnd ("||" +charClassAnd)* ;
    charClassAnd[] = +charClassNot ("&&" +charClassNot)* ;
    charClassNot[] = +'!'? +charClassTerm ;
    charClassTerm[] = +string | +range | +char | '(' +charClassOr ')' ;
    
    string = '"' ( +!('"'||'\') | '\' +('"'||'\')) '"' ;
    range[] = +char '..' +char;
    char = +<a real char between simple quote, use \ to escape simple quote and itself> ;

The lexer entry point is the main rule. So at least one rule must be defined with that name.

The ruleName may be followed by [], in which case this rule will emit a Branch. It will emit a Leaf otherwise.

The ruleName is case sensitive and must be unique.

Rules may be defined in any order.

Inside ruleExpression: char classes, strings and references to another rule may be preceded by a + to indicate it is captured, otherwise it is discarded. In both cases it must still match.

Nothing is implicit, not even whitespaces.

# Exemples
the grammar `"main[] = +'a' +'A' ;"` matches `"aA"` and capture `<branch name="main"><leaf name="char" value="a"/><leaf name="char" value="A"/></branch>`

the grammar `"main = +'a' +'A' ;"` matches `"aA"` and capture `<leaf name="main" value="aA"/>`

the grammar `"main = 'a' +'A' ;"` matches `"aA"` and capture `<leaf name="main" value="A"/>`

To run this last exemple, just do `System.out.println(Utils.toLexer("main = 'a' +'A' ;").parse("aA"));`
