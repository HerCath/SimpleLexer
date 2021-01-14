package lexer.impl;

import lexer.Leaf;

public class RuleBranchToLeaf implements Rule {

    final Rule wrapped;

    RuleBranchToLeaf(Rule wrapped) { this.wrapped = wrapped; }

    @Override
    public Object createInitialState(Context ctx) {
        return wrapped.createInitialState(ctx);
    }

    @Override
    public boolean nextState(Context ctx, Object state) {
        return wrapped.nextState(ctx, state);
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
        MatchedContent mc = wrapped.tryToMatch(ctx, state);
        if (mc!=null && mc.captured!=null) {
            mc.captured = new Leaf(mc.captured.name, mc.captured.stringValue());
        }
        return mc;
    }
    
}
