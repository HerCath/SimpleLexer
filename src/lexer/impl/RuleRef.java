package lexer.impl;

import java.util.Map;

public class RuleRef extends Capturable implements Rule<State> {

    final String name;
    final Map<String, Rule> rules;

    public RuleRef(boolean capture, String name, Map<String, Rule> rules) {
        super(capture);
        this.name = name;
        this.rules = rules;
    }

    @Override public State createState(Context ctx) { return rules.get(name).createState(ctx); }

    @Override public MatchedContent match(Context ctx, State state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
	        mc = rules.get(name).match(ctx, state);
	        if (mc!=null && !capture) mc.captured = null;
	        return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    public String toString() { return capture ? "+"+name : name; }
    
}