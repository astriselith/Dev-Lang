package com.dev.lang.semantic;

import com.dev.lang.ast.*;
import com.dev.lang.symbol.*;
import com.dev.lang.unit.CompilationException;
import com.dev.lang.unit.CompilationUnit;
import com.dev.lang.unit.WarningException;
import java.util.*;

public class ClassBinder implements StmtVisitor<Void> {
	private final SymbolTable symbolTable;
	private final CompilationUnit compilationUnit;
	private ClassOrTraitSymbol currentClassOrTrait;

	public ClassBinder(SymbolTable symbolTable, CompilationUnit compilationUnit) {
		this.symbolTable = symbolTable;
		this.compilationUnit = compilationUnit;
	}

	private void error(Node node, SemanticErrorCode code, Object... args) {
		compilationUnit.addError(new CompilationException(code.format(args), node));
	}

	private void warning(Node node, String code, String message) {
		compilationUnit.addWarning(new WarningException(code, message, node));
	}

	@Override
	public Void visitClassOrTraitDeclStmt(ClassOrTraitDeclStmt stmt) {
		ClassOrTraitSymbol classSymbol = symbolTable.get(stmt.name);
		if (classSymbol == null) {
			error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.name);
			return null;
		}

		List<String> typeParamNames = classSymbol.getTypeParameterNames();
		for (int i = 0; i < typeParamNames.size(); i++) {
			String name = typeParamNames.get(i);
			TypeParamSymbol tp = new TypeParamSymbol(name, i);
			classSymbol.addTypeParameter(tp);
		}

