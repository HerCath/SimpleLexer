package lexer.impl;

import java.util.Iterator;

interface StateLessRule extends Rule {
    @Override public default Iterator<Object> getStates(Context ctx) {
        return new Iterator<Object>() {
            boolean full = true;
            @Override public boolean hasNext() { return full; }
            @Override public Object next() { full=false; return null; }
        };
    }
}
