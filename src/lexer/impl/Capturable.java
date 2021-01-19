package lexer.impl;

public abstract class Capturable<STATE extends State> extends Rule<STATE> {
    final boolean capture;
    Capturable(boolean capture) { this.capture = capture; }
}
