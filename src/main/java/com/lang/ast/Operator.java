package com.lang.ast;

import com.lang.util.Position;

public class Operator extends Node {

    public static final int PREFIX  = 0;
    public static final int INFIX   = 1;
    public static final int POSTFIX = 2;

    public String lexeme;

    public Operator() {
    }

    public Operator(String lexeme, Position position) {
        super(position);
        this.lexeme = lexeme;
    }

    public boolean isCompound() {
        return lexeme.length() > 1;
    }

    @Override
    public String toString() {
        return lexeme;
    }
}