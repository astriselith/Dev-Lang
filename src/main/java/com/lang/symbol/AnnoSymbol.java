package com.lang.symbol;

public class AnnoSymbol extends Symbol {
    private Symbol type;

    public AnnoSymbol(String name) {
        super(name);
    }

    public void setType(Symbol base) {
        this.type = base;
    }

    public Symbol getType() {
        return type;
    }
}
