package lexer.impl;

/**
 * Rules are stateless piece of logic that try to match some content. The 3
 * methods will be called in that order #1 createInitialState #2 tryToMatch #3
 * nextState, and if true got back to #2
 */
public interface Rule<STATE extends State> {
    /**
     * Creates and returns the states Iterator to use when matching.
     */
    STATE createState(Context ctx);
    /**
     * Returns next preferred match. Returns null once no more matches.
     */
    MatchedContent match(Context ctx, STATE state);
}
