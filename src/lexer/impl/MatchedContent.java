package lexer.impl;

import lexer.Node;

/**
 * When a grammar matches some contents, it emit a <code>MatchedContent</code>
 * object. It emits <code>null</code> when it does not match. The captured
 * content (if it was to be captured) is stored within <code>captured</code>. A
 * <code>null</code> is used when matched content has to be discarded. The
 * <code>from</code> and <code>to</code> fields help keep track of what portion
 * of the parsed content was consumed to produce this match result.
 */
public class MatchedContent {

	/**
	 * The capture content. It will always fit inside the [from, to[ char range from
	 * the matched char sequence. It might be smaller. It can be either a Branch, a
	 * Leaf. null is used when a rule does match, but does not capture it.
	 */
	public Node captured;
	
	/**
	 * The included start char index this matched result spans.
	 */
	public int from;
	
	/**
	 * The excluded end char index this matched result spans.
	 */
	public int to;

	public MatchedContent(int from, Node captured, int to) {
		this.captured = captured;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString() {
		return "MacthedContext{range=["+from+", "+to+"[, captured=" + (captured != null ? "<"+captured.name+">"+captured.stringValue()+"</"+captured.name+">" : "null") + "}";
	}
}
