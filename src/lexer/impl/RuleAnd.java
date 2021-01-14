package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lexer.Branch;
import lexer.Node;

public class RuleAnd implements Rule {

    final List<Rule> subRules;

    public RuleAnd() { subRules = new ArrayList<>(); }
    public RuleAnd(List<Rule> subRules) { this.subRules = subRules; }
    public RuleAnd(Rule...subRules) { this(Arrays.asList(subRules)); }

    @Override
    public Object createInitialState(Context ctx) {
        List<Object> subStates = new ArrayList<>(subRules.size());
        for (int i=0, l=subRules.size(); i<l; i++) {
            subStates.add(subRules.get(i).createInitialState(ctx));
        }
        return subStates;
    }

    @Override
    public boolean nextState(Context ctx, Object state) {
        List<Object> subStates = (List<Object>) state;
        for (int i=0, l=subRules.size(); i<l; i++) {
            if (subRules.get(i).nextState(ctx, subStates.get(i))) {
                for (int j=0; j<i; j++) {
                    subStates.set(j, subRules.get(j).createInitialState(ctx));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
        final int pos = ctx.pos;
        List<Object> subStates = (List<Object>) state;
        List<Node> subMatches = new ArrayList<>(subRules.size());
        for (int i=0, l=subRules.size(); i<l; i++) {
            Rule subRule = subRules.get(i);
            MatchedContent subMatch = subRule.tryToMatch(ctx, subStates.get(i));
            if (subMatch==null) {
                ctx.pos = pos;
                return null;
            }
            if (subMatch.captured!=null) {
                if (subMatch.captured.name.equals("*")) {
                    Branch b = (Branch) subMatch.captured;
                    for (int bi=0, bl=b.childs.size(); bi<bl; bi++) {
                        subMatches.add(b.childs.get(bi));
                    }
                } else {
                    subMatches.add(subMatch.captured);
                }
            }
        }
        if (subMatches.size()>0) {
            return new MatchedContent(new Branch("*", subMatches));
        }
        return new MatchedContent(new Branch("*"));
    }
    
}
