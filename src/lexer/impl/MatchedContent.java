package lexer.impl;

import lexer.Node;

/**
 * When a grammar matched some contents, it emit a <code>MatchedContent</code> object.
 * It emits <code>null</code> when it does not match. The captured content (if it was
 * to be captured) is stored within <code>captured</code>. A <code>null</code> is used
 * when matched content has to be discarded.
 */
public class MatchedContent {
	public Node captured;
    public MatchedContent(Node captured) { this.captured = captured; }
    public String toString() { return "MacthedContext{captured="+(captured!=null?captured.stringValue():"null")+"}"; }
}
