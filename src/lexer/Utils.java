package lexer;

import java.util.HashMap;
import java.util.Iterator;
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
                Iterator<Object> states = rule.getStates(ctx);
                while (states.hasNext()) {
                    MatchedContent mc = rule.match(ctx, states);
                    if (mc != null) return mc.captured;
				}
				return null;
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
    	Rule ruleStringRef = new RuleRef(true, "string", ruleRefsMap);
    	Rule charClassOrRef = new RuleRef(true, "charClassOr", ruleRefsMap);
    	Rule charClassTerm = new RuleRef(true, "char", ruleRefsMap); // TODO : handle charClassRange and '(' + charClassOr + ')' cases
    	Rule charClassNot = charClassTerm; // TODO
    	Rule charClassAnd = charClassNot; // TODO
    	Rule charClassOr = charClassAnd; // TODO
    	Rule capturable = new RuleOr(
    		charClassOr,
    		ruleStringRef,
    		new RuleRef(true, "ruleRef", ruleRefsMap)
    	);
    	Rule capturableRef = new RuleRef(true, "capturable", ruleRefsMap);
    	Rule ruleTerm = new RuleOr(
    		new RuleAnd(
				new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('+'))),
				capturableRef
			),
    		new RuleAnd(
    			new RuleChar(false, CharClass.fromChar('(')),
    			skipSpaces,
    			ruleOrRef,
    			skipSpaces,
    			new RuleChar(false, CharClass.fromChar(')'))
    		)
    	);
    	Rule ruleTermRef = new RuleRef(true, "ruleTerm", ruleRefsMap);
    	Rule ruleAnd = new RuleAnd(
			ruleTermRef,
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
				skipSpaces1,
				ruleTermRef
			))
		);
    	Rule ruleAndRef = new RuleRef(true, "ruleAnd", ruleRefsMap);
    	Rule ruleOr = new RuleAnd(
    		ruleAndRef,
    		new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
   				skipSpaces,
   				PIPE,
   				skipSpaces,
   	    		ruleAndRef
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
    	ruleRefsMap.put("string", ruleString);
    	ruleRefsMap.put("char", ruleChar);
    	Rule main = new RuleAnd(
    		skipSpaces,
    		new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleAnd(rule, skipSpaces))
    	);
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
		
		Rule r = new RuleOr(
			new RuleOr(
				new RuleChar(true, CharClass.fromChar('A')),
				new RuleChar(true, CharClass.fromChar('B'))
			),
			new RuleOr(
				new RuleChar(true, CharClass.fromChar('C')),
				new RuleChar(true, CharClass.fromChar('D'))
			)
		);
		System.out.println(toLexer(r).parse("D"));

        //System.out.println("START");
        //toLexer("r='A';");
    }
}
