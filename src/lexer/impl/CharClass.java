package lexer.impl;

public interface CharClass {
	public boolean contains(char c);
    public static CharClass fromChar(char c) {
        return new CharClass() {
        	@Override public boolean contains(char a) { return a==c; }
        	@Override public String toString() {
        		return c=='\'' || c == '\\' ? "'\\"+c+"'" : "'"+c+"'";
        	}
        };
    }
    public static CharClass fromRange(char cStart, char cStop) {
        return new CharClass() {
        	@Override public boolean contains(char a) { return cStart<=a && a<=cStop; }
        	@Override public String toString() { return "'"+cStart+"'..'"+cStop+"'"; }
        };
    }
    public static CharClass or(CharClass...cClasses) {
        return new CharClass() {
            @Override public boolean contains(char a) {
                for (CharClass cClass:cClasses) {
                    if (cClass.contains(a)) return true;
                }
                return false;
            }
            public String toString() {
            	StringBuilder sb = new StringBuilder();
            	sb.append('(').append(cClasses[0]);
            	for (int i=0; i<cClasses.length; i++) {
            		sb.append(" || ").append(cClasses[i]);
            	}
            	return sb.append(')').toString();
            }
        };
    }
    public static CharClass and(CharClass...cClasses) {
        return new CharClass() {
            @Override public boolean contains(char a) {
                for (CharClass cClass:cClasses) {
                    if (!cClass.contains(a)) return false;
                }
                return true;
            }
            public String toString() {
            	StringBuilder sb = new StringBuilder();
            	sb.append('(').append(cClasses[0]);
            	for (int i=0; i<cClasses.length; i++) {
            		sb.append(" && ").append(cClasses[i]);
            	}
            	return sb.append(')').toString();
            }
        };
    }
    public static CharClass negate(CharClass cClass) {
        return new CharClass() {
        	@Override public boolean contains(char a) { return !cClass.contains(a); }
        	public String toString() {
        		return "!"+cClass;
        	}
        };
    }
}
