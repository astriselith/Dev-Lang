package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;
import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import java.util.*;

public class ClassBinder implements StmtVisitor<Void> {
	private final SymbolTable table;
	private final CompilationUnit unit;
	private ClassSymbol currentClass;
	private FunSymbol currentFun;

	private Map<String, TypeParamSymbol> currentTypeParameters = new LinkedHashMap<>();
	private Map<String, ParamSymbol> currentParameters = new LinkedHashMap<>();

	public ClassBinder(SymbolTable table, CompilationUnit unit) {
		this.table = table;
		this.unit = unit;
	}

	public boolean hasCurrentClass() {
		return currentClass != null;
	}

	public boolean hasCurrentFun() {
		return currentFun != null;
	}

	public ClassSymbol getCurrentClass() {
		return currentClass;
	}

	public FunSymbol getCurrentFun() {
		return currentFun;
	}

	@Override
	public Void visitClassDeclStmt(ClassDeclStmt stmt) {
		ClassSymbol classSymbol = table.get(stmt.name);

		for (TypeParamDeclStmt typeParam : stmt.typeParameters) {
			typeParam.accept(this);
		}

		classSymbol.setTypeParameters(new ArrayList<>(currentTypeParameters.values()));

		currentTypeParameters.clear();

		Map<String, Symbol> selfSubstitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : classSymbol.getTypeParameters()) {
			selfSubstitutions.put(tp.getName(), tp);
		}

		Typed superclassType = classSymbol.getSuperclassType();
		if (superclassType != null) {
			Symbol resolved = table.resolve(superclassType, selfSubstitutions);

			if (resolved != null) {
				if (resolved == classSymbol) {
					unit.error(TAG, CIRCULAR_INHERITANCE.format(stmt.name), stmt);
				} else {
					classSymbol.setSuperclass(resolved.asClass());
				}
			} else {
				unit.error(TAG, UNDEFINED_CLASS.format(superclassType.getName()), stmt);
			}
		} else {
			classSymbol.setSuperclass(table.getAny());
		}

		List<Typed> supertraitTypes = classSymbol.getSupertraitTypes();
		if (!supertraitTypes.isEmpty()) {
			List<Symbol> supertraits = new ArrayList<>();

			for (Typed traitType : supertraitTypes) {
				Symbol resolved = table.resolve(traitType, selfSubstitutions);

				if (resolved != null) {
					if (resolved == classSymbol) {
						unit.error(TAG, CIRCULAR_INHERITANCE.format(stmt.name), stmt);
					} else {
						supertraits.add(resolved);
					}
				} else {
					unit.error(TAG, UNDEFINED_CLASS.format(traitType.getName()), stmt);
				}
			}
			classSymbol.setSupertraits(supertraits);
		}

		currentClass = classSymbol;

		for (VarDeclStmt var : stmt.vars) {
			var.accept(this);
		}

		for (FunDeclStmt fun : stmt.funs) {
			fun.accept(this);
		}

