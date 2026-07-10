package com.dev.lang.semantic;

import com.dev.lang.ast.*;
import com.dev.lang.symbol.*;
import com.dev.lang.unit.CompilationException;
import com.dev.lang.unit.CompilationUnit;
import java.util.*;

public class ClassRegister implements StmtVisitor<Void> {
	private final SymbolTable symbolTable;
	private final CompilationUnit compilationUnit;

	public ClassRegister(SymbolTable symbolTable, CompilationUnit compilationUnit) {
		this.symbolTable = symbolTable;
		this.compilationUnit = compilationUnit;
	}

	private void error(Node node, SemanticErrorCode code, Object... args) {
		compilationUnit.addError(new CompilationException(code.format(args), node));
	}

	@Override
	public Void visitClassOrTraitDeclStmt(ClassOrTraitDeclStmt stmt) {
		if (symbolTable.get(stmt.name) != null) {
			error(stmt, SemanticErrorCode.REDEFINED_CLASS, stmt.name);
			return null;
		}

		List<String> typeParameterNames = new ArrayList<>();
		for (TypeParamDeclStmt param : stmt.typeParameters) {
			typeParameterNames.add(param.name);
		}

		Typed classType;
		if (!typeParameterNames.isEmpty()) {
			List<Typed> typeArgs = new ArrayList<>();
			for (String tp : typeParameterNames) {
				typeArgs.add(new RefTyped(tp, stmt.getPosition()));
			}
			classType = new ParameterizedRefTyped(stmt.name, typeArgs, stmt.getPosition());
		} else {
			classType = new RefTyped(stmt.name, stmt.getPosition());
		}

		Typed superclassType = stmt.hasSuperclass() ? stmt.superclass : null;
		List<Typed> supertraitTypes = new ArrayList<>(stmt.supertraits);

		Symbol.Kind kind = stmt.isClass() ? Symbol.Kind.CLASS : Symbol.Kind.TRAIT;
		ClassOrTraitSymbol classSymbol = new ClassOrTraitSymbol(
			stmt.name, kind, typeParameterNames, classType, superclassType, supertraitTypes
		);
		classSymbol.modifier = stmt.modifier;

		symbolTable.register(stmt.name, classSymbol);

		return null;
	}

	@Override
	public Void visitFunDeclStmt(FunDeclStmt stmt) {
		return null;
	}

	@Override
	public Void visitVarDeclStmt(VarDeclStmt stmt) {
		return null;
	}

	@Override
	public Void visitLetDeclStmt(LetDeclStmt stmt) {
		return null;
	}

	@Override
	public Void visitParamDeclStmt(ParamDeclStmt stmt) {
		return null;
	}

	@Override
	public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
		return null;
	}

	@Override
	public Void visitBlockStmt(BlockStmt stmt) {
		return null;
	}

	@Override
	public Void visitIfStmt(IfStmt stmt) {
		return null;
	}

	@Override
	public Void visitWhileStmt(WhileStmt stmt) {
		return null;
	}

	@Override
	public Void visitReturnStmt(ReturnStmt stmt) {
		return null;
	}

	@Override
	public Void visitBreakStmt(BreakStmt stmt) {
		return null;
	}

	@Override
	public Void visitContinueStmt(ContinueStmt stmt) {
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt stmt) {
		return null;
	}
}