package lexer.impl;

public class SingleMatchState extends State {
	boolean hasBeenUsed = false;
	SingleMatchState(Context ctx) { super(ctx); }
}
