package lexer.impl;

import java.util.ArrayList;
import java.util.List;

import lexer.Branch;
import lexer.Node;

public class RuleCardinality implements Rule {

    public final int min;
    public final int max;
    public final boolean greedy;
    public final Rule subRule;

    private Object createInitialState(Context ctx, int size) {
        List<Object> subStates = new ArrayList<>(size);
        while (size-->0) {
            subStates.add(subRule.createInitialState(ctx));
        }
        return subStates;
    }

    @Override public Object createInitialState(Context ctx) {
        if (greedy) {
            return createInitialState(ctx, min);
        } else {
            // rules should at least capture one char, so we could not eat more patterns than what is remaining. we use this property to not start with a max value being Integer.MAX_VALUE
        	int minSize = 1;//subRule.minSize();
        	int maxOcc = (ctx.remaining()+minSize-1)/minSize;
        	System.out.println("Our sub rule has a min size of "+minSize+" and there are "+ctx.remaining()+" chars left so this make for up to "+maxOcc);
            return createInitialState(ctx, Math.min(maxOcc, max));
        }
    }

    @Override public boolean nextState(Context ctx, Object state) {
        
    	// idea : for fast-fail, remember which ctx.pos we reached with which states, so that we may know that some states already fail when applied at the given ctx.pos
    	
        List<Object> subStates = (List<Object>) state;
        for (int i=0, l=subStates.size(); i<l; i++) {
            if (subRule.nextState(ctx, subStates.get(i))) {
                for (int j=0; j<i; j++) {
                    subStates.set(j, subRule.createInitialState(ctx));
                }
                return true;
            }
        }
        System.out.println("state of size "+subStates.size()+" did not match. Going to change its size");
        // if we reached this line, this means we have exhausted all permutations of state from the subRule for current cardinality
        // we then adjust the cardinality according to min/max/greedy
        int curr = subStates.size();
        if (greedy) {
            curr++;
            if (curr>Math.min(ctx.remaining(), max)) return false;
        } else {
            curr--;
            if (curr<min) return false;
        }
        // we are still in between min/max, so let's just forge the new state
        subStates.clear();
        while (curr-->0) {
            subStates.add(subRule.createInitialState(ctx));
        }
        return true;
    }

    @Override public MatchedContent tryToMatch(Context ctx, Object state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
	        final int pos = ctx.pos;
	        TRY : while (true) {
		        List<Node> matches = new ArrayList<>();
	        	int nbMatch = 0;
		        List<Object> subStates = (List<Object>) state;
		        for (int i=0, l=subStates.size(); i<l; i++) {
		            MatchedContent subMatch = subRule.tryToMatch(ctx, subStates.get(i));
		            if (subMatch==null) {
		            	if (nbMatch>=min) break; // we tried to reach subStates.size() matches but with statefull a subRule this is only a best effort
		            	if (nextState(ctx, state)) continue TRY;
		                ctx.pos = pos;
		                return null;
		            }
		            nbMatch++;
		            if (subMatch.captured!=null) {
		                if (subMatch.captured.name.equals("*")) {
		                	Branch b = (Branch) subMatch.captured;
		                	matches.addAll(b.childs);
		                } else {
		                    matches.add(subMatch.captured);
		                }
		            }
		        }
		        return mc=new MatchedContent(new Branch("*", matches));
	        }
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
//    @Override public int minSize(Rule rootRule) {
//    	if (min==0) return 0;
//    	return min*subRule.minSize(rootRule);
//    } 

    public RuleCardinality(int min, int max, boolean greedy, Rule subRule) {
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
