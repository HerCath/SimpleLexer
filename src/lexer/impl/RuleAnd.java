package lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import lexer.Branch;
import lexer.Node;
import lexer.impl.RuleAnd.RuleAndState;

public class RuleAnd extends Rule<RuleAndState> {

    final List<Rule> subRules;

    public RuleAnd() { subRules = new ArrayList<>(); }
    public RuleAnd(List<Rule> subRules) { this.subRules = subRules; }
    public RuleAnd(Rule...subRules) { this(Arrays.asList(subRules)); }
    
    private static <T> T getLast(List<T> list) { return list.isEmpty() ? null : list.get(list.size()-1); }
    private static <T> T removeLast(List<T> list) { return list.isEmpty() ? null : list.remove(list.size()-1); }

    class RuleAndState extends State {
    	
    	List<State> subStates;
    	List<MatchedContent> subMatches;
    	
    	RuleAndState(Context ctx) {
    		super(ctx);
	    	subStates = new ArrayList<>(subRules.size());
	    	subMatches = new ArrayList<>(subRules.size());
	    	subStates.add(subRules.get(0).createState(ctx));
	    	// the initial state as 1 sub state, 0 match out of the N sub rules (N>=1)
    	}
    	
    	public String toString() {
    		return "AND{"+subMatches+", "+subStates+"}";
    	}
    	
    }
    
    @Override public RuleAndState createState(Context ctx) {
    	return new RuleAndState(ctx);
    }

    @Override
    public MatchedContent match(Context ctx, RuleAndState state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		final int pos = ctx.pos;
    		while (state.subStates.size()>0) {
    			///////////////////////////////////////////////////////////////
    			// safety net. the context may have been altered during the  //
    			// previous loop iteration, we just restore it               //
    			///////////////////////////////////////////////////////////////
    			ctx.pos = pos;
    			
    			if (state.subMatches.size()==state.subStates.size()) {
    				if (state.subStates.size()<subRules.size()) {
    					///////////////////////////////////////////////////////
    					// we have less currently sub states than we have    //
    					// rules. we need to create the next missing state   //
    					///////////////////////////////////////////////////////

    					///////////////////////////////////////////////////////
    					// get the rule to inject                            //
    					///////////////////////////////////////////////////////
    					int subRuleToInjectIdx = state.subStates.size();
    					Rule subRuleToInject = subRules.get(subRuleToInjectIdx);
    					
    					// setup the context are the correct position
    					MatchedContent lastKnownSubMatch = getLast(state.subMatches); 
    					if (lastKnownSubMatch!=null) ctx.pos = lastKnownSubMatch.to;
    					
    					// then create the state object for this rule within the correctly adjusted context
    					State subRuleToInjectState = subRuleToInject.createState(ctx);
    					state.subStates.add(subRuleToInjectState);
    					
    					continue;
    				} else {
    					// we have as many matches as states as sub rules,
    					// this means we have a set of consecutive rule matches
    					// this means this AND-rule has a match
    					// we have to emit the match result and also we have to consume it
    					// so next call to AND.macth will generate a new match and not just
    					// repeat it again and again
    					Branch match = new Branch("*");
    					for (MatchedContent subMatch:state.subMatches) {
    						if (subMatch.captured!=null) {
    							if ("*".equals(subMatch.captured.name)) {
    								try {
    									match.childs.addAll(((Branch)subMatch.captured).childs);
    								} catch (ClassCastException cce) {
    									throw new RuntimeException("Got a leaf with a * name: "+subMatch.captured);
    								}
    							} else {
    								match.childs.add(subMatch.captured);
    							}
    						}
    						// else it was a sub-match but discarded sub-result
    					}
    					MatchedContent lastSubMatch = removeLast(state.subMatches);
    					ctx.pos = lastSubMatch.to;
    					return mc = new MatchedContent(pos, match, ctx.pos);
    				}
    			} else {
    				///////////////////////////////////////////////////////////
    				// we got 1 more state than we have rule, this means we  //
    				// have to check if the rule with that state has a match //
    				///////////////////////////////////////////////////////////
    				
    				// we adjust the context if needed
    				MatchedContent lastKnownSubMatch = getLast(state.subMatches);
    				final int posBefore = ctx.pos = lastKnownSubMatch!=null ? lastKnownSubMatch.to : pos;
    				
    				// get everyone in place for the big final test
    				Rule subRuleToTry = subRules.get(state.subMatches.size());
    				State subRuleToTryState = getLast(state.subStates);
    				
    				// then the test : is it a match or not ?
    				MatchedContent subMatch = subRuleToTry.match(ctx, subRuleToTryState);

    				///////////////////////////////////////////////////////////
    				// if it matches, we just store the match and loop again //
    				///////////////////////////////////////////////////////////
    				if (subMatch!=null) {
    					state.subMatches.add(subMatch);
    					continue;
    				}

    				///////////////////////////////////////////////////////////
    				// if it does not match, we have to go back to the       //
    				// previous subRule with a match, invalidate its match   //
    				// and try agin from there                               //
    				///////////////////////////////////////////////////////////
    				else {
    					removeLast(state.subStates); // out of loop condition : if we remove the last state, that means there is not more any combination possible to try
    					removeLast(state.subMatches);
    					continue;
    				}
    			}
    		}
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
