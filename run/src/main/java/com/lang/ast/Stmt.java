package com.lang.ast;

import com.lang.util.Position;

public abstract class Stmt extends Node {

	public Stmt() {
	}

	public Stmt(Position position) {
		super(position);
	}

	public <T> T accept(StmtVisitor<T> visitor) {
		if (this instanceof IfStmt)
			return visitor.visitIfStmt((IfStmt) this);
		if (this instanceof WhileStmt)
			return visitor.visitWhileStmt((WhileStmt) this);
		if (this instanceof ReturnStmt)
			return visitor.visitReturnStmt((ReturnStmt) this);
		if (this instanceof BreakStmt)
			return visitor.visitBreakStmt((BreakStmt) this);
		if (this instanceof ContinueStmt)
			return visitor.visitContinueStmt((ContinueStmt) this);
		if (this instanceof BlockStmt)
			return visitor.visitBlockStmt((BlockStmt) this);
		if (this instanceof ExprStmt)
			return visitor.visitExprStmt((ExprStmt) this);
		if (this instanceof ParamDeclStmt)
			return visitor.visitParamDeclStmt((ParamDeclStmt) this);
		if (this instanceof TypeParamDeclStmt)
			return visitor.visitTypeParamDeclStmt((TypeParamDeclStmt) this);
		if (this instanceof ClassDeclStmt)
			return visitor.visitClassDeclStmt((ClassDeclStmt) this);
		if (this instanceof VarDeclStmt)
			return visitor.visitVarDeclStmt((VarDeclStmt) this);
		if (this instanceof FunDeclStmt)
			return visitor.visitFunDeclStmt((FunDeclStmt) this);

		throw new UnsupportedOperationException("Unknown statement type: " + this.getClass().getName());
	}
}