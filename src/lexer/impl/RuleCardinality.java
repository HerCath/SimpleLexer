package lexer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lexer.Branch;
import lexer.Node;

public class RuleCardinality implements Rule {

    public final int min;
    public final int max;
    public final boolean greedy;
    public final Rule subRule;

    @Override public Iterator<Object> getStates(Context ctx) {
        // TODO : re-implement
        return Collections.emptyIterator();
    }

    @Override public MatchedContent match(Context ctx, Iterator<Object> state) {
        // TODO : re-implement
        return null;
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
