package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;
import java.util.ArrayList;
import java.util.List;

import com.lang.ast.*;
import com.lang.symbol.ClassSymbol;
import com.lang.symbol.SymbolTable;
import com.lang.unit.CompilationUnit;

public class ClassRegister implements StmtVisitor<Void> {
	private final SymbolTable symbolTable;
	private final CompilationUnit compilationUnit;

	public ClassRegister(SymbolTable symbolTable, CompilationUnit compilationUnit) {
		this.symbolTable = symbolTable;
		this.compilationUnit = compilationUnit;
	}

	@Override
	public Void visitClassDeclStmt(ClassDeclStmt stmt) {
		if (symbolTable.get(stmt.name) != null) {
			compilationUnit.error(TAG, REDEFINED_CLASS.format(stmt.name), stmt);
			return null;
		}

		List<String> typeParameterTypes = new ArrayList<>();
		for (TypeParamDeclStmt param : stmt.typeParameters) {
			typeParameterTypes.add(param.name);
		}

		List<Typed> superclassTypes = new ArrayList<>(stmt.superclasses);

		ClassSymbol classSymbol = new ClassSymbol(
				stmt.name, typeParameterTypes, superclassTypes);

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