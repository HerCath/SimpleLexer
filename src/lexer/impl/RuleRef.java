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
        MatchedContent mc = rules.get(name).tryToMatch(ctx, state);
        if (mc!=null && !capture) mc.captured = null;
        return mc;
    }
    
    @Override
    public int minSize() { return rules.get(name).minSize(); }
    
}
