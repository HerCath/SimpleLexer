package lexer.impl;

import java.util.NoSuchElementException;

public abstract class States {
	private final int createAtPos;
	
	States(Context ctx) { createAtPos=ctx.pos; }
	
	public boolean hasNext(Context ctx) {
		if (ctx.pos != createAtPos) throw new RuntimeException("Got a state that was created when parsing at #"+createAtPos+" but it was re-invoked when at #"+ctx.pos);
		return _hasNext(ctx);
	}
	public void next(Context ctx) {
		if (!hasNext(ctx)) throw new NoSuchElementException("next was called while there was no more possible state variation left to explore.");
		_next(ctx);
	}
	
	protected boolean _hasNext(Context ctx) { throw new RuntimeException("Not implemented"); }
	protected void _next(Context ctx) { throw new RuntimeException("Not implemented"); }
}
