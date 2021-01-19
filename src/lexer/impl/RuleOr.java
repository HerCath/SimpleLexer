package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lexer.Branch;
import lexer.impl.RuleOr.RuleOrStates;

public class RuleOr extends Rule<RuleOrStates> {

    final List<Rule> subRules;
    
    public RuleOr() { subRules = new ArrayList<>(); }
    public RuleOr(List<Rule> subRules) { this.subRules = subRules; }
    public RuleOr(Rule...subRules) { this(Arrays.asList(subRules)); }
    
    class RuleOrStates extends State {
    	private int subRuleIdx;
    	private State subRuleState;
    	RuleOrStates(Context ctx) { super(ctx); }
    	public String toString() {
    		return "OR["+subRuleIdx+"/"+subRules.size()+"].states="+subRuleState;
    	}
    }

	@Override public RuleOrStates createState(Context ctx) {
        return new RuleOrStates(ctx);
    }

    @Override public MatchedContent match(Context ctx, RuleOrStates state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
            while (true) {
//            	System.out.println("DEBUG 1 Inside OR loop, state is "+state);
            	if (state.subRuleIdx>=subRules.size()) return null;
            	if (state.subRuleState==null) state.subRuleState = subRules.get(state.subRuleIdx).createState(ctx);
//            	System.out.println("DEBUG 2 Inside OR loop, state is "+state);
            	MatchedContent subMatch = subRules.get(state.subRuleIdx).match(ctx, state.subRuleState);
                if (subMatch!=null) {
                	Branch b = new Branch("*");
                	if (subMatch.captured!=null) {
                		if (subMatch.captured.name.equals("*")) {
                			b.childs.addAll(((Branch)subMatch.captured).childs);
                		} else {
                			b.childs.add(subMatch.captured);
                		}
                	}
                	return mc = new MatchedContent(subMatch.from, b, subMatch.to);
                }
                state.subRuleIdx++;
                state.subRuleState = null;
            }
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
