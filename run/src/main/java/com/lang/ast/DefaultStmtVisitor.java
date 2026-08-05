package com.lang.ast;

public interface DefaultStmtVisitor<T> extends StmtVisitor<T> {

    @Override
    default T visitLetStmt(LetStmt stmt) {
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

}