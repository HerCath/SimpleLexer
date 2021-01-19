package lexer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lexer.Branch;
import lexer.Node;
import lexer.impl.RuleCardinality.RuleCardinalityState;;

public class RuleCardinality extends Rule<RuleCardinalityState> {

    public final int min;
    public final int max;
    public final boolean greedy;
    public final Rule subRule;
    
    class RuleCardinalityState extends State {
    	boolean zeroEmitted = false; // the zero cardinality is a special case. Due to the way we handle consumming already emitted result, we have to other meaning of remerbering that we emitted the 0 cardinality matching result except by using this flag
    	boolean gotAMatch = false; // flag used when resoving greedy mode. used to keep track if we got at least one match for the currMin cardinality. Used to know if it is worth trying currMin+1 when nothing could be tried at currMin
    	int currMin;
    	int currMax;
    	List<State> subStates;
    	List<MatchedContent> subMatches;
		@Override
		public String toString() {
			return "RuleCardinalityState [zeroEmitted=" + zeroEmitted + ", currMin=" + currMin + ", currMax=" + currMax
					+ ", subStates=" + subStates + ", subMatches=" + subMatches + "]";
		}
		RuleCardinalityState(Context ctx) {
			super(ctx);
			currMin = min;
			currMax = greedy ? currMin : max;
			int bestSize = max<Integer.MAX_VALUE ? max : 2*(min+1);
			if (bestSize>100) bestSize=100;
			subStates = new ArrayList<>(bestSize);
			subMatches = new ArrayList<>(bestSize);
			if (!greedy) subStates.add(subRule.createState(ctx));
		}
		MatchedContent buildMatchedContent(int pos) {
			Branch result = new Branch("*");
			int to = pos;
			for (MatchedContent subMatch:subMatches) {
				to = subMatch.to;
				if (subMatch.captured!=null) {
					if (subMatch.captured.name.equals("*")) {
						result.childs.addAll(((Branch)subMatch.captured).childs);
					} else {
						result.childs.add(subMatch.captured);
					}
				}
			}
			return new MatchedContent(pos, result, to);
		}
	}
    
    private static <T> T getLast(List<T> list) { return list.isEmpty() ? null : list.get(list.size()-1); }
    private static <T> T removeLast(List<T> list) { return list.isEmpty() ? null : list.remove(list.size()-1); }

    @Override public RuleCardinalityState createState(Context ctx) {
        return new RuleCardinalityState(ctx);
    }

    @Override public MatchedContent match(Context ctx, RuleCardinalityState state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		final int pos = ctx.pos;
    		while (true) {
    			ctx.pos = pos;
    			
    			if (state.subStates.isEmpty()) {
    				if (greedy) {
						if (state.gotAMatch) {
							state.currMin = ++state.currMax;
							state.gotAMatch = false;
							state.subStates.add(subRule.createState(ctx));
						} else if (state.currMin>0) {
							return null;
						}
    					if (state.currMin>max) return null;
    				} else {
    					if (state.zeroEmitted || min>0) return null;
    				}
    			}
				
				///////////////////////////////////////////////////////////
				// do we have enough sub-matches to emit a result ?      //
				///////////////////////////////////////////////////////////
				if (state.subMatches.size()==state.currMax) {
					
					///////////////////////////////////////////////////////
					// yes, we have the maximum number of sub matches    //
					// possible, so we generate the cardinality match    //
					///////////////////////////////////////////////////////
					state.zeroEmitted = state.subMatches.isEmpty(); 
					state.gotAMatch = true;
					mc = state.buildMatchedContent(pos);
					ctx.pos = mc.to;
					removeLast(state.subMatches); // removing last sub match will firt next match(...) call to generate a different match and not just repeat this one again and again
					return mc;
				
				} else {
					///////////////////////////////////////////////////////
					// no, we still have some room to try another sub    //
					// match                                             //
					///////////////////////////////////////////////////////
					
					MatchedContent lastMatchedContent = getLast(state.subMatches);
					if (lastMatchedContent!=null) ctx.pos = lastMatchedContent.to;
					
					///////////////////////////////////////////////////////
					// we have 2 cases here, either we have the same     //
					// amout of sub matches than we have sub states, or  //
					// we have 1 sub match less than sub states          //
					///////////////////////////////////////////////////////
					State nextCardState; 
					if (state.subMatches.size()==state.subStates.size()) {
    					state.subStates.add(nextCardState = subRule.createState(ctx));
					} else {
						nextCardState = getLast(state.subStates);
					}
					
					///////////////////////////////////////////////////////
					// Beyond this line, we have less sub matches than   //
					// the expected cardinality and also that we have    //
					// exactly 1 more sub-state than sub-match.          //
					// We may have as many sub states than the target    //
					// cardinality                                       //
					///////////////////////////////////////////////////////
					MatchedContent nextCardMatch = subRule.match(ctx, nextCardState);
					if (nextCardMatch!=null && nextCardMatch.from!=nextCardMatch.to) { // we do not count matches with 0 chars as being real sub-match, otherwise this would generate infinite loop uppon unbounded-max cardinalities 
						state.subMatches.add(nextCardMatch);
					} else {
						// ok, so we have a miss, but maybe the cardinality itself is a match
						if (state.subMatches.size()>=state.currMin) {
							state.zeroEmitted = state.subMatches.isEmpty();
							mc = state.buildMatchedContent(pos);
							state.gotAMatch = true;
						}
						
						// we still have to rollback up to the rule just prior to this last
						// missed tried one
						removeLast(state.subStates);
						removeLast(state.subMatches);
						if (mc!=null) {
							ctx.pos = mc.to;
							return mc;
						}
					}
				}
    		}
    	} finally {
    		ctx.leave(this, mc);
    	}
    }

    public RuleCardinality(int min, int max, boolean greedy, Rule<?> subRule) {
        this.min = min;
        this.max = max;
        this.greedy = greedy;
        this.subRule = subRule;
    }
    
    public String toString() {
    	String card;
    	if (min==0) {
    		if (max==1) card = "?";
    		else if (max==Integer.MAX_VALUE) card = "*";
    		else card = "{"+min+","+max+"}";
    	} else if (min==1) {
    		if (max==Integer.MAX_VALUE) card = "+";
    		else card = "{"+min+","+max+"}";
    	} else if (max==Integer.MAX_VALUE) {
    		card = "{"+min+",}";
    	} else {
    		card = "{"+min+","+max+"}";
    	}
    	return subRule+card+(greedy?"?":"");
    }
    
}
