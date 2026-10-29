package com.lang.ast;

public interface ExprVisitor<T> {
    T visitLiteralExpr(LiteralExpr expr);
    T visitRefExpr(RefExpr expr);
    T visitMemberExpr(MemberExpr expr);
    T visitCallExpr(CallExpr expr);
    T visitParenthesisExpr(ParenthesisExpr expr);
    T visitOperatorExpr(OperatorExpr expr);
}