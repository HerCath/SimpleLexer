package lexer.impl;

import lexer.Leaf;

public class RuleString extends Capturable implements StateLessRule {

    final CharSequence cSeq;

    public RuleString(boolean capture, CharSequence cSeq) {
        super(capture);
        if (cSeq.length()==0) throw new RuntimeException("A rule that match the empty string is not a legit one.");
        this.cSeq = cSeq;
    }

    @Override public MatchedContent match(Context ctx, States states) {
    	if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
//			states.next(ctx);
	        final int pos = ctx.pos;
	        int i=0;
	        while (i<cSeq.length() && ctx.is(cSeq.charAt(i))) {
	            i++;
	            ctx.pos++;
	        }
	        if (i==cSeq.length()) {
	            return mc=new MatchedContent(capture?new Leaf("string", cSeq):null);
	        }
	        ctx.pos = pos;
	        return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override public String toString() { return "\""+cSeq+"\""; }
    
}
