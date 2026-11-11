package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class FunDecl extends CallableDecl {

    public BlockStmt body;

    public FunDecl() {
    }

    public FunDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                   Identifier name, Typed type,
                   List<TypeParamDecl> typeParameters,
                   List<ParamDecl> parameters, BlockStmt body,
                   Position position) {
        super(annotations, modifiers, name, type, typeParameters, parameters, position);
        this.body = body;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitFunDecl(this);
    }
}