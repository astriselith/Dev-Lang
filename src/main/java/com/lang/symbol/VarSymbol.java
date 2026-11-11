package com.lang.symbol;

public class VarSymbol extends MemberSymbol {

    private Symbol type;

    public VarSymbol(String name) {
        super(name);
    }

    public void setType(Symbol type) {
        this.type = type;
    }

    public Symbol getType() {
        return type;
    }
}