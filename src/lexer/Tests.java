package lexer;

import java.io.File;

public class Tests {
    public static void main(String[] args) throws Throwable {
        String grammar = 
            "main = +a1 +a2 +a3 +A1 +A2 +A3;\n"
            +"A1 = +'a'|+'A';\n"
            +"a1 = 'b'||'B';\n"
            +"a2 = a1;\n"
            +"a3 = a1;\n"
            +"A2 = +A1;\n"
            +"A3 = +A1;\n"
            ;
        //System.out.println(Utils.LEXER_PARSER.parse(grammar));
        //Lexer lexer_parser = Utils.toLexer(Utils.toString(new File("src/lexer/lexer.g")));
        //System.out.println(lexer_parser.parse(grammar));
        //System.out.println(lexer_parser.parse(grammar));
        Lexer lexer = Utils.toLexer(grammar);
        String input = "bbbAAA";
        System.out.println(lexer.parse(input));
    }
}
