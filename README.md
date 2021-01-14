# SimpleLexer
A simple lexer engine to write simple syntax recognition grammar patterns to macth complex expressions. It is fully runtime and does not need any code generation that need to be compiled afterward. The first implementation is 100% java and only in java.

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
    main[] = WS* (+rule WS*)* ;
    rule[] = +ruleName WS* '=' WS* +ruleOr WS* ';' ;
    ruleName = +LETTER (+LETTER|+DIGIT|+'_')* ;
    LETTER = 'a'..'z'|'A'..'Z' ;
    DIGIT = '0'..'9' ;
    ruleOr[] = +ruleAnd (WS* '|' WS* +ruleAnd)* ;
    ruleAnd[] = +ruleTerm (WS* +ruleTerm)* ;
    ruleTerm[] = +charClass | +string | +ruleRef | '(' WS* +ruleOr WS* ')';

The lexer entry point is the main rule. So at least one rule must be defined with that name.

The ruleName may be followed by [], in which case this rule will emit a Branch. It will emit a Leaf otherwise.

The ruleName is case sensitive and must be uniq.

Rules may be defined in any order.

Inside ruleExpression: char classes, strings and references to another rule may be precedeed by a + to indicate it is captured, otherwise it is discarded. In both cases it must still match.

# Exemples
the grammar `"main[] = +'a' +'A' ;"` matches `"aA"` and capture `<branch name="main"><leaf name="char" value="a"/><<leaf name="char" value="A"/></branch>`

the grammar `"main = +'a' +'A' ;"` matches `"aA"` and capture `<leaf name="main" value="aA"/>`

the grammar `"main = 'a' +'A' ;"` matches `"aA"` and cature `<leaf name="main" value="A"/>`

To run this last exemple, just do `System.out.println(Utils.toGrammar("main = 'a' +'A' ;").parse("aA"));`
