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

    public String name() {
        return name(lexeme);
    }

    public static String name(String lexeme) {
        if (lexeme == null || lexeme.isEmpty())
            return null;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lexeme.length(); i++) {
            String part;
            switch (lexeme.charAt(i)) {
                case '+': part = "plus";      break;
                case '-': part = "minus";     break;
                case '*': part = "star";      break;
                case '/': part = "slash";     break;
                case '%': part = "percent";   break;

                case '=': part = "equals";    break;
                case '!': part = "bang";      break;

                case '<': part = "less";      break;
                case '>': part = "greater";   break;

                case '&': part = "amp";       break;
                case '|': part = "bar";       break;

                case '?': part = "question";  break;
                case ':': part = "colon";     break;
                case '~': part = "tilde";     break;

                default:  return null;
            }
            if (builder.length() > 0)
                builder.append('_');
            builder.append(part);
        }
        return builder.toString();
    }

    public static String typeName(int kind) {
        switch (kind) {
            case PREFIX:  return "prefix";
            case INFIX:   return "infix";
            case POSTFIX: return "postfix";
            default:      return "unknown";
        }
    }

    @Override
    public String toString() {
        return lexeme;
    }
}