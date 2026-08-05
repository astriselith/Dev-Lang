package com.lang.ast;

public interface StmtVisitor<T> {
	T visitLetDeclStmt(LetDecl stmt);
	T visitIfStmt(IfStmt stmt);
	T visitWhileStmt(WhileStmt stmt);
	T visitReturnStmt(ReturnStmt stmt);
	T visitBreakStmt(BreakStmt stmt);
	T visitContinueStmt(ContinueStmt stmt);
	T visitBlockStmt(BlockStmt stmt);
	T visitExprStmt(ExprStmt stmt);
	T visitParamDeclStmt(ParamDecl stmt);
	T visitTypeParamDeclStmt(TypeParamDecl stmt);
	T visitClassDeclStmt(ClassDecl stmt);
	T visitVarDeclStmt(VarDecl stmt);
	T visitFunDeclStmt(FunDecl stmt);
}