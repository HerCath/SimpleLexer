package lexer.impl;

import java.util.Iterator;

public class RuleRename implements Rule {

	final String name;
	final Rule subRule;
	
	public RuleRename(String name, Rule subRule) {
		this.name = name;
		this.subRule = subRule;
	}

	@Override public States createStates(Context ctx) {
		return subRule.createStates(ctx);
	}

	@Override public MatchedContent match(Context ctx, States states) {
        if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
			mc = subRule.match(ctx, states);
			if (mc != null && mc.captured!=null) mc.captured.name = name;
			return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
	}
	
//	@Override public int minSize(Rule rootRule) { return subRule.minSize(rootRule); }

	public String toString() { return subRule.toString(); };
	
}
