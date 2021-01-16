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
		public ParserStack(Rule rule, int pos) {
			this.rule = rule;
			this.pos = pos;
		}
		
	}
	
    final CharSequence cSeq;
    int pos;
    Stack<ParserStack> stack = new Stack<>();
    
    void enter(Rule rule) {
    	stack.push(new ParserStack(rule, pos));
    	System.out.println("IN  : at pos #"+pos+", entering rule "+rule);
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
    	try {
    		if (rule!=stkElt.rule)
    			System.err.println("We have a rule that is popping up a stack element it does not own. Stack size is "+stack.size()+". Stack rule is "+stkElt.rule+". Rule trying to pop it up is "+rule+".");
	    	if (mc==null) {
	    		if (pos != stkElt.pos)
	    			System.err.println("We have a rule that did not matched, it entered at pos "+stkElt.pos+" but rolledback at a different location "+pos+". rule is implemented by "+stkElt.rule.getClass()+". It is "+stkElt.rule);
	    		System.out.println("FAIL : Rule "+stkElt.rule+" did not matched.");
	    	} else {
		    	System.out.print("SUCCESS : Rule "+stkElt.rule+" matched and schewed "+(pos-stkElt.pos)+" chars: "+cSeq.subSequence(stkElt.pos, pos)+". ");
		    	if (mc.captured!=null) {
		    		System.out.println("It captured \""+mc.captured.stringValue().replace("\\", "\\\\").replace("\"", "\\\"")+"\".");
		    	} else {
		    		System.out.println("It discarded its content.");
		    	}
	    	}
    	} finally {
    		stack.pop();
    	}
    }
    public Context(CharSequence cSeq) {
    	this.cSeq = cSeq;
    	System.setOut(new PrintStream(new OutputStream() {
    		boolean indentNext = true;
			@Override public void write(int b) throws IOException {
				if (indentNext) {
					indent();
					indentNext = false;
				}
				STDOUT.write(b);
				indentNext = (b=='\n');
			}
		}));
    }
    int remaining() { return cSeq.length()-pos; }
    char peek() { return cSeq.charAt(pos); }
    char poll() { return cSeq.charAt(pos++); }
    void skip(CharClass cClass) { while (is(cClass)) pos++; }
	boolean is(char c) { return !atEnd() && peek()==c; }
    boolean is(CharClass cClass) { return !atEnd() && cClass.contains(peek()); }
    boolean atEnd() { return pos>=cSeq.length(); }
}