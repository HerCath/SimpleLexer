package lexer.impl;

import java.util.Map;

public class RuleRef extends Capturable implements Rule {

    final String name;
    final Map<String, Rule> rules;

    public RuleRef(boolean capture, String name, Map<String, Rule> rules) {
        super(capture);
        this.name = name;
        this.rules = rules;
    }

    @Override
    public Object createInitialState(Context ctx) { return rules.get(name).createInitialState(ctx); }

    @Override
    public boolean nextState(Context ctx, Object state) { return rules.get(name).nextState(ctx, state); }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
	        mc = rules.get(name).tryToMatch(ctx, state);
	        if (mc!=null && !capture) mc.captured = null;
	        return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    public String toString() { return capture ? "+"+name : name; }
    
//    @Override public int minSize(Rule rootRule) { return rootRule==this?0:rules.get(name).minSize(rootRule); }
    
}
