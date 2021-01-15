package lexer.impl;

import lexer.Leaf;

public class RuleChar extends Capturable implements StateLessRule {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }

    @Override public MatchedContent tryToMatch(Context ctx, Object state) {
        if (ctx.is(cClass)) {
        	System.out.println("char["+ctx.pos+"]="+ctx.peek()+" matches");
            char c = ctx.poll(); // poll to consume. needed even when not capturing
            return new MatchedContent(capture ? new Leaf("char", c) : null);
        }
    	System.out.println("char["+ctx.pos+"] does not match (remaining = "+ctx.remaining()+")");
        return null;
    }
    
    @Override
    public int minSize() { return 1; }
    
    @Override
    public String toString() { return cClass.toString(); }
}
