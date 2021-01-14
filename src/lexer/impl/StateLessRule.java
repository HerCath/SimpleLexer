package lexer.impl;

interface StateLessRule extends Rule {
    @Override public default Object createInitialState(Context ctx) { return null; }
    @Override public default boolean nextState(Context ctx, Object state) { return false; }
}
