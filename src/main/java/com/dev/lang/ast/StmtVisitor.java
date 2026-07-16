package com.dev.lang.ast;

public interface StmtVisitor<T> {
	T visitLetDeclStmt(LetDeclStmt stmt);
	T visitIfStmt(IfStmt stmt);
	T visitWhileStmt(WhileStmt stmt);
	T visitReturnStmt(ReturnStmt stmt);
	T visitBreakStmt(BreakStmt stmt);
	T visitContinueStmt(ContinueStmt stmt);
	T visitBlockStmt(BlockStmt stmt);
	T visitExprStmt(ExprStmt stmt);
	T visitParamDeclStmt(ParamDeclStmt stmt);
	T visitTypeParamDeclStmt(TypeParamDeclStmt stmt);
	T visitClassOrTraitDeclStmt(ClassDeclStmt stmt);
	T visitVarDeclStmt(VarDeclStmt stmt);
	T visitFunDeclStmt(FunDeclStmt stmt);
}