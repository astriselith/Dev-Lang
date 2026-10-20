package com.lang.token;

import com.lang.util.Position;
import com.lang.util.Positioned;

public class Token implements Positioned {
    public static final int UNDEFINED = 0,
            EOF = 1,

            NULL = 10,
            BOOL = 11,
            INT = 12,
            FLOAT = 13,
            STRING = 14,

            IDENTIFIER = 20,

            PLUS = 30,
            MINUS = 31,
            STAR = 32,
            SLASH = 33,
            PERCENT = 34,

            EQUALS = 40,
            BANG = 41,
            LANGLE = 42,
            RANGLE = 43,

            AMP = 50,
            BAR = 51,

            QUESTION = 60,
            COLON = 61,

            LPAREN = 70,
            RPAREN = 71,
            LBRACE = 72,
            RBRACE = 73,
            LBRACKET = 74,
            RBRACKET = 75,
            COMMA = 76,
            DOT = 77,
            SEMICOLON = 78,

            DOLLAR = 80,
            AT = 81,
            TILDE = 82;

    public static final Token EMPTY = new Token(UNDEFINED, "", Position.UNKNOWN);

    public final int type;
    public final String lexeme;
    public final Position position;

    private Token(int type, String lexeme, Position position) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
    }

    public static Token of(int type, String lexeme, Position position) {
        return new Token(type, lexeme, position);
    }

    public static Token of(int type, String lexeme) {
        return new Token(type, lexeme, Position.ZERO);
    }

    public static Token of(int type) {
        return new Token(type, "", Position.ZERO);
    }

    public Position getPosition() {
        return position;
    }

    public boolean is(int... types) {
        for (int t : types)
            if (this.type == t)
                return true;
        return false;
    }

    public boolean typeEquals(int type) {
        return this.type == type;
    }

    public boolean lexemeEquals(String lexeme) {
        return lexeme != null && lexeme.equals(this.lexeme);
    }

    @Override 
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token other = (Token) obj;
        return type == other.type && lexeme.equals(other.lexeme);
    }

    @Override
    public String toString() {
        return typeName(type) + " '" + lexeme + "' " + position;
    }

    public static String typeName(int type) {
        switch (type) {
            case UNDEFINED:
                return "UNDEFINED";
            case EOF:
                return "EOF";
            case NULL:
                return "NULL";
            case BOOL:
                return "BOOL";
            case INT:
                return "INT";
            case FLOAT:
                return "FLOAT";
            case STRING:
                return "STRING";
            case IDENTIFIER:
                return "IDENTIFIER";
            case PLUS:
                return "PLUS";
            case MINUS:
                return "MINUS";
            case STAR:
                return "STAR";
            case SLASH:
                return "SLASH";
            case PERCENT:
                return "PERCENT";
            case EQUALS:
                return "EQUALS";
            case BANG:
                return "BANG";
            case LANGLE:
                return "LANGLE";
            case RANGLE:
                return "RANGLE";
            case AMP:
                return "AMP";
            case BAR:
                return "BAR";
            case QUESTION:
                return "QUESTION";
            case COLON:
                return "COLON";
            case LPAREN:
                return "LPAREN";
            case RPAREN:
                return "RPAREN";
            case LBRACE:
                return "LBRACE";
            case RBRACE:
                return "RBRACE";
            case LBRACKET:
                return "LBRACKET";
            case RBRACKET:
                return "RBRACKET";
            case COMMA:
                return "COMMA";
            case DOT:
                return "DOT";
            case SEMICOLON:
                return "SEMICOLON";
            case DOLLAR:
                return "DOLLAR";
            case AT:
                return "AT";
            case TILDE:
                return "TILDE";
            default:
                return "UNKNOWN(" + type + ")";
        }
    }
}