		currentClass = null;
		return null;
	}

	@Override
	public Void visitFunDeclStmt(FunDeclStmt stmt) {
		if (currentClass.hasFun(stmt.name)) {
			unit.error(TAG, REDEFINED_FUNCTION.format(stmt.name), stmt);
			return null;
		}

		for (TypeParamDeclStmt typeParam : stmt.typeParameters) {
			typeParam.accept(this);
		}

		List<TypeParamSymbol> typeParameters = new ArrayList<>(currentTypeParameters.values());
		currentTypeParameters.clear();

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : currentClass.getTypeParameters()) {
			substitutions.put(tp.getName(), tp);
		}

		for (TypeParamSymbol tp : typeParameters) {
			substitutions.put(tp.getName(), tp);
		}

		for (ParamDeclStmt param : stmt.parameters) {
			param.accept(this);
		}

		List<ParamSymbol> params = new ArrayList<>(currentParameters.values());
		currentParameters.clear();

		Symbol returnType = null;

		if (stmt.hasReturnType()) {
			returnType = table.resolve(stmt.returnType, substitutions);
		} else {
			returnType = table.getVoid();
		}

		FunSymbol funSymbol = new FunSymbol(
				stmt.name, params, returnType,
				typeParameters);

		funSymbol.setBody(stmt.body);

		currentClass.addFun(funSymbol);
		return null;
	}

	@Override
	public Void visitVarDeclStmt(VarDeclStmt stmt) {
		if (currentClass.hasVar(stmt.name)) {
			unit.error(TAG, REDEFINED_VARIABLE.format(stmt.name), stmt);
			return null;
		}

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : currentClass.getTypeParameters()) {
			substitutions.put(tp.getName(), tp);
		}

		Typed varType = stmt.type;
		Symbol varTypeSymbol = table.getAny();

		if (varType != null) {
			varTypeSymbol = table.resolve(varType, substitutions);

			if (varTypeSymbol == null) {
				unit.error(TAG, UNDEFINED_CLASS.format(varType.getName()), stmt);
			}
		}

		VarSymbol varSymbol = new VarSymbol(stmt.name, varTypeSymbol);
		currentClass.addVar(varSymbol);
		return null;
	}

	@Override
	public Void visitParamDeclStmt(ParamDeclStmt stmt) {
		if (currentParameters.containsKey(stmt.name)) {
			unit.error(TAG, REDEFINED_PARAMETER.format(stmt.name), stmt);
			return null;
		}

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		for (TypeParamSymbol tp : currentClass.getTypeParameters()) {
			substitutions.put(tp.getName(), tp);
		}

		Typed paramType = stmt.type;
		Symbol paramTypeSymbol = table.getAny();

		if (paramType != null) {
			paramTypeSymbol = table.resolve(paramType, substitutions);

			if (paramTypeSymbol == null) {
				unit.error(TAG, UNDEFINED_CLASS.format(paramType.getName()), stmt);
			}
		}

		ParamSymbol paramSymbol = new ParamSymbol(stmt.name, paramTypeSymbol);
		currentParameters.put(stmt.name, paramSymbol);
		return null;
	}

	@Override
	public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {

		if (currentTypeParameters.containsKey(stmt.name)) {
			unit.error(TAG, REDEFINED_PARAMETER.format(stmt.name), stmt);
			return null;
		}

		Symbol superclass = null;
		List<Symbol> supertraits = new ArrayList<>();

		Map<String, Symbol> substitutions = new LinkedHashMap<>();
		if (currentClass != null) {
			for (TypeParamSymbol tp : currentClass.getTypeParameters()) {
				substitutions.put(tp.getName(), tp);
			}
		}

		if (currentFun != null) {
			for (TypeParamSymbol tp : currentFun.getTypeParameters()) {
				substitutions.put(tp.getName(), tp);
			}
		}

		Typed superclassType = stmt.superclass;
		if (superclassType != null) {
			Symbol resolved = table.resolve(superclassType, substitutions);

			if (resolved != null) {
				if (resolved == currentClass) {
					unit.error(TAG, CIRCULAR_INHERITANCE.format(stmt.name), stmt);
				} else {
					superclass = resolved;
				}
			} else {
				unit.error(TAG, UNRESOLVED_SUPERTYPE.format(superclassType.getName()), stmt);
			}
		}

		List<Typed> supertraitTypes = stmt.supertraits;
		if (!supertraitTypes.isEmpty()) {
			for (Typed traitType : supertraitTypes) {
				Symbol resolved = table.resolve(traitType, substitutions);

				if (resolved != null) {
					if (resolved == currentClass) {
						unit.error(TAG, CIRCULAR_INHERITANCE.format(stmt.name), stmt);
					} else {
						supertraits.add(resolved);
					}
				} else {
					unit.error(TAG, UNRESOLVED_SUPERTYPE.format(traitType.getName()), stmt);
				}
			}
		}

		TypeParamSymbol typeParamSymbol = new TypeParamSymbol(stmt.name, superclass, supertraits);
		currentTypeParameters.put(stmt.name, typeParamSymbol);

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