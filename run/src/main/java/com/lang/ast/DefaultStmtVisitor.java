package com.lang.ast;

public interface DefaultStmtVisitor<T> extends StmtVisitor<T> {

    @Override
    default T visitLetDeclStmt(LetDecl stmt) {
        return null;
    }

    @Override
    default T visitIfStmt(IfStmt stmt) {
        return null;
    }

    @Override
    default T visitWhileStmt(WhileStmt stmt) {
        return null;
    }

    @Override
    default T visitReturnStmt(ReturnStmt stmt) {
        return null;
    }

    @Override
    default T visitBreakStmt(BreakStmt stmt) {
        return null;
    }

    @Override
    default T visitContinueStmt(ContinueStmt stmt) {
        return null;
    }

    @Override
    default T visitBlockStmt(BlockStmt stmt) {
        return null;
    }

    @Override
    default T visitExprStmt(ExprStmt stmt) {
        return null;
    }

    @Override
    default T visitParamDeclStmt(ParamDecl stmt) {
        return null;
    }

    @Override
    default T visitTypeParamDeclStmt(TypeParamDecl stmt) {
        return null;
    }

    @Override
    default T visitClassDeclStmt(ClassDecl stmt) {
        return null;
    }

    @Override
    default T visitVarDeclStmt(VarDecl stmt) {
        return null;
    }

    @Override
    default T visitFunDeclStmt(FunDecl stmt) {
        return null;
    }
}