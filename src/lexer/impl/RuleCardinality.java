package lexer.impl;

import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.bcel.internal.classfile.Node;

import javafx.scene.media.SubtitleTrack;

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
        // BEWARE : Rule may match content with a size of 0 but such behaviour is dangerous
        if (greedy) {
            return createInitialState(ctx, min);
        } else {
            // rules should at least capture one char, so we could not eat more patterns than what is remaining. we use this property to not start with a max value being Integer.MAX_VALUE
            return createInitialState(ctx, Math.min(ctx.remaining(), max));
        }
    }

    @Override public boolean nextState(Context ctx, Object state) {
        
        List<Object> subStates = (List<Object>) state;
        for (int i=0, l=subStates.size(); i<l; i++) {
            if (subRule.nextState(ctx, subStates.get(i))) {
                for (int j=0; j<i; j++) {
                    subStates.set(j, subRule.createInitialState(ctx));
                }
                return true;
            }
        }
        // if we reached this line, this means we have exhausted all permutations of state from the subRule for current cardinality
        // we then adjust the cardinality according to min/max/greedy
        int curr = subStates.size();
        if (greedy) {
            curr++;
            if (curr>max) return false;
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
        List<Object> subStates = (List<Object>) state;
        List<Node> matches = new ArrayList<>(subStates.size());
        for (int i=0, l=subStates.size(); i<l; i++) {
            MatchedContent mc = subRule.tryToMatch(ctx, subStates.get(i));
            if (mc==null) {
                ctx.pos = pos;
                return null;
            }
            if (mc.captured!=null) {
                if (mc.captured.name.equals("*")) {

                } else {
                    
                }
            }
        }
            return new Match
        }
        return null;
    }

    public RuleCardinality(int min, int max, boolean greedy, Rule subRule) {
        this.min = min;
        this.max = max;
        this.greedy = greedy;
        this.subRule = subRule;
    }
    
}
