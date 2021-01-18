package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.sun.org.apache.xerces.internal.impl.xs.SubstitutionGroupHandler;

import lexer.Branch;
import lexer.Node;
import sun.nio.cs.ext.MacThai;

public class RuleAnd implements Rule {

    final List<Rule> subRules;

    public RuleAnd() { subRules = new ArrayList<>(); }
    public RuleAnd(List<Rule> subRules) { this.subRules = subRules; }
    public RuleAnd(Rule...subRules) { this(Arrays.asList(subRules)); }
    
    static class SubRuleMatch extends MatchedContent {
    	final int posBefore; // we store the position before it matched so we can roll it back
    	final int posAfter; // we store the position after is matched so we can simulate the Context state after subRule.match(...) was called
    	SubRuleMatch(int posBefore, MatchedContent mc, int posAfter) {
    		super(mc.captured);
    		this.posBefore = posBefore;
    		this.posAfter = posAfter;
    	}
    	public String _toString() {
    		return "\n\t\tSubRuleMatch: from "+posBefore+" to "+posAfter+", matched "+captured;
    	}
    }
    
    private static <T> T getLast(List<T> list) { return list.isEmpty() ? null : list.get(list.size()-1); }
    private static <T> T removeLast(List<T> list) { return list.remove(list.size()-1); }

    private class RuleAndStates extends State {
    	
    	List<State> subStates;
    	List<SubRuleMatch> subMatches;
    	
    	RuleAndStates(Context ctx) {
    		super(ctx);
	    	subStates = new ArrayList<>(subRules.size());
	    	subStates.add(subRules.get(0).createStates(ctx));
	    	subMatches = new ArrayList<>(subRules.size());
	    	// the initial state as 1 sub state, 0 match out of the N sub rules (N>=1)
    	}
    	
    	public String toString() {
    		return "AND{"+subMatches+", "+subStates+"}";
    	}
    	
		@Override protected boolean _hasNext(Context ctx) {
			return subStates.get(0).hasNext(ctx);
		}

		@Override protected void _next(Context ctx) {
			final int pos = ctx.pos;
			while (true) {
				try {
					if (subStates.size()==subMatches.size())
						removeLast(subMatches); // forget about last match if any
					State lastSubRuleStates = getLast(subStates);
					SubRuleMatch beforeLastSubRuleMatch = getLast(subMatches);
					if (beforeLastSubRuleMatch!=null)
						ctx.pos = beforeLastSubRuleMatch.posAfter; // adjust context because we might try to check the hasNext of a in-the-middle rule, so we want to test this while the context is a the right index
					if (lastSubRuleStates.hasNext(ctx)) {
						lastSubRuleStates.next(ctx);
						return;
					}
					if (beforeLastSubRuleMatch!=null)
						removeLast(subStates);
					else
						throw new NoSuchElementException();
				} finally {
					ctx.pos = pos;
				}
			}
		}
    	
    }
    
    @Override
    public State createStates(Context ctx) {
    	return new RuleAndStates(ctx);
    }

    @Override
    public MatchedContent match(Context ctx, State states) {
    	if (!states.hasNext(ctx)) return null;
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		final int pos = ctx.pos;
    		final RuleAndStates _states = (RuleAndStates) states;
    		while (states.hasNext(ctx)) {
    			if (_states.subMatches.size()<_states.subStates.size()) {
    				// 1st, we try to match the rule that has this last sub state
    				// if it matched, we store the match, then either emit the full resul if it it was the last sub rule
    				// it it did not matched, we go back to the previous rule and move its state a step further
    				int subRuleIdx = _states.subStates.size()-1;
    				Rule subRule = subRules.get(subRuleIdx);
    				State subRuleStates = _states.subStates.get(subRuleIdx);
    				if (!_states.subMatches.isEmpty()) {
    					ctx.pos = getLast(_states.subMatches).posAfter;
    				}
    				int posBefore = ctx.pos;
    				MatchedContent subMatch = subRule.match(ctx, subRuleStates);
    				if (subMatch!=null) {
    					// got a match, store it
    					_states.subMatches.add(new SubRuleMatch(posBefore, subMatch, ctx.pos));
    				} else {
	    				// no match. Since each rule tries its best to match, that means this last explored subRule can never match from its current context location
	    				// we have to rollback this rule, go to the previous one, enforce this previous one into its next state, and try again from there
	    				removeLast(_states.subStates);
	    				if (_states.subMatches.isEmpty()) {
	    					ctx.pos = pos;
	    					return null;
	    				}
	    				states.next(ctx);
	    				continue;
    				}
    			}
    			System.out.println("RuleAnd, subMatches = "+_states.subMatches+", subMatches.size == "+_states.subMatches.size()+", subStates.size == "+_states.subStates.size());
    			// we have as many matches has currently followed states
    			// but we may still have less sub states than we have sub rules which means we are only partially resolving all the ANDed rules
    			if (_states.subStates.size()<subRules.size()) {
    				ctx.pos = getLast(_states.subMatches).posAfter;
    				_states.subStates.add(subRules.get(_states.subStates.size()).createStates(ctx));
    				ctx.pos = pos;
    				continue;
    			}
    			// beyond this line, we have as many matches as many sub states as many sub rules
    			Branch b = new Branch("*");
    			for (SubRuleMatch subMatch:_states.subMatches) {
    				if (subMatch.captured!=null)
    					b.childs.add(subMatch.captured);
    			}
    			return mc=new MatchedContent(b);
    		}
    		System.out.println("RuleAnd has no more states : "+states);
    		ctx.pos = pos;
    		return null;
    	} finally {
    		ctx.leave(this, mc);
		}
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("(").append(subRules.get(0)).append(")");
    	for (int i=1, l=subRules.size(); i<l; sb.append(" (").append(subRules.get(i++)).append(")") );
    	return sb.toString();
    }
}
