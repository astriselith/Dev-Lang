package com.lang.symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lang.ast.Operator;
import com.lang.ast.Typed;

public class OperatorSymbol extends MemberSymbol {

    private final int kind;
    private final Operator operator;
    private final Map<String, TypeParamSymbol> typeParameters = new LinkedHashMap<>();
    private final Map<String, ParamSymbol> parameters = new LinkedHashMap<>();

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

    public void addTypeParameter(TypeParamSymbol parameter) {
        typeParameters.put(parameter.getName(), parameter);
    }

    public TypeParamSymbol getTypeParameter(String name) {
        return typeParameters.get(name);
    }

    public boolean hasTypeParameter(String name) {
        return typeParameters.containsKey(name);
    }

    public Map<String, TypeParamSymbol> getTypeParameters() {
        return Collections.unmodifiableMap(typeParameters);
    }

    public void addParameter(ParamSymbol parameter) {
        parameters.put(parameter.getName(), parameter);
    }

    public ParamSymbol getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, ParamSymbol> getParameters() {
        return Collections.unmodifiableMap(parameters);
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
