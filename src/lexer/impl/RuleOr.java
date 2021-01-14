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

        _state.ruleState = subRules.get(0).createInitialState(ctx);;
        return true;
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
        State _state = (State) state;
        return subRules.get(_state.ruleIdx).tryToMatch(ctx, _state.ruleState);
    }
    
    @Override
    public int minSize() {
    	int min = Integer.MAX_VALUE;
    	for (int i=0, l=subRules.size(); i<l;i++) {
    		int ms = subRules.get(i).minSize();
    		if (min>ms) min=ms;
    	}
    	return min;
    }
    
}
