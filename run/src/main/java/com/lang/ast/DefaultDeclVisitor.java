package com.lang.ast;

public interface DefaultDeclVisitor<T> extends DeclVisitor<T> {

    @Override
    default T visitClassDecl(ClassDecl decl) {
        return null;
    }

    @Override
    default T visitTypeParamDecl(TypeParamDecl decl) {
        return null;
    }

    @Override
    default T visitSuperDecl(SuperDecl decl) {
        return null;
    }

    @Override
    default T visitVarDecl(VarDecl decl) {
        return null;
    }

    @Override
    default T visitFunDecl(FunDecl decl) {
        return null;
    }

    @Override
    default T visitParamDecl(ParamDecl decl) {
        return null;
    }

}
