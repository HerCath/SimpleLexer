package lexer;

public class Leaf extends Node {

    public CharSequence value;

    public Leaf(String name, CharSequence value) {
        super(name);
        this.value = value;
    }

    @Override
    void toString(StringBuilder sb, int indent) {
        while (indent-->0) sb.append(' ');
        if (value==null)
            sb.append('<').append(name).append("/>\n");
        else
            sb.append('<').append(name).append('>').append(value).append("</").append(name).append(">\n");
    }

    @Override
    void stringValue(StringBuilder sb) {
        if (value != null) sb.append(value);
    }
}