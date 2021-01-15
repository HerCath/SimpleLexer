package lexer;

import lexer.impl.*;

public class Utils {

    public static Lexer toLexer(String lexerExpression) {
        Node lexerTree = LEXER_PARSER.parse(lexerExpression);
        System.out.println("Parsed \""+lexerExpression+"\" as "+lexerTree);
        // TODO : compile the returned tree into a Lexer using the Rule framework
        return null;
    }

    public static Lexer toLexer(Rule rule) {
        return new Lexer() {
            @Override public Node parse(CharSequence input) {
                Context ctx = new Context(input);
                Object state = rule.createInitialState(ctx);
                while (true) {
                    MatchedContent mc = rule.tryToMatch(ctx, state);
                    if (mc != null) return mc.captured;
                    if (!rule.nextState(ctx, state)) return null;
                }
            }
        };
    }
    
    private static final Lexer LEXER_PARSER;
    static {
    	CharClass _ws = Character::isWhitespace;
    	CharClass _letter = CharClass.or(CharClass.fromRange('a', 'z'), CharClass.fromRange('A', 'Z'));
    	CharClass _digit = CharClass.fromRange('0', '9');
    	CharClass _underscore = CharClass.fromChar('_');
    	CharClass _letter_or_digit_or_underscode = CharClass.or(_letter, _digit, _underscore);
    	CharClass _eq = CharClass.fromChar('=');
    	CharClass _semiColomn = CharClass.fromChar(';');
    	
    	Rule EQ = new RuleChar(false, _eq);
    	Rule SM = new RuleChar(false, _semiColomn);
    	Rule skipSpaces = new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleChar(false, _ws){public String toString(){return "WS";}} );
    	Rule ruleName = new RuleRename("ruleName", new RuleBranchToLeaf(new RuleAnd(
			new RuleChar(true, _letter),
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleChar(true, _letter_or_digit_or_underscode))
    	)));
    	Rule rule = new RuleAnd(
    		ruleName,
    		skipSpaces,
    		EQ,
    		skipSpaces,
    		// TODO : rule expression
    		skipSpaces,
    		SM
    	);
    	Rule main = new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleAnd(skipSpaces, rule, skipSpaces));
    	System.out.println("lexer main rule minimal size is "+main.minSize());
    	LEXER_PARSER = toLexer(main);
//    	System.exit(0);
    }

    public static void main(String...args) throws Throwable {
    	/*
        Rule lower = new RuleChar(true, CharClass.fromRange('a', 'z'));
        Rule upper = new RuleChar(false, CharClass.fromRange('A', 'Z'));
        Rule letter = new RuleOr(lower, upper);
        Rule letter_1_to_3 = new RuleCardinality(0, Integer.MAX_VALUE, true, letter);
        Rule digit = new RuleChar(true, CharClass.fromRange('0', '9'));
        Rule letter_1_to_3_then_digit = new RuleAnd(letter_1_to_3, digit);
        Lexer grammar = toLexer(new RuleBranchToLeaf(letter_1_to_3_then_digit));
        System.out.println(grammar.parse("abCDef3"));
        System.out.println(grammar.parse("AA"));
        System.out.println(grammar.parse("a3"));
        */
    	

        Rule upper = new RuleChar(true, CharClass.fromRange('A', 'Z'));
        Rule uppers = new RuleCardinality(0, Integer.MAX_VALUE, false, upper);
        Rule AB = new RuleString(true, "AB");
        Rule main = new RuleAnd(uppers, AB);
        System.out.println(toLexer(main).parse("AAAB"));
        
//        System.out.println("START");
//        toLexer("    a=;    b                   = ;c=;                         d                        =   ;                 ");
    }
}
