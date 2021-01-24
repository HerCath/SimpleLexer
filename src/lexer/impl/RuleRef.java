package lexer.impl;

import java.util.Map;

import lexer.Branch;
import lexer.Leaf;

public class RuleRef extends Capturable<State> {

    final String name;
    final Map<String, Rule> rules;
    Rule rule;

    public RuleRef(boolean capture, String name, Map<String, Rule> rules) {
        super(capture);
        this.name = name;
        this.rules = rules;
        rule = null;
    }

    @Override public State createState(Context ctx) {
    	if (rule == null)
    		rule = rules.get(name);
    	if (rule == null)
    		throw new RuntimeException("Got a reference to an unknown rule \""+name+"\". TODO : give the closest rule name to help debugging rules.");
    	return rule.createState(ctx);
    }

    @Override public MatchedContent match(Context ctx, State state) {
    	MatchedContent mc = null;
    	ctx.enter(this);
    	try {
    		Rule rule = rules.get(name); 
	        mc = rule.match(ctx, state);
	        if (mc!=null) {
	        	if (!capture) {
	        		// maybe it was already null
	        		mc.captured = null;
	        	} else if (mc.captured==null) {
	        		// special case : some rule may not capture but may be captured themselves
	        		// in this case, they must forge a result. the Utils.toLexer(Node)
	        		// implementation took care of making the RuleBranchToLeaf the last
	        		// Rule wrapper used when translating/regsitering rules, so that
	        		// RuleRef can use instanceof to detected if the forged result should be
	        		// a Leaf or a Branch
	        		if (rule instanceof RuleBranchToLeaf) {
	        			mc.captured = new Leaf(name, null);
	        		} else {
	        			mc.captured = new Branch(name);
	        		}
	        	}
	        }
	        return mc;
    	} finally {
    		ctx.leave(this, mc);
    	}
    }
    
    public String toString() { return capture ? "+"+name : name; }
    
}