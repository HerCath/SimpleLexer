package lexer.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Stack;

public class Context {
	
	final static PrintStream STDOUT = System.out;
	
	static class ParserStack {
		Rule rule;
		int pos;
		boolean debug;
		public ParserStack(Rule rule, int pos, boolean debug) {
			this.rule = rule;
			this.pos = pos;
			this.debug = debug;
		}
		
	}
	
    public final CharSequence cSeq;
    public boolean debug = false;
    public int pos;
    Stack<ParserStack> stack = new Stack<>();
    static String baseName(Rule rule) {
    	String cName = rule.getClass().getName();
    	int i = cName.lastIndexOf('.');
    	return i>0 ? cName.substring(i+1) : cName;
    }
    String snapshot() {
    	StringBuilder sb = new StringBuilder();
    	final int DELTA = 3; 
    	if (pos-DELTA>-1) sb.append("...");
    	for (int delta=-DELTA; delta<=DELTA; delta++) {
    		int i = pos+delta;
    		if (i==-1) sb.append('"');
    		if (i<0) continue;
    		if (i==cSeq.length()) sb.append('"');
    		if (i>=cSeq.length()) continue;
    		if (delta==0) sb.append('[');
    		sb.append(cSeq.charAt(i));
    		if (delta==0) sb.append(']');
    	}
    	if (pos+DELTA<cSeq.length()) sb.append("...");
    	return sb.toString();
    }
    void enter(Rule rule) {
    	stack.push(new ParserStack(rule, pos, debug));
    	debug |= rule.debug;
    	if (debug) System.out.println("IN "+baseName(rule)+": at "+snapshot()+", entering rule "+rule);
    }
    void indent() {
    	int i = stack.size();
    	STDOUT.print(i);
    	while (i-->0) {
    		STDOUT.print("  ");
    	}
    }
    void leave(Rule rule, MatchedContent mc) {
    	ParserStack stkElt = stack.peek();
    	if (mc==null) pos = stkElt.pos;
    	else pos = mc.to;
    	try {
    		if (rule!=stkElt.rule)
    			System.err.println("We have a rule that is popping up a stack element it does not own. Stack size is "+stack.size()+". Stack rule is "+stkElt.rule+". Rule trying to pop it up is "+rule+".");
	    	if (mc==null) {
	    		if (pos != stkElt.pos)
	    			System.err.println("We have a rule that did not matched, it entered at pos "+stkElt.pos+" but rolledback at a different location "+pos+". rule is implemented by "+stkElt.rule.getClass()+". It is "+stkElt.rule);
	    		if (debug) System.out.println("FAIL : Rule "+stkElt.rule+" did not matched.");
	    	} else {
		    	if (debug) System.out.println("SUCCESS : Rule "+stkElt.rule+" matched and schewed "+(pos-stkElt.pos)+" chars: "+cSeq.subSequence(stkElt.pos, pos)+". It capture "+mc);
	    	}
    	} finally {
    		stack.pop();
    		debug = stkElt.debug;
    	}
    }
    public Context(CharSequence cSeq) {
    	this.cSeq = cSeq;
    	/*System.setOut(new PrintStream(new OutputStream() {
    		boolean indentNext = true;
			@Override public void write(int b) throws IOException {
				if (indentNext) {
					indent();
					indentNext = false;
				}
				STDOUT.write(b);
				indentNext = (b=='\n');
			}
		}));*/
    }
    public char peek() { return cSeq.charAt(pos); }
    public char poll() { return cSeq.charAt(pos++); }
	public boolean is(char c) { return !atEnd() && peek()==c; }
    public boolean is(CharClass cClass) { return !atEnd() && cClass.contains(peek()); }
    public boolean atEnd() { return pos>=cSeq.length(); }
}