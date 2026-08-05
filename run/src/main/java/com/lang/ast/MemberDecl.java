package com.lang.ast;

import com.lang.util.Position;

public class MemberDecl extends Decl {
    public Identifier name;

    public MemberDecl() {
    }

    public MemberDecl(Identifier name, Position position) {
        super(position);
        this.name = name;
    }
}
