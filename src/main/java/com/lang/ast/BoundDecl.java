package com.lang.ast;

import com.lang.util.Position;

public class BoundDecl extends Decl {
    public Typed type;

    public BoundDecl() {}

    public BoundDecl(Typed type, Position position) {
        super(position);
        this.type = type;
    }
}