package lexer.impl;

import java.util.NoSuchElementException;

interface StateLessRule extends Rule {
    @Override public default States createStates(Context ctx) {
        return new States(ctx) {
        	boolean hasState = true;
        	protected boolean _hasNext(Context ctx) { return hasState; }
        	protected void _next(Context ctx) {
        		if (hasState) {
        			hasState = false;
//        			System.out.println("===================");
//        			Thread.dumpStack();
        			return;
        		}
        		throw new NoSuchElementException("No next state.");
        	}
        	public String toString() { return "OneShotStates{hasState="+hasState+"}"; }
        };
    }
}
