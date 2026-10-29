package com.lang.ast;

import com.lang.util.Position;

public abstract class Decl extends Node {

    public Decl() {
    }

    public Decl(Position position) {
        super(position);
    }

    public abstract <T> T accept(DeclVisitor<T> visitor);
}
