package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lexer.Branch;
import lexer.Node;

public class RuleAnd implements Rule {

    final List<Rule> subRules;

    public RuleAnd() { subRules = new ArrayList<>(); }
    public RuleAnd(List<Rule> subRules) { this.subRules = subRules; }
    public RuleAnd(Rule...subRules) { this(Arrays.asList(subRules)); }

    @Override
    public Iterator<Object> getStates(Context ctx) {
        return Collections.emptyIterator();
    }

    @Override
    public MatchedContent match(Context ctx, Iterator<Object> state) {
		// TODO : re-implement
		return null;
		/*
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		final int pos = ctx.pos;
    		TRY : while (true) {
		        List<Object> subStates = (List<Object>) state;
		        List<Node> subMatches = new ArrayList<>(subRules.size());
		        for (int i=0, l=subRules.size(); i<l; i++) {
		        	int j = ctx.pos;
		            Rule subRule = subRules.get(i);
		            MatchedContent subMatch = subRule.tryToMatch(ctx, subStates.get(i));
		            if (subMatch==null) {
		                ctx.pos = pos;
		                if (nextState(ctx, state)) continue TRY;
		                return null;
		            }
		            if (subMatch.captured!=null) {
		                if (subMatch.captured.name.equals("*")) {
		                    Branch b = (Branch) subMatch.captured;
		                    subMatches.addAll(b.childs);
		                } else {
		                    subMatches.add(subMatch.captured);
		                }
		            }
		        }
		        return mc = new MatchedContent(new Branch("*", subMatches));
    		}
    	} finally {
    		ctx.leave(this, mc);
		}
		*/
    }
    
//    @Override
//    public int _minSize() {
//    	int sum = 0;
//    	for (int i=0, l=subRules.size(); i<l;sum+=subRules.get(i++).minSize(rootRule));
//    	return sum;
//    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("(").append(subRules.get(0)).append(")");
    	for (int i=1, l=subRules.size(); i<l; sb.append(" (").append(subRules.get(i++)).append(")") );
    	return sb.toString();
    }
}
