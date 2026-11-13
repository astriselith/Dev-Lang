package com.lang.symbol;

import com.lang.ast.Operator;

public class OperatorSymbol extends CallableSymbol {

    private final int kind;
    private final Operator operator;

    public OperatorSymbol(int kind, Operator operator) {
        super(operator != null ? operator.lexeme : "");
        this.kind = kind;
        this.operator = operator;
    }

    public int getKind() {
        return kind;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getLexeme() {
        return operator != null ? operator.lexeme : "";
    }

    public String signatureKey() {
        return signatureKey(kind, operator);
    }

    public static String signatureKey(int kind, Operator operator) {
        String name = operator != null ? operator.name() : null;
        if (name == null)
            name = operator != null ? operator.lexeme : "";
        return Operator.typeName(kind) + "_" + name;
    }
}