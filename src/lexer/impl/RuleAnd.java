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
		            MatchedContent subMatch;
		            try {
		            	subMatch = subRule.tryToMatch(ctx, subStates.get(i));
		            } catch (RuntimeException e) {
		            	System.err.println("RuleAnd Crashed with i="+i+", ctx.pos="+ctx.pos+", ct.remaining="+ctx.remaining()+" on rule "+this);
		            	throw e;
		            }
		            if (subMatch==null) {
		            	if (nextState(ctx, state)) continue TRY;
		                ctx.pos = pos;
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
