package lexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lexer.impl.*;

public class Utils {

	private static Lexer toLexer(Node lexerNode) {
		Branch rules = (Branch) lexerNode;
		// we need to go through the rules 2 times
		// 1st round : to get all the charClass
		// 2nd round : to get the rules
		// we do this because rules may refers either charClass or othre rules by name
		// and the only way to know if a name is a rule or a charClass is to know all the charClass
		// charClass can be compiled alone because when a charClass refers a name, this is another charClass and not a rule
		
		// 1st round : get the charClasses
		Map<String,CharClass> charClassesMap = new HashMap<>();
		charClassesMap.put("WS", Character::isWhitespace);
		for (Node ruleOrCharClass:rules.childs) {
			Branch b = (Branch) ruleOrCharClass;
			String name = b.childs.get(0).stringValue();
			if (b.childs.size()==2 && b.childs.get(1).name.equals("charClassOr")) {
				CharClass charClass = compile(b.childs.get(1), charClassesMap);
				charClassesMap.put(name, charClass);
			}
		}
		
		// 2nd round ; get the rules
		Map<String,Rule> rulesMap = new HashMap<>();
		rulesMap.put("WS", new RuleChar(false, charClassesMap.get("WS")));
		for (Node ruleOrCharClass:rules.childs) {
			Branch b = (Branch) ruleOrCharClass;
			String name = b.childs.get(0).stringValue();
			if (b.childs.size()!=2 || ! b.childs.get(1).name.equals("charClassOr")) {
				boolean asLeaf;
				Node ruleNode;
				if (b.childs.size()==3) {
					asLeaf = false;
					ruleNode = b.childs.get(2);
				} else {
					asLeaf = true;
					ruleNode = b.childs.get(1);
				}
				Rule ruleImpl = new RuleRename(name, compile(rulesMap, charClassesMap, ruleNode));
				if (asLeaf)
					ruleImpl = new RuleBranchToLeaf(ruleImpl);
				rulesMap.put(name, ruleImpl);
			}
//			System.out.println(ruleName+(asLeaf?"":"[]")+" = "+ruleImpl+";");
		}
		Rule main = rulesMap.get("main");
		if (main==null)
			throw new RuntimeException("The main rule has not been defined.");
		return toLexer(main);
	}
	
	public static String toString(File file) throws IOException {
		try (java.io.Reader reader = new java.io.FileReader(file)) {
	    	char[] buf = new char[4096];
	    	int i;
	    	java.io.StringWriter writer = new java.io.StringWriter(); 
	    	while ((i=reader.read(buf))>0) {
	    		writer.write(buf, 0, i);
	    	}
	    	return writer.toString();
    	}
	}
	
    public static Lexer toLexer(String lexerExpression) {
    	return toLexer(LEXER_PARSER.parse(lexerExpression));
    }
    
