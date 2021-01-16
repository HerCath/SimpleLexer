package lexer.impl;

import java.util.Iterator;

public class RuleRename implements Rule {

	final String name;
	final Rule subRule;
	
	public RuleRename(String name, Rule subRule) {
		this.name = name;
		this.subRule = subRule;
	}

	@Override public Iterator<Object> getStates(Context ctx) {
		return subRule.getStates(ctx);
	}

	@Override public MatchedContent match(Context ctx, Iterator<Object> states) {
        if (!states.hasNext()) return null;
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
