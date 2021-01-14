package lexer.impl;

public interface CharClass {
	public boolean contains(char c);
    public static CharClass fromChar(char c) {
        return new CharClass() { @Override public boolean contains(char a) { return a==c; } };
    }
    public static CharClass fromRange(char cStart, char cStop) {
        return new CharClass() { @Override public boolean contains(char a) { return cStart<=a && a<=cStop; } };
    }
    public static CharClass or(CharClass...cClasses) {
        return new CharClass() {
            @Override public boolean contains(char a) {
                for (CharClass cClass:cClasses) {
                    if (cClass.contains(a)) return true;
                }
                return false;
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
        };
    }
    public static CharClass negate(CharClass cClass) {
        return new CharClass() { @Override public boolean contains(char a) { return !cClass.contains(a); } };
    }
}
