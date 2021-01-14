package lexer.impl;

/**
 * Rules are stateless piece of logic that try to match some content.
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
}
