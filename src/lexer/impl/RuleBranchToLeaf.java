package lexer.impl;

import lexer.Leaf;

public class RuleBranchToLeaf extends Rule<State> {

    private final Rule subRule;

    public RuleBranchToLeaf(Rule subRule) { this.subRule = subRule; }

    @Override public State createState(Context ctx) { return subRule.createState(ctx); }

    @Override public MatchedContent match(Context ctx, State state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
	        mc = subRule.match(ctx, state);
	        if (mc!=null && mc.captured!=null) {
	            mc.captured = new Leaf(mc.captured.name, mc.captured.stringValue());
	        }
	        return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override public String toString() { return "flatten "+subRule; }
    
}
