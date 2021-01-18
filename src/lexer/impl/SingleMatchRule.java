package lexer.impl;

interface SingleMatchRule extends Rule<SingleMatchState> {
	
    @Override public default SingleMatchState createState(Context ctx) {
        return new SingleMatchState(ctx) {
        	public String toString() { return "SingleMatchState{hasBeenUsed="+hasBeenUsed+", rule="+SingleMatchRule.this+"}"; }
        };
    }
}
