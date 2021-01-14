package lexer;

import java.util.List;
import java.util.ArrayList;

public class Branch extends Node {

    public List<Node> childs = new ArrayList<>();

    public Branch(String name, Node...childs) {
        super(name);
        for (Node child:childs) { this.childs.add(child); }
    }

    public Branch(String name, Iterable<? extends Node> childs) {
        super(name);
        for (Node child:childs) { this.childs.add(child); }
    }

    @Override
    void stringValue(StringBuilder sb) {
        for (int i=0, l=childs.size(); i<l; i++) {
            childs.get(i).stringValue(sb);
        }
    }

    @Override
    void toString(StringBuilder sb, int indent) {
        for (int i=indent; i-->0; sb.append(' '));
        sb.append("<").append(name).append(">\n");
        for (Node child:childs) {
            child.toString(sb, indent+3);
        }
        for (int i=indent; i-->0; sb.append(' '));
        sb.append("</").append(name).append(">\n");
    }
}