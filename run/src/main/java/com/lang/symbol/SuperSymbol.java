package com.lang.symbol;

public class SuperSymbol extends Symbol {
    private Symbol type;

    public SuperSymbol(String name) {
        super(name);
    }

    public void setType(Symbol base) {
        this.type = base;
    }

    public Symbol getType() {
        return type;
    }
}