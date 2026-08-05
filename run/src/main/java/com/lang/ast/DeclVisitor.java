package com.lang.ast;

public interface DeclVisitor<T> {
    T visitClassDecl(ClassDecl decl);

    T visitTypeParamDecl(TypeParamDecl decl);

    T visitSuperDecl(SuperDecl decl);

    T visitVarDecl(VarDecl decl);

    T visitFunDecl(FunDecl decl);

    T visitParamDecl(ParamDecl decl);
}
