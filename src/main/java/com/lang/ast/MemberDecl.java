package com.lang.ast;

import com.lang.util.Position;

public class MemberDecl extends Decl {
    public Identifier name;
    public Typed type;

    public MemberDecl() {
    }

    public MemberDecl(Identifier name, Typed type, Position position) {
        super(position);
        this.name = name;
        this.type = type;
    }
}
