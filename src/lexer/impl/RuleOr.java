package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RuleOr implements Rule {

    final List<Rule> subRules;
    
    public RuleOr() { subRules = new ArrayList<>(); }
    public RuleOr(List<Rule> subRules) { this.subRules = subRules; }
    public RuleOr(Rule...subRules) { this(Arrays.asList(subRules)); }

    private static class RuleOrState implements Iterator<Object> {
        private Context ctx;
        private Iterator<Rule> subRules;
        private Rule subRule;
        private Iterator<Object> currentSubRuleStates;

        RuleOrState(Context ctx, Iterator<Rule> subRules) {
            this.ctx = ctx;
            this.subRules = subRules;
        }

        @Override public boolean hasNext() {
            while (true) {
                while (currentSubRuleStates==null) {
                    if (!subRules.hasNext()) return false;
                    subRule=subRules.next();
                    currentSubRuleStates=subRule.getStates(ctx);
                }
                if (currentSubRuleStates.hasNext()) return true;
                currentSubRuleStates = null;
            }
        }

        @Override public Object next() {
            return null;
        }
    }

	@Override public Iterator<Object> getStates(Context ctx) {
        return new RuleOrState(ctx, subRules.iterator());
    }

    @Override public MatchedContent match(Context ctx, Iterator<Object> states) {
        if (!states.hasNext()) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
            RuleOrState _states = (RuleOrState) states;
            do {
                states.next();
                mc=_states.subRule.match(ctx, _states.currentSubRuleStates);
                if (mc!=null) return mc;
            } while (states.hasNext());
            return null;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
//    @Override
//    public int minSize(Rule rootRule) {
//    	int min = Integer.MAX_VALUE;
//    	for (int i=0, l=subRules.size(); i<l;i++) {
//    		int ms = subRules.get(i).minSize(rootRule);
//    		if (min>ms) min=ms;
//    	}
//    	return min;
//    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("(").append(subRules.get(0)).append(")");
    	for (int i=1, l=subRules.size(); i<l; sb.append(" | (").append(subRules.get(i++)).append(")") );
    	return sb.toString();
    }
}
