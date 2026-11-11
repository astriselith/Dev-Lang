package com.lang.symbol;

import java.util.List;

import com.lang.ast.Operator;
import com.lang.ast.Typed;

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

    public String signatureKey(List<? extends Typed> operandTypes) {
        return signatureKey(kind, operator, operandTypes);
    }

    public static String signatureKey(int kind, Operator operator,
            List<? extends Typed> operandTypes) {
        String lexeme = operator != null ? operator.lexeme : "";
        StringBuilder signature = new StringBuilder()
                .append(kind).append("|").append(lexeme);
        if (operandTypes != null) {
            for (Typed operandType : operandTypes)
                signature.append("|").append(
                        operandType != null ? operandType.getSignature() : "");
        }
        return signature.toString();
    }
}