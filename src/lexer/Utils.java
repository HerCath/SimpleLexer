package lexer;

import java.util.HashMap;
import java.util.Map;

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
    	CharClass _ws = new CharClass() {
			public String toString() { return "WS"; }
			@Override public boolean contains(char c) { return Character.isWhitespace(c); }
		};
    	CharClass _letter = CharClass.or(CharClass.fromRange('a', 'z'), CharClass.fromRange('A', 'Z'));
    	CharClass _digit = CharClass.fromRange('0', '9');
    	CharClass _underscore = CharClass.fromChar('_');
    	CharClass _letter_or_digit_or_underscode = CharClass.or(_letter, _digit, _underscore);
    	
    	Rule EQ = new RuleChar(false, CharClass.fromChar('='));
    	Rule SM = new RuleChar(false, CharClass.fromChar(';'));
    	Rule PIPE = new RuleChar(false, CharClass.fromChar('|'));
    	Rule WS = new RuleChar(false, _ws);
    	Rule BS = new RuleChar(false, CharClass.fromChar('\\'));
    	Rule notBS = new RuleChar(true, CharClass.fromChar('\\'));
    	Rule SQ = new RuleChar(false, CharClass.fromChar('\''));
    	Rule skipSpaces = new RuleCardinality(0, Integer.MAX_VALUE, false, WS );
    	Rule skipSpaces1 = new RuleCardinality(1, Integer.MAX_VALUE, false, WS );
    	Rule pattern_ruleName = new RuleBranchToLeaf(new RuleAnd(
			new RuleChar(true, _letter),
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleChar(true, _letter_or_digit_or_underscode))
    	));
    	Rule ruleName = new RuleRename("ruleName", pattern_ruleName);
    	final Map<String,Rule> ruleRefsMap = new HashMap<>();
    	Rule ruleRef = new RuleRename("ruleRef", pattern_ruleName);
    	Rule ruleOrRef = new RuleRef(true, "ruleOr", ruleRefsMap);
    	Rule ruleChar = new RuleAnd(
    		SQ,
    		new RuleOr(
    			new RuleChar(true, CharClass.and(CharClass.fromChar('\''), CharClass.fromChar('\\'))),
    			new RuleAnd(
    				BS,
    				new RuleOr(
   						new RuleChar(true, CharClass.fromChar('\'')),
   						new RuleChar(true, CharClass.fromChar('\\'))
    				)
    			)
    		),
    		SQ
    	);
    	Rule ruleString = new RuleBranchToLeaf(new RuleAnd(
			new RuleChar(false, CharClass.fromChar('"')),
    		new RuleOr(
    			new RuleChar(true, CharClass.and(CharClass.fromChar('"'), CharClass.fromChar('\\'))),
    			new RuleAnd(
    				BS,
    				new RuleOr(
   						new RuleChar(true, CharClass.fromChar('"')),
   						new RuleChar(true, CharClass.fromChar('\\'))
    				)
    			)
    		),
    		new RuleChar(false, CharClass.fromChar('"'))
    	));
    	Rule charClassOrRef = new RuleRef(true, "charClassOr", ruleRefsMap);
    	Rule charClassTerm = ruleChar; // TODO : handle charClassRange and '(' + charClassOr + ')' cases
    	Rule charClassNot = charClassTerm; // TODO
    	Rule charClassAnd = charClassNot; // TODO
    	Rule charClassOr = charClassAnd; // TODO
    	Rule capturable = new RuleOr(
    		charClassOr,
    		ruleString,
    		ruleRef
    	);
    	Rule ruleTerm = new RuleOr(
    		new RuleAnd(
				new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('+'))),
				capturable
			),
    		new RuleAnd(
    			new RuleChar(false, CharClass.fromChar('(')),
    			skipSpaces,
    			ruleOrRef,
    			skipSpaces,
    			new RuleChar(false, CharClass.fromChar(')'))
    		)
    	);
    	Rule ruleAnd = new RuleAnd(
			ruleTerm,
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
				skipSpaces1,
				ruleTerm
			))
		);
    	Rule ruleOr = new RuleAnd(
    		ruleAnd,
    		new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
   				skipSpaces,
   				PIPE,
   				skipSpaces,
   	    		ruleAnd
    		))
    	);
    	Rule rule = new RuleAnd(
    		ruleName,
    		skipSpaces,
    		EQ,
    		skipSpaces,
    		ruleOrRef,
    		skipSpaces,
    		SM
    	);
    	ruleRefsMap.put("ruleRef", ruleRef);
    	ruleRefsMap.put("ruleOr", ruleOr);
    	ruleRefsMap.put("ruleAnd", ruleAnd);
    	ruleRefsMap.put("ruleTerm", ruleTerm);
    	ruleRefsMap.put("capturable", capturable);
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
    	

//        Rule upper = new RuleChar(true, CharClass.fromRange('A', 'Z'));
//        Rule uppers = new RuleCardinality(0, Integer.MAX_VALUE, false, upper);
//        Rule AB = new RuleString(true, "AB");
//        Rule main = new RuleAnd(uppers, AB);
//        System.out.println(toLexer(main).parse("AAAB"));
        
        System.out.println("START");
        toLexer("r='A';");
    }
}
