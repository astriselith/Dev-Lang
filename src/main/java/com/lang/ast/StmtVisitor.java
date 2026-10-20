package com.lang.ast;

public interface StmtVisitor<T> {
	T visitLetStmt(LetStmt stmt);

	T visitIfStmt(IfStmt stmt);

	T visitWhileStmt(WhileStmt stmt);

	T visitBlockStmt(BlockStmt stmt);

	T visitReturnStmt(ReturnStmt stmt);

	T visitBreakStmt(BreakStmt stmt);

	T visitContinueStmt(ContinueStmt stmt);

	T visitExprStmt(ExprStmt stmt);
}