	// TODO : better perfs can be achieved if using ranges and just doing range
	// intersection/complementary, with maybe just a special case for final char
	// classes with only 1 possible value
    private static CharClass compile(Node charClassNode, Map<String, CharClass> charClassesMap) {
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
    		return CharClass.fromChar(unescape(l.value).charAt(0));
    	}
    	if (charClassNode.name.equals("charClassName")) {
    		Leaf l = (Leaf) charClassNode;
    		String name = l.value.toString();
    		return new CharClass() {
				@Override public boolean contains(char c) {
					CharClass cClass = charClassesMap.get(name);
					if (cClass == null)
						throw new RuntimeException("Got a reference to an unknown CharClass \""+name+"\"");
					return cClass.contains(c);
				}
				@Override public String toString() { return name; } 
			};
    	}
    	if (charClassNode.name.equals("charClassNot")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0), charClassesMap);
    		return CharClass.negate(compile(b.childs.get(1), charClassesMap));
    	}
    	if (charClassNode.name.equals("charClassAnd")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0), charClassesMap);
    		CharClass[] charClasses = new CharClass[b.childs.size()];
    		for (int i=0; i<b.childs.size(); i++) {
    			charClasses[i] = compile(b.childs.get(i), charClassesMap);
    		}
    		return CharClass.and(charClasses);
    	}
    	if (charClassNode.name.equals("charClassOr")) {
    		Branch b = (Branch) charClassNode;
    		if (b.childs.size()==1) return compile(b.childs.get(0), charClassesMap);
    		CharClass[] charClasses = new CharClass[b.childs.size()];
    		for (int i=0; i<b.childs.size(); i++) {
    			charClasses[i] = compile(b.childs.get(i), charClassesMap);
    		}
    		return CharClass.or(charClasses);
    	}
    	throw new RuntimeException("Do not know how to compile into a char class node "+charClassNode);
    }
    
    private static Rule compile(Map<String,Rule> rulesMap, Map<String, CharClass> charClassesMap , Node ruleNode) {
    	if (ruleNode.name.equals("ruleTerm")) {
    		Branch b = (Branch) ruleNode;
    		Node c = b.childs.get(0);
    		Rule rule;
    		if (c.name.equals("ruleOr")) {
    			rule = compile(rulesMap, charClassesMap, c);
    		} else {
    			// this is a capturable content, next node is either a string, a range, a char or a ref to another rule
    			boolean capture = false;;
    			if (c.name.equals("capture")) {
    				capture = true;
    				c = b.childs.get(1);
    			}
    			if (c.name.equals("string")) {
    				Leaf l = (Leaf) c;
    				// TODO : unescape unicode, ' and \ chars
    				l.value = unescape(l.value);
    				rule = new RuleString(capture, l.value);
    			} else if (c.name.equals("charClassOr")) {
    				rule = new RuleChar(capture, compile(c, charClassesMap));
    			} else if (c.name.equals("anyName")) {
    				Leaf l = (Leaf) c;
    				String name = c.stringValue();
    				CharClass cClass = charClassesMap.get(name);
    				if (cClass!=null) {
    					rule = new RuleRename(name, new RuleChar(capture, cClass));
    				} else {    					
    					rule = new RuleRef(capture, (String)l.value, rulesMap);
    				}
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
    		if (b.childs.size()==1) return compile(rulesMap, charClassesMap, b.childs.get(0));
    		List<Rule> subRules = new ArrayList<>(b.childs.size());
    		for (Node child:b.childs) {
    			subRules.add(compile(rulesMap, charClassesMap, child));
    		}
    		return new RuleAnd(subRules);
    	}
    	if (ruleNode.name.equals("ruleOr")) {
    		Branch b = (Branch) ruleNode;
    		if (b.childs.size()==1) return compile(rulesMap, charClassesMap, b.childs.get(0));
    		List<Rule> subRules = new ArrayList<>(b.childs.size());
    		for (Node child:b.childs) {
    			subRules.add(compile(rulesMap, charClassesMap, child));
    		}
    		return new RuleOr(subRules);
    	}
    	throw new RuntimeException("Do not know how to compile node named "+ruleNode.name+". Full node is "+ruleNode);
    }

    private static Lexer toLexer(Rule rule) {
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
    
    private static CharSequence unescape(CharSequence cSeq) {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0, l=cSeq.length(); i<l;) {
    		char c = cSeq.charAt(i++);
    		if (c=='\\') {
    			c = cSeq.charAt(i++);
    			switch (c) {
    			case '\'' : break;
    			case '\\' : break;
    			case 'u' :
    				int j = hex2dec(cSeq.charAt(i))<<12
    					| hex2dec(cSeq.charAt(i+1))<<8
    					| hex2dec(cSeq.charAt(i+2))<<4
    					| hex2dec(cSeq.charAt(i+3));
    				c = (char) j;
    				i+=4; // eats 4 hex, \ and u have already been eaten
    				break;
    			default :
    				throw new RuntimeException("Bad unicode expressed char. Should be '\\u' followed by 4 hex digits. This was not the case in \""+cSeq+"\".");
    			}
    		}
    		sb.append(c);
    	}
    	return sb.toString();
    };
    
    private static int hex2dec(char c) {
    	if (c>='0' && c<='9') return c-'0';
    	if (c>='A' && c<='F') return c-'A'+10;
    	if (c>='a' && c<='f') return c-'a'+10;
    	throw new RuntimeException("Got a char that should be interpreted has being an hexadecimal value but it is '"+c+"'");
    }
    
    private static final Lexer LEXER_PARSER;
    static {
    	
    	final Map<String,Rule> rules = new HashMap<>();
    	
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
    	Rule anyName = new RuleBranchToLeaf(new RuleAnd(
			new RuleChar(true, _letter),
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleChar(true, _letter_or_digit_or_underscode))
    	));
    	Rule capture = new RuleChar(false, CharClass.fromChar('+'));
    	Rule not = new RuleChar(false, CharClass.fromChar('!'));
//    	Rule ruleName = new RuleRename("ruleName", new RuleRef(true, "anyName", rules));
//    	Rule charClassName = new RuleRename("charClassName", new RuleRef(true, "anyName", rules));
    	Rule ruleOrRef = new RuleRef(true, "ruleOr", rules);
    	Rule ruleStringRef = new RuleRef(true, "string", rules);
    	CharClass HEXA = CharClass.or(
    		_digit,
    		CharClass.fromRange('a', 'z'),
    		CharClass.fromRange('A', 'Z')
    	);
    	Rule innerChar = new RuleOr(
			new RuleChar(true, CharClass.negate(
				CharClass.or(
					CharClass.fromChar('\''),
					CharClass.fromChar('\\')
				)
			)),
			new RuleAnd(
				new RuleChar(true, CharClass.fromChar('\\')),
				new RuleChar(true,CharClass.or(
					CharClass.fromChar('\''),
					CharClass.fromChar('\\')
				))
			),
			new RuleAnd(
				new RuleString(true, "\\u"),
				new RuleCardinality(4, 4, false, new RuleChar(true, HEXA))
			)
		);
    	Rule string = new RuleBranchToLeaf(new RuleAnd(
			SQ,
			new RuleCardinality(0, Integer.MAX_VALUE, false, innerChar),
    		SQ
    	));
    	Rule charClassChar = new RuleRename("char", new RuleBranchToLeaf(new RuleAnd(
			SQ,
			innerChar,
			SQ
		)));
    	Rule range = new RuleAnd(
    		charClassChar,
    		new RuleString(false, ".."),
    		charClassChar
    	);
    	Rule charClassOrRef = new RuleRef(true, "charClassOr", rules);
    	Rule charClassAndRef = new RuleRef(true, "charClassAnd", rules);
    	Rule charClassNotRef = new RuleRef(true, "charClassNot", rules);
    	Rule charClassNot = new RuleAnd(
    		new RuleCardinality(0, 1, false, new RuleRef(true, "not", rules)),
    		skipSpaces,
    		new RuleOr(
				new RuleRef(true, "range", rules),
				new RuleRef(true, "char", rules),
				new RuleRef(true, "charClassName", rules),
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
					new RuleCardinality(0, 1, false, new RuleRef(true, "capture", rules)),
					new RuleOr(
						ruleStringRef,
						new RuleRef(true, "anyName", rules),
			    		charClassOrRef
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
	    	new RuleCardinality(0, 1, false, new RuleRef(true, "cardinality", rules))
    	);
    	Rule integer = new RuleBranchToLeaf(new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleRef(true, "DIGIT", rules)));
    	Rule cardinality = new RuleAnd(
    		new RuleOr(
				new RuleChar(true, CharClass.fromChar('?')),
				new RuleChar(true, CharClass.fromChar('*')),
				new RuleChar(true, CharClass.fromChar('+')),
				new RuleAnd(
					new RuleChar(false, CharClass.fromChar('{')),
					skipSpaces,
					new RuleRef(true, "integer", rules),
					skipSpaces,
					new RuleChar(false, CharClass.fromChar(',')),
					skipSpaces,
					new RuleCardinality(0, 1, false, new RuleRef(true, "integer", rules)),
					skipSpaces,
					new RuleChar(false, CharClass.fromChar('}'))
				)
    		),
    		new RuleCardinality(0, 1, false, new RuleChar(true, CharClass.fromChar('?')))
    	);
    	Rule ruleTermRef = new RuleRef(true, "ruleTerm", rules);
    	Rule ruleAnd = new RuleAnd(
			ruleTermRef,
			new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
				skipSpaces1,
				ruleTermRef
			))
		);
    	Rule ruleAndRef = new RuleRef(true, "ruleAnd", rules);
    	Rule ruleOr = new RuleAnd(
    		ruleAndRef,
    		new RuleCardinality(0, Integer.MAX_VALUE, false, new RuleAnd(
   				skipSpaces,
   				PIPE,
   				skipSpaces,
   	    		ruleAndRef
    		))
    	);
    	Rule asBranch = new RuleString(false, "[]");
    	Rule rule = new RuleOr(
    		new RuleAnd(
	    		new RuleRef(true, "charClassName", rules),
	    		skipSpaces,
	    		EQ,
	    		skipSpaces,
	    		charClassOrRef,
	    		skipSpaces,
	    		SM
	    	),
    		new RuleAnd(
	    		new RuleRef(true, "ruleName", rules),
	    		new RuleCardinality(0, 1, false, new RuleRef(true, "asBranch", rules)),
	    		skipSpaces,
	    		EQ,
	    		skipSpaces,
	    		ruleOrRef,
	    		skipSpaces,
	    		SM
	    	)
    	);
    	Rule main = new RuleRename("main", new RuleAnd(
			skipSpaces,
			new RuleCardinality(1, Integer.MAX_VALUE, false, new RuleAnd(new RuleRef(true, "rule", rules), skipSpaces))
		));
    	rules.put("WS", new RuleRename("WS", new RuleChar(false, _ws)));
    	rules.put("LETTER", new RuleRename("LETTER", new RuleChar(true, _letter)));
    	rules.put("DIGIT", new RuleRename("DIGIT", new RuleChar(true, _digit)));
    	rules.put("integer", new RuleRename("integer", integer));
    	rules.put("main", new RuleRename("main", main));
    	rules.put("rule", new RuleRename("rule", rule));
    	rules.put("anyName", new RuleRename("anyName", anyName));
    	rules.put("charClassName", new RuleRename("charClassName", anyName));
    	rules.put("ruleName", new RuleRename("ruleName", anyName));
    	rules.put("asBranch", new RuleBranchToLeaf(new RuleRename("asBranch", asBranch)));
    	rules.put("ruleOr", new RuleRename("ruleOr", ruleOr));
    	rules.put("ruleAnd", new RuleRename("ruleAnd", ruleAnd));
    	rules.put("ruleTerm", new RuleRename("ruleTerm", ruleTerm));
    	rules.put("capture", new RuleBranchToLeaf(new RuleRename("capture", capture)));
    	rules.put("cardinality", new RuleRename("cardinality", cardinality));
    	rules.put("charClassOr", new RuleRename("charClassOr", charClassOr));
    	rules.put("charClassAnd", new RuleRename("charClassAnd", charClassAnd));
    	rules.put("charClassNot", new RuleRename("charClassNot", charClassNot));
    	rules.put("not", new RuleBranchToLeaf(new RuleRename("not", not)));
    	rules.put("string", new RuleRename("string", string));
    	rules.put("range", new RuleRename("range", range));
    	rules.put("char", new RuleRename("char", charClassChar));
    	LEXER_PARSER = toLexer(main);
    }

    public static void main(String...args) throws Throwable {
    	
    	// simple demo test
    	// tab is char 9, so \u0009 is a tab using the unicode encoded format
    	System.out.println(toLexer("main[] = +hello ' ' +world ; hello = +'hel\u0009lo'; world = 'world';").parse("hel	lo world"));
    	
    	// demo using the csv grammar. Note the fact that it can match empty fields
    	String csvGrammar = toString(new File("grammars/csv.g"));
    	Lexer csvLexer = toLexer(csvGrammar);
    	System.out.println(csvLexer.parse(
    		"line name,b column,some string\n"
    		+"line 1, some spaces ,\"got a comma , inside a quoted value to test protection\"\n"
    		+"line 2 with one field,,\"\"\"\"\n"
    	));
    	
    	// demo/checking using the grammar of the grammar itself
    	String lexerGrammar = toString(new File("src/lexer/lexer.g"));
    	Lexer lexerLexer = LEXER_PARSER;
    	Node node;
    	for (int i=0; i<4; i++) {
	    	node = lexerLexer.parse(lexerGrammar);
	    	System.out.println("["+i+"] ================================================");
	    	System.out.println(node);
	    	lexerLexer = toLexer(node);
    	}
		
    }
}
