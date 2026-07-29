package com.lang.ast;

public interface DefaultStmtVisitor<T> extends StmtVisitor<T> {

    @Override
    default T visitLetDeclStmt(LetDeclStmt stmt) {
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
    default T visitParamDeclStmt(ParamDeclStmt stmt) {
        return null;
    }

    @Override
    default T visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
        return null;
    }

    @Override
    default T visitClassDeclStmt(ClassDeclStmt stmt) {
        return null;
    }

    @Override
    default T visitVarDeclStmt(VarDeclStmt stmt) {
        return null;
    }

    @Override
    default T visitFunDeclStmt(FunDeclStmt stmt) {
        return null;
    }
}