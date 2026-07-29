package com.lang.ast;

public interface DefaultExprVisitor<T> extends ExprVisitor<T> {

    @Override
    default T visitLiteralExpr(LiteralExpr expr) {
        return null;
    }

    @Override
    default T visitRefExpr(RefExpr expr) {
        return null;
    }

    @Override
    default T visitParenthesisExpr(ParenthesisExpr expr) {
        return null;
    }

    @Override
    default T visitAssignExpr(AssignExpr expr) {
        return null;
    }

    @Override
    default T visitBinaryExpr(BinaryExpr expr) {
        return null;
    }

    @Override
    default T visitUnaryExpr(UnaryExpr expr) {
        return null;
    }

    @Override
    default T visitTernaryExpr(TernaryExpr expr) {
        return null;
    }

    @Override
    default T visitCallExpr(CallExpr expr) {
        return null;
    }

    @Override
    default T visitIndexAccessExpr(IndexAccessExpr expr) {
        return null;
    }

    @Override
    default T visitMemberAccessExpr(MemberAccessExpr expr) {
        return null;
    }
}