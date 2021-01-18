package lexer.impl;

import lexer.Leaf;

public class RuleChar extends Capturable implements SingleMatchRule {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }

    @Override public MatchedContent match(Context ctx, SingleMatchState state) {
    	if (state.hasBeenUsed) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		state.hasBeenUsed = true;
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
