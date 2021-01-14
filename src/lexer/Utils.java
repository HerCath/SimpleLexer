package lexer;

import lexer.impl.*;

public class Utils {

    public static Lexer toLexer(String lexerExpression) {
        throw new RuntimeException("Not yet implemented");
    }

    public static Lexer toLexer(Rule rule) {
        return new Lexer() {
            @Override public Node parse(CharSequence input) {
                Context ctx = new Context(input);
                Object state = rule.createInitialState(ctx);
                while (true) {
                    MatchedContent mc = rule.tryToMatch(ctx, state);
                    if (mc != null) return mc.captured;
                    if (!rule.nextState(ctx, state)) return null;
                }
            }
        };
    }

    public static void main(String...args) throws Throwable {
        Rule lower = new RuleChar(true, CharClass.fromRange('a', 'z'));
        Rule upper = new RuleChar(false, CharClass.fromRange('A', 'Z'));
        Rule letter = new RuleOr(lower, upper);
        Rule digit = new RuleChar(true, CharClass.fromRange('0', '9'));
        Rule letter_then_digit = new RuleAnd(letter, digit);
        Lexer grammar = toLexer(letter_then_digit);
        System.out.println(grammar.parse("A3"));
        System.out.println(grammar.parse("AA"));
        System.out.println(grammar.parse("a3"));
    }
}
