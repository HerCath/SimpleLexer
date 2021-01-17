package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class RuleOr implements Rule {

    final List<Rule> subRules;
    
    public RuleOr() { subRules = new ArrayList<>(); }
    public RuleOr(List<Rule> subRules) { this.subRules = subRules; }
    public RuleOr(Rule...subRules) { this(Arrays.asList(subRules)); }
    
    class RuleOrStates extends States {
    	private int subRuleIdx;
    	private States subRuleStates;
    	RuleOrStates(Context ctx) {
    		super(ctx);
    	}
    	protected boolean _hasNext(Context ctx) {
    		while (subRuleIdx<subRules.size()) {
    			if (subRuleStates == null) subRuleStates = subRules.get(subRuleIdx).createStates(ctx);
    			if (subRuleStates.hasNext(ctx)) return true;
    			subRuleStates = null;
    			subRuleIdx++;
    		}
    		return false;
    	}
    	protected void _next(Context ctx) {
    		hasNext(ctx);
    		subRuleStates.next(ctx);
    	}
    	public String toString() {
    		return "OR["+subRuleIdx+"/"+subRules.size()+"].states="+subRuleStates;
    	}
    }

	@Override public States createStates(Context ctx) {
        return new RuleOrStates(ctx);
    }

    @Override public MatchedContent match(Context ctx, States states) {
        if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
            RuleOrStates _states = (RuleOrStates) states;
            do {
                mc=subRules.get(_states.subRuleIdx).match(ctx, _states.subRuleStates);
                if (mc!=null) return mc;
                states.next(ctx);
            } while (states.hasNext(ctx));
            return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("(").append(subRules.get(0)).append(")");
    	for (int i=1, l=subRules.size(); i<l; sb.append(" | (").append(subRules.get(i++)).append(")") );
    	return sb.toString();
    }
}
