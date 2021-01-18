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
                State states = rule.createState(ctx);
                MatchedContent mc = rule.match(ctx, states);
                return mc != null ?  mc.captured : null;
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
		
    	Rule A = new RuleChar(true, CharClass.fromChar('A'));
    	Rule B = new RuleChar(true, CharClass.fromChar('B'));
    	Rule C = new RuleChar(true, CharClass.fromChar('C'));
    	Rule D = new RuleChar(true, CharClass.fromChar('D')); 
    	Rule A_or_B = new RuleOr(A, B);
    	Rule C_or_D = new RuleOr(C, D);
    	Rule A_and_B = new RuleAnd(A, B);
		Rule A_or_B__or__C_or_D = new RuleOr(
			A_or_B,
			C_or_D
		);
//		Context ctx = new Context("D");
//		States states = D.createStates(ctx);
//		System.out.println(D.match(ctx, states));
//		ctx.pos = 0;
//		System.out.println(D.match(ctx, states));
//		ctx.pos = 0;
//		states = C_or_D.createStates(ctx);
//		while (states.hasNext(ctx)) {
//			System.out.println("After hasNext, state "+states);
//			states.next(ctx);
//			System.out.println("After next, new state is "+states);
////			MatchedContent mc = C_or_D.match(ctx, states);
////			System.out.println("It macthed "+mc);
//		}
//		System.out.println("Last state is "+states);
//		System.out.println(toLexer(D).parse("D"));
		Rule CD = new RuleString(true, "CD");
		Rule C_or_D__and__C_or_D = new RuleAnd(C_or_D, C_or_D);
		Rule CD___or___C_or_D__and__C_or_D = new RuleOr(CD, C_or_D__and__C_or_D);
		Rule tested = A_and_B;
		Context ctx = new Context("AB");
		State state = tested.createState(ctx);
		while (true) {
			ctx.pos = 0;
//			System.out.println("States before match is : "+state);
			MatchedContent mc = tested.match(ctx, state);
			if (mc == null) break;
			System.out.println("!!!! ctx.pos="+ctx.pos+" : "+mc.captured);
		}
		System.out.println("Out of loop, final state is "+state);
//		System.out.println(toLexer().parse("DC"));
		System.exit(0);
//		Context ctx = new Context("D");
//		States states = C_or_D.createStates(ctx);
//		int i = 0;
//		while (states.hasNext(ctx)) {
//			System.out.println("states["+(i++)+"] = "+states);
//			states.next(ctx);
//		}
//		System.out.println("states["+(i++)+"] = "+states);
		System.out.println(toLexer(C_or_D__and__C_or_D).parse("DD"));
//		System.out.println(toLexer(A_or_B__or__C_or_D).parse("D"));

        //System.out.println("START");
        //toLexer("r='A';");
    }
}
