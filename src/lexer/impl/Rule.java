package lexer.impl;

import java.util.Iterator;

/**
 * Rules are stateless piece of logic that try to match some content. The 3
 * methods will be called in that order #1 createInitialState #2 tryToMatch #3
 * nextState, and if true got back to #2
 */
public interface Rule {
    /**
     * Creates and returns the states Iterator to use when matching.
     */
    States createStates(Context ctx);
    /**
     * Match or not some content consumming possible states until one matched or no more exists.
     */
    MatchedContent match(Context ctx, States states);
}
