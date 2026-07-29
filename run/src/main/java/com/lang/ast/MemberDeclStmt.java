package com.lang.ast;

import com.lang.util.Position;

public class MemberDeclStmt extends Stmt {
    public Identifier name;

    public MemberDeclStmt() {
    }

    public MemberDeclStmt(Identifier name, Position position) {
        super(position);
        this.name = name;
    }
}
