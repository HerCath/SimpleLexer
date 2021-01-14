package lexer.impl;

import lexer.Leaf;

public class RuleString extends Capturable implements StateLessRule {

    final CharSequence cSeq;

    public RuleString(boolean capture, CharSequence cSeq) {
        super(capture);
        this.cSeq = cSeq;
    }

    @Override
    public MatchedContent tryToMatch(Context ctx, Object state) {
        final int pos = ctx.pos;
        int i=0;
        while (i<cSeq.length()) {
            if (ctx.is(cSeq.charAt(i))) {
                i++;
                ctx.pos++;
            }
        }
        if (i==cSeq.length()) {
            return new MatchedContent(capture?new Leaf("string", cSeq):null);
        }
        ctx.pos = pos;
        return null;
    }
    
}
