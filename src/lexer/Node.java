package lexer;

public abstract class Node {
    public String name;

    public Node(String name) {
        this.name = name;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }
    abstract void toString(StringBuilder sb, int indent);

    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        stringValue(sb);
        return sb.toString();
    }
    abstract void stringValue(StringBuilder sb);
}