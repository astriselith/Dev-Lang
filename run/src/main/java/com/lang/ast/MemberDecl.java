package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class MemberDecl extends Decl {
    public List<Meta> metas;
    public Identifier name;

    public MemberDecl() {
    }

    public MemberDecl(List<Meta> metas, Identifier name, Position position) {
        super(position);
        this.metas = metas;
        this.name = name;
    }
}
