package lexer.impl;

import lexer.Leaf;

public class RuleChar extends Capturable<SingleMatchState> {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }
    
    @Override public SingleMatchState createState(Context ctx) {
        return new SingleMatchState(ctx) {
        	public String toString() { return "SingleMatchState{hasBeenUsed="+hasBeenUsed+", rule="+RuleChar.this+"}"; }
        };
    }

    @Override public MatchedContent match(Context ctx, SingleMatchState state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		if (state.hasBeenUsed) return null;
    		state.hasBeenUsed = true;
	        if (ctx.is(cClass)) {
	        	int from = ctx.pos;
	            char c = ctx.poll(); // poll to consume. needed even when not capturing
	            return mc = new MatchedContent(from, capture ? new Leaf("char", c) : null, ctx.pos);
	        }
	        return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override public String toString() { return capture ? "+"+cClass.toString() : cClass.toString(); }
}
