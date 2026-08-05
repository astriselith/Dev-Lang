package com.lang.ast;

import com.lang.util.Position;

public class SuperDecl extends Decl {
    public Identifier name;
    public Typed type;

    public SuperDecl() {
    }

    public SuperDecl(Identifier name, Typed type, Position position) {
        super(position);
        this.name = name;
        this.type = type;
    }
}
