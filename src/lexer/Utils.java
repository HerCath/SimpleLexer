package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lexer.impl.*;

public class Utils {

    public static Lexer toLexer(String lexerExpression) {
        Branch rules = (Branch) LEXER_PARSER.parse(lexerExpression);
        Map<String,Rule> ruleRefsMap = new HashMap<>();
        for (Node rule:rules.childs) {
        	Branch _rule = (Branch) rule;
        	String ruleName = _rule.childs.get(0).stringValue();
        	boolean asLeaf;
        	Node ruleNode;
        	if (_rule.childs.size()==3) {
        		asLeaf = false;
        		ruleNode = _rule.childs.get(2);
        	} else {
        		asLeaf = true;
        		ruleNode = _rule.childs.get(1);
        	}
        	Rule ruleImpl = compile(ruleRefsMap, ruleNode);
        	if (asLeaf)
        		ruleImpl = new RuleBranchToLeaf(ruleImpl);
        	ruleRefsMap.put(ruleName, new RuleRename(ruleName, ruleImpl));
        }
        Rule main = ruleRefsMap.get("main");
        if (main==null)
        	throw new RuntimeException("The main rule has not been defined.");
        return toLexer(main);
    }
    
	// TODO : better perfs can be achieved if using ranges and just doing range
	// intersection/complementary, with maybe just a special case for final char
	// classes with only 1 possible value
    private static CharClass compile(Node charClassNode) {
    	if (charClassNode.name.equals("range")) {
    		Branch b = (Branch) charClassNode;
    		Leaf from = (Leaf) b.childs.get(0);
    		Leaf to = (Leaf) b.childs.get(1);
    		return CharClass.fromRange(
    			from.value.toString().charAt(0),
    			to.value.toString().charAt(0)
    		);
    	}
    	if (charClassNode.name.equals("char")) {
    		Leaf l = (Leaf) charClassNode;
    		return CharClass.fromChar(l.value.toString().charAt(0));
    	}
    	if (charClassNode.name.equals("charClassNot")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0));
    		return CharClass.negate(compile(b.childs.get(1)));
    	}
    	if (charClassNode.name.equals("charClassAnd")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0));
    		CharClass[] charClasses = new CharClass[b.childs.size()];
    		for (int i=0; i<b.childs.size(); i++) {
    			charClasses[i] = compile(b.childs.get(i));
    		}
    		return CharClass.and(charClasses);
    	}
    	if (charClassNode.name.equals("charClassOr")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0));
    		CharClass[] charClasses = new CharClass[b.childs.size()];
    		for (int i=0; i<b.childs.size(); i++) {
    			charClasses[i] = compile(b.childs.get(i));
    		}
    		return CharClass.or(charClasses);
    	}
    	throw new RuntimeException("Do not know how to compile into a char class node "+charClassNode);
    }
    
    private static Rule compile(Map<String,Rule> ruleRefsMap, Node ruleNode) {
    	if (ruleNode.name.equals("ruleTerm")) {
    		Branch b = (Branch) ruleNode;
    		Node c = b.childs.get(0);
    		if (c.name.equals("ruleOr")) {
    			if (b.childs.size()!=1) throw new RuntimeException("Bad ruleTerm node, it has more than 1 child while the 1st one is a ruleOr. node is: "+ruleNode);
    			return compile(ruleRefsMap, c);
    		} else {
    			// this is a capturable content, next node is either a string, a range, a char or a ref to another rule
    			boolean capture = false;;
    			if (b.childs.size()==2) {
    				capture = true;
    				c = b.childs.get(1);
    			}
    			if (c.name.equals("string")) {
    				Leaf l = (Leaf) c;
    				return new RuleString(capture, (String)l.value);
    			} else if (c.name.equals("charClassOr")) {
    				return new RuleChar(capture, compile(c));
    			} else if (c.name.equals("ruleName")) {
    				Leaf l = (Leaf) c;
    				return new RuleRef(capture, (String)l.value, ruleRefsMap);
    			}
    		}
    	}
    	if (ruleNode.name.equals("ruleAnd")) {
    		Branch b = (Branch) ruleNode;
    		if (b.childs.size()==1) return compile(ruleRefsMap, b.childs.get(0));
    		List<Rule> subRules = new ArrayList<>(b.childs.size());
    		for (Node child:b.childs) {
    			subRules.add(compile(ruleRefsMap, child));
    		}
    		return new RuleAnd(subRules);
    	}
    	if (ruleNode.name.equals("ruleOr")) {
    		Branch b = (Branch) ruleNode;
    		if (b.childs.size()==1) return compile(ruleRefsMap, b.childs.get(0));
    		List<Rule> subRules = new ArrayList<>(b.childs.size());
    		for (Node child:b.childs) {
    			subRules.add(compile(ruleRefsMap, child));
    		}
    		return new RuleOr(subRules);
    	}
    	throw new RuntimeException("Do not know how to compile node named "+ruleNode.name+". Full node is "+ruleNode);
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
    	Rule LB = new RuleChar(false, CharClass.fromChar('['));
    	Rule RB = new RuleChar(false, CharClass.fromChar(']'));
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
//    	Rule ruleRef = new RuleRename("ruleRef", pattern_ruleName);
    	Rule ruleOrRef = new RuleRef(true, "ruleOr", ruleRefsMap);
    	Rule ruleStringRef = new RuleRef(true, "string", ruleRefsMap);
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
    	Rule ruleString = new RuleBranchToLeaf(new RuleAnd(
			SQ,
			new RuleCardinality(0, Integer.MAX_VALUE, false, unescapedChar),
    		SQ
    	));
    	Rule charClassChar = new RuleRename("char", new RuleBranchToLeaf(new RuleAnd(
			SQ,
			unescapedChar,
			SQ
		)));
    	Rule charClassRange = new RuleAnd(
    		charClassChar,
    		new RuleString(false, ".."),
    		charClassChar
    	);
    	Rule charClassOrRef = new RuleRef(true, "charClassOr", ruleRefsMap);
    	Rule charClassAndRef = new RuleRef(true, "charClassAnd", ruleRefsMap);
    	Rule charClassNotRef = new RuleRef(true, "charClassNot", ruleRefsMap);
    	Rule charClassNot = new RuleAnd(
    		new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('!'))),
    		skipSpaces,
    		new RuleOr(
				new RuleRef(true, "range", ruleRefsMap),
				new RuleRef(true, "char", ruleRefsMap),
				new RuleAnd(
					LP,
					skipSpaces,
					charClassOrRef,
					skipSpaces,
					RP
				)
    		)
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
					ruleStringRef,
		    		charClassOrRef,
		    		new RuleRef(true, "ruleName", ruleRefsMap)
		    	)
			),
    		new RuleAnd(
    			LP,
    			skipSpaces,
    			ruleOrRef,
    			skipSpaces,
    			RP
    		)
    	);// TODO : adds cardinality parsing
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
    	Rule asBranch = new RuleAnd( LB, skipSpaces, RB );
    	Rule rule = new RuleAnd(
    		ruleName,
    		skipSpaces,
    		new RuleCardinality(0, 1, false, new RuleRef(true, "asBranch", ruleRefsMap)),
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
    	ruleRefsMap.put("ruleName", new RuleRename("ruleName", ruleName));
    	ruleRefsMap.put("ruleOr", new RuleRename("ruleOr", ruleOr));
    	ruleRefsMap.put("ruleAnd", new RuleRename("ruleAnd", ruleAnd));
    	ruleRefsMap.put("ruleTerm", new RuleRename("ruleTerm", ruleTerm));
    	ruleRefsMap.put("asBranch", new RuleRename("asBranch", asBranch));
    	ruleRefsMap.put("charClassOr", new RuleRename("charClassOr", charClassOr));
    	ruleRefsMap.put("charClassAnd", new RuleRename("charClassAnd", charClassAnd));
    	ruleRefsMap.put("charClassNot", new RuleRename("charClassNot", charClassNot));
    	ruleRefsMap.put("string", new RuleRename("string", ruleString));
    	ruleRefsMap.put("char", new RuleRename("char", charClassChar));
    	ruleRefsMap.put("range", new RuleRename("range", charClassRange));
    	ruleRefsMap.put("main", new RuleRename("main", main));
    	LEXER_PARSER = toLexer(main);
    }

    public static void main(String...args) throws Throwable {
    	String grammar = "main[] = +toto toto; toto = +'a'||'A';";
    	System.out.println(grammar);
    	System.out.println("==================================");
    	Node asNode = LEXER_PARSER.parse(grammar);
    	System.out.println(asNode);
    	System.out.println("==================================");
    	System.out.println(toLexer(grammar).parse("aA"));
    	
    	System.exit(0);
    	
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
		String input = "main = +'A' | +'AA' | +'A'..'A' | +anotherRule | ( +'C' );";
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
