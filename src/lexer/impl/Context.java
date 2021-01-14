package lexer.impl;

public class Context {
    final CharSequence cSeq;
    int pos;
    public Context(CharSequence cSeq) { this.cSeq = cSeq; }
    int remaining() { return cSeq.length()-pos; }
    char peek() { return cSeq.charAt(pos); }
    char poll() { return cSeq.charAt(pos++); }
    void skip(CharClass cClass) { while (is(cClass)) pos++; }
	boolean is(char c) { return !atEnd() && peek()==c; }
    boolean is(CharClass cClass) { return !atEnd() && cClass.contains(peek()); }
    boolean atEnd() { return pos>=cSeq.length(); }
}