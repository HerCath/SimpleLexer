package lexer.impl;

import lexer.Leaf;

public class RuleBranchToLeaf implements Rule {

    private final Rule subRule;

    public RuleBranchToLeaf(Rule wrapped) { this.subRule = wrapped; }

    @Override
    public Object createInitialState(Context ctx) {
        return subRule.createInitialState(ctx);
    }

    @Override
    public boolean nextState(Context ctx, Object state) {
        return subRule.nextState(ctx, state);
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
        MatchedContent mc = subRule.tryToMatch(ctx, state);
        if (mc!=null && mc.captured!=null) {
            mc.captured = new Leaf(mc.captured.name, mc.captured.stringValue());
        }
        return mc;
    }
    
//    @Override public int minSize(Rule rootRule) { return subRule.minSize(rootRule); }
    
    public String toString() { return "flatten "+subRule; }
    
}
