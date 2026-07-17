package com.lang.ast;

public interface ExprVisitor<T> {
	T visitLiteralExpr(LiteralExpr expr);
	T visitRefExpr(RefExpr expr);
	T visitAssignExpr(AssignExpr expr);
	T visitBinaryExpr(BinaryExpr expr);
	T visitUnaryExpr(UnaryExpr expr);
	T visitTernaryExpr(TernaryExpr expr);
	T visitCallExpr(CallExpr expr);
	T visitIndexAccessExpr(IndexAccessExpr expr);
	T visitMemberAccessExpr(MemberAccessExpr expr);
}