		Map<String, Symbol> selfSubstitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : classSymbol.getTypeParameters()) {
			selfSubstitutions.put(tp.name, tp);
		}

		Typed superclassType = classSymbol.getSuperclassType();
		if (superclassType != null) {
			Symbol resolved = symbolTable.resolve(superclassType, selfSubstitutions);

			if (resolved != null) {
				ClassOrTraitSymbol baseSymbol;
				if (resolved instanceof ParameterizedClassOrTraitSymbol) {
					baseSymbol = ((ParameterizedClassOrTraitSymbol) resolved).getBaseSymbol();
				} else if (resolved instanceof ClassOrTraitSymbol) {
					baseSymbol = (ClassOrTraitSymbol) resolved;
				} else {
					error(stmt, SemanticErrorCode.INVALID_SUPERCLASS, superclassType.getName());
					return null;
				}

				if (baseSymbol.isTrait()) {
					error(stmt, SemanticErrorCode.CANNOT_INHERIT_FROM_FINAL,
						  "Cannot extend a trait '" + baseSymbol.name + "'. Use '|' for traits.");
				} else if (baseSymbol.name.equals(stmt.name)) {
					error(stmt, SemanticErrorCode.CIRCULAR_INHERITANCE, stmt.name);
				} else {
					classSymbol.setSuperclass(resolved);
				}
			} else {
				error(stmt, SemanticErrorCode.UNDEFINED_CLASS, superclassType.getName());
			}
		}

		List<Typed> supertraitTypes = classSymbol.getSupertraitTypes();
		if (!supertraitTypes.isEmpty()) {
			List<Symbol> supertraits = new ArrayList<>();

			for (Typed traitType : supertraitTypes) {
				Symbol resolved = symbolTable.resolve(traitType, selfSubstitutions);

				if (resolved != null) {
					ClassOrTraitSymbol baseSymbol;
					if (resolved instanceof ParameterizedClassOrTraitSymbol) {
						baseSymbol = ((ParameterizedClassOrTraitSymbol) resolved).getBaseSymbol();
					} else if (resolved instanceof ClassOrTraitSymbol) {
						baseSymbol = (ClassOrTraitSymbol) resolved;
					} else {
						error(stmt, SemanticErrorCode.INVALID_TRAIT, traitType.getName());
						continue;
					}

					if (!baseSymbol.isTrait()) {
						error(stmt, SemanticErrorCode.CANNOT_INHERIT_FROM_FINAL,
							  "Expected trait but found class '" + baseSymbol.name + "'");
					} else {
						supertraits.add(resolved);
					}
				} else {
					error(stmt, SemanticErrorCode.UNDEFINED_CLASS, traitType.getName());
				}
			}
			classSymbol.setSupertraits(supertraits);
		}

		if (classSymbol.isClass() && !classSymbol.hasSuperclass() && !classSymbol.name.equals("Any")) {
			ClassOrTraitSymbol anyClass = symbolTable.get("Any");
			if (anyClass != null) {
				classSymbol.setSuperclass(anyClass);
			}
		}

		currentClassOrTrait = classSymbol;

		for (MemberDeclStmt member : stmt.members) {
			if (member != null) {
				member.accept(this);
			}
		}

		currentClassOrTrait = null;

		return null;
	}

	@Override
	public Void visitFunDeclStmt(FunDeclStmt stmt) {
		if (currentClassOrTrait == null) {
			error(stmt, SemanticErrorCode.FUNCTION_OUTSIDE_CLASS, stmt.name);
			return null;
		}

		FunSymbol existing = currentClassOrTrait.getDeclaredFun(stmt.name);
		if (existing != null) {
			error(stmt, SemanticErrorCode.REDEFINED_FUNCTION, stmt.name);
			return null;
		}

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : currentClassOrTrait.getTypeParameters()) {
			substitutions.put(tp.name, tp);
		}

		List<TypeParamSymbol> typeParameters = new ArrayList<>();

		for (int i = 0; i < stmt.typeParameters.size(); i++) {
			TypeParamDeclStmt tp = stmt.typeParameters.get(i);
			TypeParamSymbol tpSym = new TypeParamSymbol(tp.name, i);
			typeParameters.add(tpSym);
			substitutions.put(tp.name, tpSym);
		}

		List<ParamSymbol> params = new ArrayList<>();
		for (int i = 0; i < stmt.parameters.size(); i++) {
			ParamDeclStmt param = stmt.parameters.get(i);

			Symbol paramType = null;

			if (param.hasType()) {
				paramType = symbolTable.resolve(param.type, substitutions);
			}

			ParamSymbol paramSym = new ParamSymbol(param.name, paramType, i, null);
			params.add(paramSym);
		}

		Symbol returnType = null;

		if (stmt.hasReturnType()) {
			returnType = symbolTable.resolve(stmt.returnType, substitutions);
		} else {
			returnType = symbolTable.get("Void");
		}

		FunSymbol funSymbol = new FunSymbol(
			stmt.name, params, returnType, currentClassOrTrait,
			typeParameters
		);
		funSymbol.modifier = stmt.modifier;
		funSymbol.setBody(stmt.body);

		currentClassOrTrait.addMember(funSymbol);
		return null;
	}

	@Override
	public Void visitVarDeclStmt(VarDeclStmt stmt) {
		if (currentClassOrTrait == null) {
			error(stmt, SemanticErrorCode.VARIABLE_OUTSIDE_CLASS, stmt.name);
			return null;
		}

		if (currentClassOrTrait.getDeclaredVar(stmt.name) != null) {
			error(stmt, SemanticErrorCode.REDEFINED_VARIABLE, stmt.name);
			return null;
		}

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : currentClassOrTrait.getTypeParameters()) {
			substitutions.put(tp.name, tp);
		}

		Symbol varType = null;

		if (stmt.hasType()) {
			varType = symbolTable.resolve(stmt.getType(), substitutions);

			if (varType == null) {
				error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.getType().getName());
			}
		}

		VarSymbol varSymbol = new VarSymbol(stmt.name, varType, currentClassOrTrait);
		varSymbol.modifier = stmt.modifier;

		if (stmt.getValue() != null) {
			varSymbol.setInitialized(true);
		}

		currentClassOrTrait.addMember(varSymbol);
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

	@Override
	public Void visitLetDeclStmt(LetDeclStmt stmt) {
		return null;
	}
}