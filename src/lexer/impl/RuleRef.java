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

    @Override public States createStates(Context ctx) { return rules.get(name).createStates(ctx); }

    @Override public MatchedContent match(Context ctx, States states) {
        if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
	        mc = rules.get(name).match(ctx, states);
	        if (mc!=null && !capture) mc.captured = null;
	        return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    public String toString() { return capture ? "+"+name : name; }
    
}