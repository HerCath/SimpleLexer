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
    
    private class StateLessSpecialCase {
    	int curr;
    }

    private Object createInitialState(Context ctx, int size) {
//    	System.out.println("Creating initial cardinalty state of size "+size);
        List<Object> subStates = new ArrayList<>(size);
        while (size-->0) {
            subStates.add(subRule.createInitialState(ctx));
        }
        return subStates;
    }

    @Override public Object createInitialState(Context ctx) {
        // BEWARE : Rule may match content with a size of 0 but such behaviour is dangerous
    	Object subState = subRule.createInitialState(ctx);
    	if (subState == null) {
    		System.out.println("createInitialState : Detected special cardinality stateless case for "+this);
    		StateLessSpecialCase state = new StateLessSpecialCase();
    		state.curr = -1;
    		return state;
    	} else {
    		System.out.println("createInitialState : not a special cardinality stateless case for "+this);
	        if (greedy) {
	            return createInitialState(ctx, min);
	        } else {
	            // rules should at least capture one char, so we could not eat more patterns than what is remaining. we use this property to not start with a max value being Integer.MAX_VALUE
	        	int minSize = subRule.minSize();
	        	int maxOcc = (ctx.remaining()+minSize-1)/minSize;
	        	System.out.println("Our sub rule has a min size of "+minSize+" and there are "+ctx.remaining()+" chars left so this make for up to "+maxOcc);
	            return createInitialState(ctx, Math.min(maxOcc, max));
	        }
    	}
    }

    @Override public boolean nextState(Context ctx, Object state) {
        
    	// idea : for fast-fail, remember which ctx.pos we reached with which states, so that we may know that some states already fail when applied at the given ctx.pos
    	
    	if (state instanceof StateLessSpecialCase) {
    		StateLessSpecialCase _state = (StateLessSpecialCase) state;
    		if (_state.curr==-2) return false; // the 1st try main have deduced it will never match anyway, so it setted this special -2 value and returned null
    		if (greedy) {
    			_state.curr++;
    			return _state.curr<=max;
    		}
			_state.curr--;
			return _state.curr>=min;
    	}
    	
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
        final int pos = ctx.pos;
        List<Node> matches = new ArrayList<>();
        if (state instanceof StateLessSpecialCase) {
        	StateLessSpecialCase _state = (StateLessSpecialCase) state;
        	System.out.println("tryToMatch : special cardinality stateless case, state.curr="+_state.curr);
        	if (_state.curr == -1) {
        		// special stateless optim, the decision about cardinalities is made on 1st try
        		int nbMatches = 0;
        		while (true) {
        			int j = ctx.pos;
        			MatchedContent mc = subRule.tryToMatch(ctx, null); // stateless rules have a null state
        			if (mc==null) break;
        			if (j==ctx.pos) throw new RuntimeException("Got a stateless rule that matched some content without moving forward. This is a bug in this stateless rule that breaks cardinality handling by generating an infinite loop.");
        			if (mc.captured!=null) {
    	                if (mc.captured.name.equals("*")) {
    	                	Branch b = (Branch) mc.captured;
    	                	matches.addAll(b.childs);
    	                } else {
    	                    matches.add(mc.captured);
    	                }
    	            }
        			nbMatches++;
        			if (greedy && nbMatches==min) break;
        			if (nbMatches==max) break;
        		}
        		if (nbMatches<min) {
        			_state.curr = -2; // will never match special feedback value, to be recognized by nextState
        			System.out.println("tryToMatch : special cardinality stateless case, returns with state.curr="+_state.curr);
        			ctx.pos = pos;
        			return null;
        		}
        		_state.curr = nbMatches; // setup the best deduced 1st value
    			System.out.println("tryToMatch : special cardinality stateless case, returns with state.curr="+_state.curr);
        	} else {
        		for (int i=0; i<_state.curr; i++) {
        			MatchedContent mc = subRule.tryToMatch(ctx, null); // stateless rules have a null state
        			if (mc==null) {
        				ctx.pos = pos;
        				return null;
        			}
        			if (mc.captured!=null) {
    	                if (mc.captured.name.equals("*")) {
    	                	Branch b = (Branch) mc.captured;
    	                	matches.addAll(b.childs);
    	                } else {
    	                    matches.add(mc.captured);
    	                }
    	            }
        		}
        	}
        } else {
        	int nbMatch = 0;
	        List<Object> subStates = (List<Object>) state;
	        for (int i=0, l=subStates.size(); i<l; i++) {
	            MatchedContent mc = subRule.tryToMatch(ctx, subStates.get(i));
	            if (mc==null) {
	            	if (nbMatch>=min) break; // we tried to reach subStates.size() matches but with statefull a subRule this is only a best effort
	                ctx.pos = pos;
	                return null;
	            }
	            nbMatch++;
	            if (mc.captured!=null) {
	                if (mc.captured.name.equals("*")) {
	                	Branch b = (Branch) mc.captured;
	                	matches.addAll(b.childs);
	                } else {
	                    matches.add(mc.captured);
	                }
	            }
	        }
        }
        return new MatchedContent(new Branch("*", matches));
    }
    
    @Override
    public int minSize() {
    	if (min==0) return 0;
    	return min*subRule.minSize();
    } 

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
