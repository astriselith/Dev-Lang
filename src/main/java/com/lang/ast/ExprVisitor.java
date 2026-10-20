package com.lang.ast;

public interface ExprVisitor<T> {
	T visitLiteralExpr(LiteralExpr expr);
	T visitRefExpr(RefExpr expr);
	T visitParenthesisExpr(ParenthesisExpr expr);
	T visitAssignExpr(AssignExpr expr);
	T visitBinaryExpr(BinaryExpr expr);
	T visitUnaryExpr(UnaryExpr expr);
	T visitCallExpr(CallExpr expr);
	T visitMemberAccessExpr(MemberAccessExpr expr);
}