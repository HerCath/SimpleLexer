package lexer.impl;

public class RuleRename implements Rule {

	final String name;
	final Rule subRule;
	
	public RuleRename(String name, Rule subRule) {
		this.name = name;
		this.subRule = subRule;
	}

	@Override public Object createInitialState(Context ctx) {
		return subRule.createInitialState(ctx);
	}

	@Override public boolean nextState(Context ctx, Object state) {
		return subRule.nextState(ctx, state);
	}

	@Override public MatchedContent tryToMatch(Context ctx, Object state) {
		MatchedContent mc = subRule.tryToMatch(ctx, state);
		if (mc != null && mc.captured!=null) mc.captured.name = name;
		return mc;
	}
	
	@Override public int minSize() { return subRule.minSize(); }

}
