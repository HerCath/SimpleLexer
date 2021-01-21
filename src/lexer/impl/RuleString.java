package lexer.impl;

import lexer.Leaf;

public class RuleString extends Capturable<SingleMatchState> {

    final CharSequence cSeq;

    public RuleString(boolean capture, CharSequence cSeq) {
        super(capture);
        this.cSeq = cSeq;
    }
    
    @Override public SingleMatchState createState(Context ctx) {
        return new SingleMatchState(ctx) {
        	public String toString() { return "SingleMatchState{hasBeenUsed="+hasBeenUsed+", rule="+RuleString.this+"}"; }
        };
    }

    @Override public MatchedContent match(Context ctx, SingleMatchState state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		if (state.hasBeenUsed) return null;
    		state.hasBeenUsed = true;
	        final int pos = ctx.pos;
	        int i=0;
	        while (i<cSeq.length() && ctx.is(cSeq.charAt(i))) {
	            i++;
	            ctx.pos++;
	        }
	        if (i==cSeq.length()) {
	            return mc=new MatchedContent(pos, capture?new Leaf("string", cSeq):null, ctx.pos);
	        }
	        ctx.pos = pos;
	        return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override public String toString() { return "'"+cSeq+"'"; }
    
}
