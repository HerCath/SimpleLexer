package lexer.impl;

import java.util.Iterator;

import lexer.Leaf;

public class RuleChar extends Capturable implements StateLessRule {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }

    @Override public MatchedContent match(Context ctx, Iterator<Object> states) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
			if (!states.hasNext()) return null;
			states.next();
	        if (ctx.is(cClass)) {
	            char c = ctx.poll(); // poll to consume. needed even when not capturing
	            return mc = new MatchedContent(capture ? new Leaf("char", c) : null);
	        }
	        return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override public String toString() { return capture ? "+"+cClass.toString() : cClass.toString(); }
}
