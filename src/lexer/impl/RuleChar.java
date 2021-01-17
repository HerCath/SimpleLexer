package lexer.impl;

import lexer.Leaf;

public class RuleChar extends Capturable implements StateLessRule {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }

    @Override public MatchedContent match(Context ctx, States states) {
    	if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
//    		System.out.println("RuleChar for "+cClass+" is being evaluated with states "+states);
			//states.next(ctx);
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
