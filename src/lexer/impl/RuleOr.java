package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RuleOr implements Rule {

    final List<Rule> subRules;

    static class State {
        int ruleIdx;
        Object ruleState;
    }
    
    public RuleOr() { subRules = new ArrayList<>(); }
    public RuleOr(List<Rule> subRules) { this.subRules = subRules; }
    public RuleOr(Rule...subRules) { this(Arrays.asList(subRules)); }

	@Override
    public Object createInitialState(Context ctx) {
        State state = new State();
        state.ruleIdx = 0;
        state.ruleState = subRules.get(0).createInitialState(ctx);
        return state;
    }

    @Override
    public boolean nextState(Context ctx, Object state) {
        State _state = (State) state;

        if (_state.ruleIdx>=subRules.size()) return false;

        if (subRules.get(_state.ruleIdx).nextState(ctx, _state.ruleState)) return true;

        _state.ruleIdx++;
        _state.ruleState = null;

        if (_state.ruleIdx>=subRules.size()) return false;

        _state.ruleState = subRules.get(_state.ruleIdx).createInitialState(ctx);
        return true;
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	State _state = (State) state;
    	int loop = 0;
    	try {
	        while (true) {
	        	loop++;
	        	mc=subRules.get(_state.ruleIdx).tryToMatch(ctx, _state.ruleState);
	        	if (mc!=null) return mc;
	        	if (!nextState(ctx, state)) return null;
	        }
    	} catch (Exception e) {
    		throw new RuntimeException("rule crashed with state at idx "+_state.ruleIdx+"/"+subRules.size()+". It was its loop #"+loop+". Rule is "+this+".", e);
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
