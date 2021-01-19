package lexer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lexer.impl.*;

public class Utils {

    public static Lexer toLexer(String lexerExpression) {
        Node lexerTree = LEXER_PARSER.parse(lexerExpression);
        throw new RuntimeException("Compiling parsed lexer grammar expression into a lexer using built-in rule engine is not yet implemented.");
    }

    public static Lexer toLexer(Rule rule) {
        return new Lexer() {
            @Override public Node parse(CharSequence input) {
                Context ctx = new Context(input);
                State states = rule.createState(ctx);
                MatchedContent mc = rule.match(ctx, states);
                return mc != null ?  mc.captured : null;
            }
        };
    }
    
    private static final Lexer LEXER_PARSER;
    private static final Map<String,Rule> ruleRefsMap;
    static {
    	CharClass _ws = new CharClass() {
			public String toString() { return "WS"; }
			@Override public boolean contains(char c) { return Character.isWhitespace(c); }
		};
    	CharClass _letter = CharClass.or(CharClass.fromRange('a', 'z'), CharClass.fromRange('A', 'Z'));
    	CharClass _digit = CharClass.fromRange('0', '9');
    	CharClass _underscore = CharClass.fromChar('_');
    	CharClass _letter_or_digit_or_underscode = CharClass.or(_letter, _digit, _underscore);
    	
    	Rule LP = new RuleChar(false, CharClass.fromChar('('));
    	Rule RP = new RuleChar(false, CharClass.fromChar(')'));
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
    	/*final Map<String,Rule>*/ ruleRefsMap = new HashMap<>();
    	Rule ruleRef = new RuleRename("ruleRef", pattern_ruleName);
    	Rule ruleOrRef = new RuleRef(true, "ruleOr", ruleRefsMap);
    	Rule unescapedChar = new RuleOr(
			new RuleChar(true, CharClass.and(
				CharClass.negate(CharClass.fromChar('\'')),
				CharClass.negate(CharClass.fromChar('\\'))
			)),
			new RuleAnd(
				BS,
				new RuleOr(
					new RuleChar(true, CharClass.fromChar('\'')),
					new RuleChar(true, CharClass.fromChar('\\'))
				)
			)
		);
    	Rule ruleChar = new RuleRename("char", new RuleBranchToLeaf(new RuleAnd(
    		SQ,
    		unescapedChar,
    		SQ
    	)));
    	Rule ruleString = new RuleBranchToLeaf(new RuleAnd(
			SQ,
			new RuleCardinality(0, Integer.MAX_VALUE, false, unescapedChar),
    		SQ
    	));
    	Rule ruleRange = new RuleAnd(
    		ruleChar,
    		new RuleString(false, ".."),
    		ruleChar
    	);
    	Rule ruleStringRef = new RuleRef(true, "string", ruleRefsMap);
    	Rule charClassOrRef = new RuleRef(true, "charClassOr", ruleRefsMap);
    	Rule charClassAndRef = new RuleRef(true, "charClassAnd", ruleRefsMap);
    	Rule charClassNotRef = new RuleRef(true, "charClassNot", ruleRefsMap);
    	Rule charClassTermRef = new RuleRef(true, "charClassTerm", ruleRefsMap);
    	Rule charClassTerm = new RuleOr(
    		new RuleRef(true, "string", ruleRefsMap),
    		new RuleRef(true, "range", ruleRefsMap),
    		new RuleRef(true, "char", ruleRefsMap),
    		new RuleAnd(
    			LP,
    			skipSpaces,
    			charClassOrRef,
    			skipSpaces,
    			RP
    		)
    	);
    	Rule charClassNot = new RuleAnd(
    		new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('!'))),
    		skipSpaces,
    		charClassTermRef
    	);
    	Rule charClassAnd = new RuleAnd(
			charClassNotRef,
    		new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
   				skipSpaces,
   				new RuleString(false, "&&"),
   				skipSpaces,
   				charClassNotRef
    		))
    	);
    	Rule charClassOr = new RuleAnd(
			charClassAndRef,
    		new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
   				skipSpaces,
   				new RuleString(false, "||"),
   				skipSpaces,
   				charClassAndRef
    		))
    	);
    	Rule ruleTerm = new RuleOr(
    		new RuleAnd(
				new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('+'))),
				new RuleOr(
		    		charClassOrRef,
		    		ruleStringRef,
		    		new RuleRef(true, "ruleRef", ruleRefsMap)
		    	)
			),
    		new RuleAnd(
    			LP,
    			skipSpaces,
    			ruleOrRef,
    			skipSpaces,
    			RP
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
    	Rule main = new RuleAnd(
			skipSpaces,
			new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleAnd(new RuleRef(true, "rule", ruleRefsMap), skipSpaces))
		);
    	ruleRefsMap.put("rule", new RuleRename("rule", rule));
    	ruleRefsMap.put("ruleRef", new RuleRename("ruleRef", ruleRef));
    	ruleRefsMap.put("ruleOr", new RuleRename("ruleOr", ruleOr));
    	ruleRefsMap.put("ruleAnd", new RuleRename("ruleAnd", ruleAnd));
    	ruleRefsMap.put("ruleTerm", new RuleRename("ruleTerm", ruleTerm));
    	ruleRefsMap.put("charClassOr", new RuleRename("charClassOr", charClassOr));
    	ruleRefsMap.put("charClassAnd", new RuleRename("charClassAnd", charClassAnd));
    	ruleRefsMap.put("charClassNot", new RuleRename("charClassNot", charClassNot));
    	ruleRefsMap.put("charClassTerm", new RuleRename("charClassTerm", charClassTerm));
    	ruleRefsMap.put("string", new RuleRename("string", ruleString));
    	ruleRefsMap.put("char", new RuleRename("char", ruleChar));
    	ruleRefsMap.put("range", new RuleRename("range", ruleRange));
    	ruleRefsMap.put("main", new RuleRename("main", main));
    	LEXER_PARSER = toLexer(main);
    }

    public static void main(String...args) throws Throwable {
    	Rule A = new RuleChar(true, CharClass.fromChar('A'));
    	Rule B = new RuleChar(true, CharClass.fromChar('B'));
    	Rule C = new RuleChar(true, CharClass.fromChar('C'));
    	Rule D = new RuleChar(true, CharClass.fromChar('D')); 
    	Rule A_or_B = new RuleOr(A, B);
    	Rule C_or_D = new RuleOr(C, D);
    	Rule C_or_C = new RuleOr(C, C);
    	Rule C_or_C__and__C_or_C = new RuleAnd(C_or_C, C_or_C);
    	Rule A_and_B = new RuleAnd(A, B);
		Rule A_or_B__or__C_or_D = new RuleOr(A_or_B, C_or_D );
		Rule A_or_B__and__C_or_D = new RuleAnd(A_or_B, C_or_D );
		Rule CD = new RuleString(true, "CD");
		Rule C_or_D__and__C_or_D = new RuleAnd(C_or_D, C_or_D);
		Rule CD___or___C_or_D__and__C_or_D = new RuleOr(CD, C_or_D__and__C_or_D);
		Rule many_D = new RuleCardinality(0, 1, true, D);
		Rule many_D_then_D_then_D = new RuleAnd(many_D, D, D);
		Rule tested = ruleRefsMap.get("main");
		String input = "rule1 = !('A'||'B') | 'AA';";
//		tested.debug = true;
		boolean fullTest = false;
		if (fullTest) {
			Context ctx = new Context(input);
			State state = tested.createState(ctx);
			System.out.println("TESTING: "+tested);
			System.out.println("INITIAL STATE: "+state);
			while (true) {
				ctx.pos = 0;
				MatchedContent mc = tested.match(ctx, state);
				if (mc == null) break;
				System.out.println("\tINTERMEDIATE STATE: "+state);
				System.out.println("\t!!!! ctx.pos="+ctx.pos+" : "+mc.captured);
			}
			System.out.println("FINAL STATE: "+state);
		} else {
			System.out.println(toLexer(tested).parse(input));
		}
		
		//System.out.println(toLexer(ruleRefsMap.get("char")).parse("'A'"));
    }
}
