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
        ruleRefsMap.put("WS", new RuleChar(false, Character::isWhitespace));
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
        	System.out.println(ruleName+(asLeaf?"":"[]")+" = "+ruleImpl+";");
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
    		Rule rule;
    		if (c.name.equals("ruleOr")) {
    			if (b.childs.size()!=1) throw new RuntimeException("Bad ruleTerm node, it has more than 1 child while the 1st one is a ruleOr. node is: "+ruleNode);
    			rule = compile(ruleRefsMap, c);
    		} else {
    			// this is a capturable content, next node is either a string, a range, a char or a ref to another rule
    			boolean capture = false;;
    			if (c.name.equals("char")) {
    				capture = true;
    				c = b.childs.get(1);
    			}
    			if (c.name.equals("string")) {
    				Leaf l = (Leaf) c;
    				rule = new RuleString(capture, (String)l.value);
    			} else if (c.name.equals("charClassOr")) {
    				rule = new RuleChar(capture, compile(c));
    			} else if (c.name.equals("ruleName")) {
    				Leaf l = (Leaf) c;
    				rule = new RuleRef(capture, (String)l.value, ruleRefsMap);
    			} else {
    				throw new RuntimeException("Could not compiled node "+ruleNode);
    			}
    		}
    		c = b.childs.get(b.childs.size()-1);
			if (c.name.equals("cardinality")) {
				Branch cardinality = (Branch) c;
				c = cardinality.childs.get(0);
				int min, max;
				boolean greedy;
				if (c.name.equals("char")) {
					// this is a cardinality expressed using one of the 3 well-known macros ?, + or *
					String wellKnownCardSymbol = c.stringValue();
					if ("?".equals(wellKnownCardSymbol)) {
						min = 0;
						max = 1;
					} else if ("+".equals(wellKnownCardSymbol)) {
						min = 1;
						max = Integer.MAX_VALUE;
					} else if ("*".equals(wellKnownCardSymbol)) {
						min = 0;
						max = Integer.MAX_VALUE;
					} else {
						throw new RuntimeException("Got a cardinality node with a strange content: "+cardinality);
					}
					if (cardinality.childs.size()==1) {
						greedy = false;
					} else if (cardinality.childs.size()==2) {
						c = cardinality.childs.get(1);
						System.out.println("Going to deduce greedy using "+c);
						if (c.name.equals("char") && c.stringValue().equals("?")) {
							greedy = true;
						} else {
    						throw new RuntimeException("Got a cardinality node with a strange content: "+cardinality);
						}
					} else {
						throw new RuntimeException("Got a cardinality node with a strange content: "+cardinality);
					}
				} else if (c.name.equals("integer")) {
					min = Integer.parseInt(c.stringValue());
					if (cardinality.childs.size()>1) {
						c = cardinality.childs.get(1);
						if (c.name.equals("integer")) {
							max = Integer.parseInt(c.stringValue());
							if (cardinality.childs.size()>2) {
								c = cardinality.childs.get(2);
							}
						} else {
							max = Integer.MAX_VALUE;
						}
						greedy = c.name.equals("char");
					} else {
						max = Integer.MAX_VALUE;
						greedy = false;
					}
				} else {
					throw new RuntimeException("Got a cardinality node with a strange content: "+cardinality);
				}
				rule = new RuleCardinality(min, max, greedy, rule);
			}
			return rule;
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
                while (true) {
	                MatchedContent mc = rule.match(ctx, states);
	                if (mc != null) {
	                	if (ctx.atEnd())
	                		return mc.captured;
	                	ctx.pos = 0;
	                } else {
	                	return null;
	                }
                }
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
    	Rule ruleTerm = new RuleAnd( 
	    	new RuleOr(
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
	    	),
	    	new RuleCardinality(0, 1, false, new RuleRef(true, "cardinality", ruleRefsMap))
    	);
    	Rule integer = new RuleBranchToLeaf(new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleRef(true, "DIGIT", ruleRefsMap)));
    	Rule cardinality = new RuleAnd(
    		new RuleOr(
				new RuleChar(true, CharClass.fromChar('?')),
				new RuleChar(true, CharClass.fromChar('*')),
				new RuleChar(true, CharClass.fromChar('+')),
				new RuleAnd(
					new RuleChar(false, CharClass.fromChar('{')),
					skipSpaces,
					new RuleRef(true, "integer", ruleRefsMap),
					skipSpaces,
					new RuleChar(false, CharClass.fromChar(',')),
					skipSpaces,
					new RuleCardinality(0, 1, false, new RuleRef(true, "integer", ruleRefsMap)),
					skipSpaces,
					new RuleChar(false, CharClass.fromChar('}'))
				)
    		),
    		new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('?')))
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
    	Rule asBranch = new RuleString(true, "[]");
    	Rule rule = new RuleAnd(
    		ruleName,
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
    	ruleRefsMap.put("WS", new RuleRename("WS", new RuleChar(false, _ws)));
    	ruleRefsMap.put("LETTER", new RuleRename("LETTER", new RuleChar(true, _letter)));
    	ruleRefsMap.put("DIGIT", new RuleRename("DIGIT", new RuleChar(true, _digit)));
    	ruleRefsMap.put("integer", new RuleRename("integer", integer));
    	ruleRefsMap.put("main", new RuleRename("main", main));
    	ruleRefsMap.put("rule", new RuleRename("rule", rule));
    	ruleRefsMap.put("ruleName", new RuleRename("ruleName", ruleName));
    	ruleRefsMap.put("asBranch", new RuleRename("asBranch", asBranch));
    	ruleRefsMap.put("ruleOr", new RuleRename("ruleOr", ruleOr));
    	ruleRefsMap.put("ruleAnd", new RuleRename("ruleAnd", ruleAnd));
    	ruleRefsMap.put("ruleTerm", new RuleRename("ruleTerm", ruleTerm));
    	ruleRefsMap.put("cardinality", new RuleRename("cardinality", cardinality));
    	ruleRefsMap.put("charClassOr", new RuleRename("charClassOr", charClassOr));
    	ruleRefsMap.put("charClassAnd", new RuleRename("charClassAnd", charClassAnd));
    	ruleRefsMap.put("charClassNot", new RuleRename("charClassNot", charClassNot));
    	ruleRefsMap.put("string", new RuleRename("string", ruleString));
    	ruleRefsMap.put("range", new RuleRename("range", charClassRange));
    	ruleRefsMap.put("char", new RuleRename("char", charClassChar));
    	LEXER_PARSER = toLexer(main);
    }

    public static void main(String...args) throws Throwable {
    	String grammar = "main[] = +toto{0,10}?; toto = +'a';"; // TODO : even if toto does not capture, it can be captured
    	System.out.println(grammar);
    	System.out.println("==================================");
    	Node asNode = LEXER_PARSER.parse(grammar);
    	System.out.println(asNode);
    	System.out.println("==================================");
    	System.out.println(toLexer(grammar).parse("aa"));
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
		Rule many_D_not_greedy = new RuleCardinality(0, Integer.MAX_VALUE, false, D);
		Rule many_D_greedy = new RuleCardinality(0, Integer.MAX_VALUE, true, D);
		Rule many_D_then_D_then_D = new RuleAnd(many_D_greedy, C);
		Rule tested = many_D_then_D_then_D;
		String input = "DDDC";
//		tested.debug = true;
		boolean fullTest = true;
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
