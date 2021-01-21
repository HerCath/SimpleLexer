package lexer.impl;

public class RuleRename extends Rule<State> {

	final String name;
	final Rule subRule;
	
	public RuleRename(String name, Rule subRule) {
		this.name = name;
		this.subRule = subRule;
	}

	@Override public State createState(Context ctx) {
		return subRule.createState(ctx);
	}

	@Override public MatchedContent match(Context ctx, State state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
			mc = subRule.match(ctx, state);
			if (mc != null && mc.captured!=null) mc.captured.name = name;
			return mc;
    	} catch (RuntimeException re) {
    		throw new RuntimeException("Failed with sub rule to be renamed to \""+name+"\".", re);
    	} finally {
    		ctx.leave(this, mc);
    	}
	}

	public String toString() { return subRule.toString(); };
	
}
