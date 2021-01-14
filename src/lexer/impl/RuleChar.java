package lexer.impl;

import lexer.Leaf;

public class RuleChar extends Capturable implements StateLessRule {

    final CharClass cClass;

    public RuleChar(boolean capture, CharClass cClass) {
        super(capture);
        this.cClass = cClass;
    }

    @Override public MatchedContent tryToMatch(Context ctx, Object state) {
        if (ctx.is(cClass)) {
            char c = ctx.poll(); // poll to consume. needed even when not capturing
            return new MatchedContent(capture ? new Leaf("char", c) : null);
        }
        return null;
    }
    
}
