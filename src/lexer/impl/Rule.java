package lexer.impl;

/**
 * Rules are stateless piece of "matching" logic. The matching processus is
 * statefull. To be able to do statefull matching using stateless rule, a State
 * object is required. Such state objects are created on demand by the Rule
 * itself. Rules impact each other because any 2 consecutive rules starts follow
 * each other, so the 2nd one will not start from the same location if the 1st
 * one matches several times but spanning different subset of chars. The impact
 * between each rule is tracked within the provided context when matching.
 */
public abstract class Rule<STATE extends State> {
	
    /**
     * Creates and returns the states Iterator to use when matching.
     */
    public abstract STATE createState(Context ctx);
    
    /**
	 * Returns next preferred match. Returns null once no more matches. Mutate the
	 * state on the fly, leaving it in a state where the next call to match will
	 * generated the next prefered match result (or null if there is no more matches
	 * possible).
	 */
    public abstract MatchedContent match(Context ctx, STATE state);
    
    public boolean debug = false;
}
