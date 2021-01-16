package lexer.impl;

import java.util.Iterator;

import lexer.Leaf;

public class RuleBranchToLeaf implements Rule {

    private final Rule subRule;

    public RuleBranchToLeaf(Rule subRule) { this.subRule = subRule; }

    @Override public Iterator<Object> getStates(Context ctx) { return subRule.getStates(ctx); }

    @Override public MatchedContent match(Context ctx, Iterator<Object> states) {
        if (!states.hasNext()) return null;
        MatchedContent mc = subRule.match(ctx, states);
        if (mc!=null && mc.captured!=null) {
            mc.captured = new Leaf(mc.captured.name, mc.captured.stringValue());
        }
        return mc;
    }
    
    @Override public String toString() { return "flatten "+subRule; }
    
}
