package com.lang.ast;

import com.lang.util.Position;

public abstract class MemberDecl extends Decl {
    public Identifier name;
    public Typed type;

    public MemberDecl() {
    }

    public MemberDecl(Identifier name, Typed type, Position position) {
        super(position);
        this.name = name;
        this.type = type;
    }

    @Override
    public abstract <T> T accept(DeclVisitor<T> visitor);
}
