package lexer.impl;

public class State {
	private final int createAtPos;
	State(Context ctx) { createAtPos=ctx.pos; }
}
