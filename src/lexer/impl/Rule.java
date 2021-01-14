package lexer.impl;

/**
 * Rules are stateless piece of logic that try to match some content.
 * The 3 methods will be called in that order
 * #1 createInitialState
 * #2 tryToMatch
 * #3 nextState, and if true got back to #2
 */
public interface Rule {
    /**
     * Creates and returns the first state to use when matching.
     */
    Object createInitialState(Context ctx);
    /**
     * Returns if a next state exist, mutating <code>state</code> if any.
     */
    boolean nextState(Context ctx, Object state);
    /**
     * Match or not some content in the current context within the given state.
     */
    MatchedContent tryToMatch(Context ctx, Object state);
    /**
     * Returns the smallest size this rule may match. Used to speed-up some treatments with variable cardinalities;   
     * @return
     */
    int minSize();
    // idea : also adds a mustContains method returning a CharClass which can also bo used for fast lookups
    // idea2 : same goes with mustNotContains
}
