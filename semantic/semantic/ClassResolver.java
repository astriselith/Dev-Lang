package com.dev.lang.semantic;

import com.dev.lang.ast.*;
import com.dev.lang.symbol.*;
import com.dev.lang.unit.CompilationException;
import com.dev.lang.unit.CompilationUnit;
import com.dev.lang.unit.WarningException;
import java.util.*;
import java.util.stream.Collectors;

public class ClassResolver implements StmtVisitor<Void>, ExprVisitor<Result> {
	private final SymbolTable symbolTable;
	private final CompilationUnit compilationUnit;
	private final Deque<Scope> scopeStack = new ArrayDeque<>();
	private final Map<Expr, Result> exprResults = new HashMap<>();
	private final TypeChecker typeChecker;
	private final TypeResolver typeResolver;

	private ClassOrTraitSymbol currentClassOrTrait;
	private FunSymbol currentFun;
	private boolean inLoop = false;
	private boolean verbose = true;

	public ClassResolver(SymbolTable symbolTable, CompilationUnit compilationUnit) {
		this.symbolTable = symbolTable;
		this.compilationUnit = compilationUnit;
		this.typeChecker = new TypeChecker(symbolTable);
		this.typeResolver = new TypeResolver(symbolTable);
		Scope globalScope = new Scope(null, "global", Scope.Kind.GLOBAL);
		scopeStack.push(globalScope);
	}

	private void log(String message) {
		if (verbose) {
			System.out.println("[ClassResolver] " + message);
		}
	}

	private void enterScope(String name, Scope.Kind kind) {
		Scope newScope = new Scope(currentScope(), name, kind);
		scopeStack.push(newScope);
	}

	private void enterScope(String name, Scope.Kind kind, Map<String, Symbol> substitution) {
		Scope newScope = new Scope(currentScope(), name, kind, substitution);
		scopeStack.push(newScope);
	}

	private void exitScope() {
		if (scopeStack.size() > 1) {
			scopeStack.pop();
		}
	}

	private Scope currentScope() {
		return scopeStack.peek();
	}

	private void define(Symbol symbol) {
		define(symbol, null);
	}

	private void define(Symbol symbol, Map<String, Symbol> substitution) {
		if (currentScope().contains(symbol.name)) {
			if (symbol.isClassOrTrait()) {
				error(symbol.name, SemanticErrorCode.REDEFINED_CLASS, symbol.name);
			} else if (symbol.isFun()) {
				error(symbol.name, SemanticErrorCode.REDEFINED_FUNCTION, symbol.name);
			} else if (symbol.isVar()) {
				error(symbol.name, SemanticErrorCode.REDEFINED_VARIABLE, symbol.name);
			} else if (symbol.isParam()) {
				error(symbol.name, SemanticErrorCode.REDEFINED_MEMBER, symbol.name);
			} else {
				error(symbol.name, SemanticErrorCode.REDEFINED_MEMBER, symbol.name);
			}
			return;
		}
		currentScope().define(symbol, substitution);
	}

	private Symbol resolve(String name) {
		return currentScope().resolve(name);
	}

	private Result getExprResult(Expr expr) {
		if (exprResults.containsKey(expr)) {
			return exprResults.get(expr);
		}
		Result result = expr.accept(this);
		if (result != null) {
			exprResults.put(expr, result);
		}
		return result;
	}

	private Typed getExprType(Expr expr) {
		Result result = getExprResult(expr);
		return result != null ? result.getType() : null;
	}

	private List<Typed> getArgumentTypes(List<Expr> args) {
		return args.stream()
			   .map(this::getExprType)
			   .filter(Objects::nonNull)
			   .collect(Collectors.toList());
	}

	private Typed getLiteralType(LiteralExpr expr) {
		return new RefTyped(expr.getTypeName(), expr.getPosition());
	}

	private Typed getBinaryType(BinaryExpr expr) {
		Typed left = getExprType(expr.left);
		Typed right = getExprType(expr.right);
		Operator op = expr.operator;
		Scope scope = currentScope();

		if (op.isArithmetic()) {
			if (typeChecker.isNumeric(left, scope) && typeChecker.isNumeric(right, scope)) {
				if (op.lexeme.equals("+") && (typeChecker.isString(left, scope) || typeChecker.isString(right, scope))) {
					return new RefTyped("String", expr.getPosition());
				}
				return typeChecker.getArithmeticResultType(left, right, scope);
			}
			if (op.lexeme.equals("+") && (typeChecker.isString(left, scope) || typeChecker.isString(right, scope))) {
				return new RefTyped("String", expr.getPosition());
			}
			error(expr, "Operator requires numeric operands");
			return null;
		}

		if (op.isNumericComparison()) {
			if (typeChecker.isNumeric(left, scope) && typeChecker.isNumeric(right, scope)) {
				return new RefTyped("Boolean", expr.getPosition());
			}
			error(expr, "Comparison operator requires numeric operands");
			return null;
		}

		if (op.isComparison()) {
			return new RefTyped("Boolean", expr.getPosition());
		}

		if (op.isLogical()) {
			if (typeChecker.isBoolean(left, scope) && typeChecker.isBoolean(right, scope)) {
				return new RefTyped("Boolean", expr.getPosition());
			}
			error(expr, "Logical operator requires boolean operands");
			return null;
		}

		return null;
	}

	private Typed getUnaryType(UnaryExpr expr) {
		Typed operand = getExprType(expr.operand);
		Operator op = expr.operator;
		Scope scope = currentScope();

		if (op.lexeme.equals("-")) {
			if (typeChecker.isNumeric(operand, scope)) {
				return operand;
			}
			error(expr, "Unary minus requires numeric operand");
			return null;
		}

		if (op.lexeme.equals("!")) {
			if (typeChecker.isBoolean(operand, scope)) {
				return new RefTyped("Boolean", expr.getPosition());
			}
			error(expr, "Logical NOT requires boolean operand");
			return null;
		}

		if (op.lexeme.equals("+")) {
			if (typeChecker.isNumeric(operand, scope)) {
				return operand;
			}
			error(expr, "Unary plus requires numeric operand");
			return null;
		}

		return null;
	}

	private Typed getTernaryType(TernaryExpr expr) {
		Typed condition = getExprType(expr.condition);
		Scope scope = currentScope();

		if (!typeChecker.isBoolean(condition, scope)) {
			error(expr.condition, "Ternary condition must be boolean");
		}

		Typed thenType = getExprType(expr.thenExpr);
		Typed elseType = getExprType(expr.elseExpr);

		if (thenType == null && elseType == null) return null;
		if (thenType == null) return elseType;
		if (elseType == null) return thenType;

		Typed common = typeChecker.getCommonSupertype(thenType, elseType, scope);
		if (common != null) {
			return common;
		}

		error(expr, "Ternary branches have incompatible types");
		return null;
	}

	private Map<String, Symbol> extractSubstitution(ParameterizedRefTyped paramType, ClassOrTraitSymbol base, Scope scope) {
		if (base == null || !base.isGeneric()) return null;
		if (paramType.typeArguments == null || paramType.typeArguments.isEmpty()) return null;

		List<String> typeParamNames = base.getTypeParameterTypes();
		if (typeParamNames.size() != paramType.typeArguments.size()) return null;

		Map<String, Symbol> substitution = new LinkedHashMap<>();
		for (int i = 0; i < typeParamNames.size(); i++) {
			Typed arg = paramType.typeArguments.get(i);
			Symbol argSym = typeResolver.resolve(arg, scope);
			if (argSym instanceof ClassOrTraitSymbol || argSym instanceof TypeParamSymbol) {
				substitution.put(typeParamNames.get(i), argSym);
			}
		}
		return substitution.isEmpty() ? null : substitution;
	}

	private Typed substitute(Typed type, Map<String, Symbol> substitution) {
		if (type == null || substitution == null || substitution.isEmpty()) return type;

		if (type instanceof RefTyped) {
			RefTyped ref = (RefTyped) type;
			Symbol sym = substitution.get(ref.name);
			if (sym instanceof ClassOrTraitSymbol) {
				return new RefTyped(sym.name, ref.getPosition());
			}
			if (sym instanceof TypeParamSymbol) {
				return new RefTyped(sym.name, ref.getPosition());
			}
			return ref;
		}

		if (type instanceof ParameterizedRefTyped) {
			ParameterizedRefTyped param = (ParameterizedRefTyped) type;
			List<Typed> newArgs = new ArrayList<>();
			for (Typed arg : param.typeArguments) {
				newArgs.add(substitute(arg, substitution));
			}
			return new ParameterizedRefTyped(param.name, newArgs, param.getPosition());
		}

		return type;
	}

	private Map<String, Symbol> createSubstitutionFromType(Typed type, Scope scope) {
		if (type == null) return null;

		Symbol typeSym = typeResolver.resolve(type, scope);
		if (!(typeSym instanceof ClassOrTraitSymbol)) return null;

		ClassOrTraitSymbol classSym = (ClassOrTraitSymbol) typeSym;
		if (!classSym.isGeneric()) return null;

		if (type instanceof ParameterizedRefTyped) {
			return extractSubstitution((ParameterizedRefTyped) type, classSym, scope);
		}

		return null;
	}

	private Result validateMethodCall(FunSymbol method, List<Expr> arguments, Node node, Map<String, Symbol> substitution) {
		if (method == null) return null;

		List<Typed> argTypes = getArgumentTypes(arguments);

		if (method.getArity() != arguments.size()) {
			error(node, SemanticErrorCode.ARITY_MISMATCH, method.getArity(), arguments.size());
			return null;
		}

		if (!typeChecker.isApplicable(method, argTypes, currentScope())) {
			error(node, SemanticErrorCode.TYPE_MISMATCH, "arguments", method.getParameterTypes());
			return null;
		}

		Typed returnType = substitute(method.getReturnType(), substitution);

		Map<String, Symbol> returnSubstitution = null;
		if (returnType instanceof ParameterizedRefTyped) {
			returnSubstitution = createSubstitutionFromType(returnType, currentScope());
		} else if (returnType instanceof RefTyped) {
			Symbol returnTypeSym = typeResolver.resolve(returnType, currentScope());
			if (returnTypeSym instanceof ClassOrTraitSymbol && ((ClassOrTraitSymbol) returnTypeSym).isGeneric()) {
				returnSubstitution = createSubstitutionFromType(returnType, currentScope());
			}
		}

		return new Result(returnType, returnSubstitution != null ? returnSubstitution : substitution, method);
	}

	private void error(Node node, SemanticErrorCode code, Object... args) {
		compilationUnit.addError(new CompilationException(code.format(args), node));
	}

	private void error(String message, SemanticErrorCode code, Object... args) {
		compilationUnit.addError(new CompilationException(code.format(args)));
	}

	private void error(Node node, String message) {
		compilationUnit.addError(new CompilationException(message, node));
	}

	private void warning(Node node, String code, String message) {
		compilationUnit.addWarning(new WarningException(code, message, node));
	}

	private void checkOrphanClasses() {
		compilationUnit.forEachClass((name, classDecl) -> {
			ClassOrTraitSymbol cs = symbolTable.get(name);
			if (cs != null && cs.isClass() && !cs.hasSuperclass() && !name.equals("Any")) {
				warning(classDecl, "ORPHAN_CLASS",
						"Class '" + name + "' has no explicit superclass (implicitly extends Any)");
			}
		});
	}

	private void checkCircularInheritance() {
		compilationUnit.forEachClass((name, classDecl) -> {
			ClassOrTraitSymbol cs = symbolTable.get(name);
			if (cs != null && hasCircularInheritance(cs, new HashSet<>())) {
				error(classDecl, SemanticErrorCode.CIRCULAR_INHERITANCE, name);
			}
		});
	}

	private boolean hasCircularInheritance(ClassOrTraitSymbol cs, Set<String> visited) {
		if (visited.contains(cs.name)) {
			log("Circular inheritance detected: " + cs.name);
			return true;
		}
		visited.add(cs.name);

		if (cs.hasSuperclass()) {
			ClassOrTraitSymbol superclass = cs.getSuperclass();
			if (superclass != null && !superclass.isTrait()) {
				if (hasCircularInheritance(superclass, visited)) return true;
			}
		}
		for (ClassOrTraitSymbol trait : cs.getSupertraits()) {
			if (trait != null && hasCircularInheritance(trait, visited)) return true;
		}
		return false;
	}

	private boolean hasReturnStatement(BlockStmt block) {
		for (Stmt stmt : block.statements) {
			if (stmt instanceof ReturnStmt) {
				return true;
			}
			if (stmt instanceof BlockStmt) {
				if (hasReturnStatement((BlockStmt) stmt)) {
					return true;
				}
			}
			if (stmt instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) stmt;
				if (hasReturnStatement(ifStmt.thenBranch) &&
						ifStmt.elseBranch != null &&
						hasReturnStatement(ifStmt.elseBranch)) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkRequiredFunctions(ClassOrTraitSymbol classSymbol) {
		log("Checking required functions for class: " + classSymbol.name);
		Set<String> requiredMethods = new HashSet<>();
		Map<String, FunSymbol> requiredMethodSignatures = new LinkedHashMap<>();

		for (ClassOrTraitSymbol trait : classSymbol.getSupertraits()) {
			log("  Supertrait: " + trait.name);
			for (FunSymbol method : trait.getAllFuns().values()) {
				if (method.modifier.isShared()) continue;
				String signature = method.name + "(" + method.getArity() + ")";
				if (!requiredMethods.contains(signature)) {
					requiredMethods.add(signature);
					requiredMethodSignatures.put(method.name, method);
					log("    Required (from trait): " + method.getSignature());
				}
			}
		}

		if (classSymbol.hasSuperclass() && classSymbol.getSuperclass().isTrait()) {
			ClassOrTraitSymbol superTrait = classSymbol.getSuperclass();
			log("  Superclass is trait (error case): " + superTrait.name);
			for (FunSymbol method : superTrait.getAllFuns().values()) {
				if (method.modifier.isShared()) continue;
				String signature = method.name + "(" + method.getArity() + ")";
				if (!requiredMethods.contains(signature)) {
					requiredMethods.add(signature);
					requiredMethodSignatures.put(method.name, method);
					log("    Required (from super-trait): " + method.getSignature());
				}
			}
		}

		for (Map.Entry<String, FunSymbol> entry : requiredMethodSignatures.entrySet()) {
			String methodName = entry.getKey();
			FunSymbol requiredMethod = entry.getValue();
			FunSymbol implementedMethod = classSymbol.getDeclaredFun(methodName);

			if (implementedMethod == null) {
				log("  Missing method: " + methodName);
				error("Class '" + classSymbol.name + "' must implement method '" +
					  methodName + "' from trait", SemanticErrorCode.MISSING_IMPLEMENTATION, methodName);
			} else if (!typeChecker.canOverride(implementedMethod, requiredMethod, currentScope())) {
				log("  Invalid override for: " + methodName);
				error("Method '" + methodName + "' in class '" + classSymbol.name +
					  "' does not properly override the method from trait",
					  SemanticErrorCode.INVALID_OVERRIDE, methodName);
			} else {
				log("  Method " + methodName + " implemented");
			}
		}
	}

	public void resolveAll() {
		log("=== RESOLVING CLASSES ===");
		compilationUnit.forEachClass((name, classDecl) -> {
			try {
				log("Resolving class: " + name);
				classDecl.accept(this);
			} catch (Exception e) {
				System.err.println("Erro ao resolver classe '" + name + "': " + e.getMessage());
				e.printStackTrace();
				compilationUnit.addError(new CompilationException("Erro interno: " + e.getMessage(), classDecl, e));
			}
		});
		checkOrphanClasses();
		checkCircularInheritance();
	}

	// ==================== StmtVisitor Implementation ====================

	@Override
	public Void visitClassOrTraitDeclStmt(ClassOrTraitDeclStmt stmt) {
		log("Visiting ClassOrTraitDeclStmt: " + stmt.name);

		ClassOrTraitSymbol classSymbol = symbolTable.get(stmt.name);
		if (classSymbol == null) {
			error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.name);
			return null;
		}

		// Criar TypeParamSymbol a partir dos nomes
		List<String> typeParamNames = classSymbol.getTypeParameterTypes();
		for (int i = 0; i < typeParamNames.size(); i++) {
			String name = typeParamNames.get(i);
			TypeParamSymbol tp = new TypeParamSymbol(name, i);
			classSymbol.addTypeParameter(tp);
		}

		Map<String, Symbol> pendingTypeParams = new HashMap<>();
		for (TypeParamSymbol tp : classSymbol.getTypeParameters()) {
			pendingTypeParams.put(tp.name, tp);
		}
		log("Pending type params: " + pendingTypeParams.keySet());

		// Resolver superclass
		Typed superclassType = classSymbol.getSuperclassType();
		if (superclassType != null) {
			Symbol resolved = typeResolver.resolve(superclassType, currentScope(), pendingTypeParams);
			if (resolved instanceof ClassOrTraitSymbol) {
				ClassOrTraitSymbol superclass = (ClassOrTraitSymbol) resolved;
				if (superclass.isTrait()) {
					error(stmt, SemanticErrorCode.CANNOT_INHERIT_FROM_FINAL,
						  "Cannot extend a trait '" + superclass.name + "'. Use '|' for traits.");
				} else if (superclass.name.equals(stmt.name)) {
					error(stmt, SemanticErrorCode.CIRCULAR_INHERITANCE, stmt.name);
				} else {
					classSymbol.setSuperclass(superclass);
					log("  Superclass: " + superclass.name);
				}
			} else {
				error(stmt, SemanticErrorCode.UNDEFINED_CLASS, superclassType.getName());
			}
		}

		// Resolver supertraits
		List<Typed> supertraitTypes = classSymbol.getSupertraitTypes();
		if (!supertraitTypes.isEmpty()) {
			List<ClassOrTraitSymbol> supertraits = new ArrayList<>();
			for (Typed traitType : supertraitTypes) {
				Symbol resolved = typeResolver.resolve(traitType, currentScope(), pendingTypeParams);
				if (resolved instanceof ClassOrTraitSymbol) {
					ClassOrTraitSymbol traitSymbol = (ClassOrTraitSymbol) resolved;
					if (!traitSymbol.isTrait()) {
						error(stmt, SemanticErrorCode.CANNOT_INHERIT_FROM_FINAL,
							  "Expected trait but found class '" + traitSymbol.name + "'");
					} else {
						supertraits.add(traitSymbol);
						log("  Supertrait: " + traitSymbol.name);
					}
				} else {
					error(stmt, SemanticErrorCode.UNDEFINED_CLASS, traitType.getName());
				}
			}
			classSymbol.setSupertraits(supertraits);
		}

		// Se for classe e não tiver supertype, setar Any como superclasse
		if (classSymbol.isClass() && !classSymbol.hasSuperclass() && !classSymbol.name.equals("Any")) {
			ClassOrTraitSymbol anyClass = symbolTable.get("Any");
			if (anyClass != null) {
				classSymbol.setSuperclass(anyClass);
				log("  Set superclass to Any for class: " + stmt.name);
			}
		}

		currentClassOrTrait = classSymbol;

		Map<String, Symbol> typeParamSubstitution = new HashMap<>();
		for (TypeParamSymbol tp : classSymbol.getTypeParameters()) {
			typeParamSubstitution.put(tp.name, tp);
		}
		enterScope(stmt.name, stmt.isClass() ? Scope.Kind.CLASS : Scope.Kind.TRAIT, typeParamSubstitution);

		for (TypeParamSymbol tp : classSymbol.getTypeParameters()) {
			define(tp);
		}

		for (MemberDeclStmt member : stmt.members) {
			if (member != null) {
				member.accept(this);
			}
		}

		if (classSymbol.isClass()) {
			checkRequiredFunctions(classSymbol);
		}

		exitScope();
		currentClassOrTrait = null;
		log("Finished resolving: " + stmt.name);

		return null;
	}

	@Override
	public Void visitFunDeclStmt(FunDeclStmt stmt) {
		log("Visiting FunDeclStmt: " + stmt.name);

		FunSymbol funSymbol = null;

		if (currentClassOrTrait != null) {
			funSymbol = currentClassOrTrait.getDeclaredFun(stmt.name);
		}

		if (funSymbol == null) {
			log("  Creating new FunSymbol: " + stmt.name);

			List<ParamSymbol> params = new ArrayList<>();
			for (int i = 0; i < stmt.parameters.size(); i++) {
				ParamDeclStmt param = stmt.parameters.get(i);
				Typed paramType = null;
				if (param.hasType()) {
					Symbol typeSymbol = typeResolver.resolve(param.type, currentScope());
					if (typeSymbol != null && typeSymbol.isType()) {
						paramType = param.type;
					} else {
						error(param, SemanticErrorCode.UNDEFINED_CLASS, param.type.getName());
					}
				}
				params.add(new ParamSymbol(param.name, paramType, i));
			}

			Typed returnType = null;
			if (stmt.hasReturnType()) {
				Symbol typeSymbol = typeResolver.resolve(stmt.returnType, currentScope());
				if (typeSymbol != null && typeSymbol.isType()) {
					returnType = stmt.returnType;
				} else {
					error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.returnType.getName());
				}
			} else {
				returnType = new RefTyped("Void", stmt.getPosition());
				log("  No return type specified, using Void");
			}

			funSymbol = new FunSymbol(stmt.name, params, returnType, currentClassOrTrait);
			funSymbol.modifier = stmt.modifier;
			funSymbol.setBody(stmt.body);

			if (currentClassOrTrait != null) {
				currentClassOrTrait.addMember(funSymbol);
			}
			define(funSymbol);
		} else {
			log("  Updating existing FunSymbol: " + stmt.name);

			List<ParamSymbol> existingParams = funSymbol.getParameters();
			for (int i = 0; i < stmt.parameters.size() && i < existingParams.size(); i++) {
				ParamDeclStmt param = stmt.parameters.get(i);
				ParamSymbol paramSym = existingParams.get(i);

				if (param.hasType() && paramSym.getParamType() == null) {
					Symbol typeSymbol = typeResolver.resolve(param.type, currentScope());
					if (typeSymbol != null && typeSymbol.isType()) {
						paramSym.setParamType(param.type);
						log("    Updated param type: " + param.name + " : " + param.type.getName());
					} else {
						error(param, SemanticErrorCode.UNDEFINED_CLASS, param.type.getName());
					}
				}
			}

			if (stmt.hasReturnType() && funSymbol.getReturnType() == null) {
				Symbol typeSymbol = typeResolver.resolve(stmt.returnType, currentScope());
				if (typeSymbol != null && typeSymbol.isType()) {
					funSymbol.setReturnType(stmt.returnType);
					log("    Updated return type: " + stmt.returnType.getName());
				} else {
					error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.returnType.getName());
				}
			} else if (!stmt.hasReturnType() && funSymbol.getReturnType() == null) {
				funSymbol.setReturnType(new RefTyped("Void", stmt.getPosition()));
				log("  No return type specified, using Void");
			}

			funSymbol.setBody(stmt.body);
		}

		currentFun = funSymbol;

		enterScope(stmt.name, Scope.Kind.FUN);

		for (ParamSymbol param : funSymbol.getParameters()) {
			define(param);
		}

		if (stmt.body != null) {
			stmt.body.accept(this);
			if (funSymbol.getReturnType() != null &&
					!funSymbol.getReturnType().getName().equals("Void") &&
					!hasReturnStatement(stmt.body)) {
				warning(stmt, "MISSING_RETURN",
						"Function '" + stmt.name + "' with return type '" +
						funSymbol.getReturnType().getName() +
						"' may not return a value on all paths");
			}
		}

		exitScope();
		currentFun = null;

		return null;
	}

	@Override
	public Void visitVarDeclStmt(VarDeclStmt stmt) {
		log("Visiting VarDeclStmt: " + stmt.name);

		if (currentScope().contains(stmt.name)) {
			error(stmt, SemanticErrorCode.REDEFINED_VARIABLE, stmt.name);
			return null;
		}

		Typed varType = null;
		Map<String, Symbol> substitution = null;

		if (stmt.hasType()) {
			Symbol typeSym = typeResolver.resolve(stmt.getType(), currentScope());
			if (typeSym == null) {
				error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.getType().getName());
				return null;
			}

			varType = stmt.getType();
			if (stmt.getType() instanceof ParameterizedRefTyped && typeSym instanceof ClassOrTraitSymbol) {
				substitution = extractSubstitution((ParameterizedRefTyped) stmt.getType(),
												   (ClassOrTraitSymbol) typeSym, currentScope());
			}
		}

		VarSymbol varSymbol = new VarSymbol(stmt.name, varType);
		varSymbol.modifier = stmt.modifier;

		if (stmt.getValue() != null) {
			Result valueResult = stmt.getValue().accept(this);
			varSymbol.setInitialized(true);

			if (varType != null && valueResult != null && valueResult.getType() != null) {
				if (!typeChecker.isAssignable(valueResult.getType(), varType, currentScope())) {
					error(stmt, SemanticErrorCode.TYPE_MISMATCH, varType.getName(), valueResult.getType().getName());
					return null;
				}
			}

			if (substitution == null && valueResult != null && valueResult.getSubstitution() != null) {
				substitution = valueResult.getSubstitution();
			}
		}

		define(varSymbol, substitution);

		if (currentClassOrTrait != null) {
			currentClassOrTrait.addMember(varSymbol);
		}

		return null;
	}

	@Override
	public Void visitLetDeclStmt(LetDeclStmt stmt) {
		if (currentScope().contains(stmt.name)) {
			error(stmt, SemanticErrorCode.REDEFINED_VARIABLE, stmt.name);
			return null;
		}

		if (stmt.value == null) {
			error(stmt, SemanticErrorCode.MISSING_RETURN);
			return null;
		}

		Typed varType = null;
		Map<String, Symbol> substitution = null;

		if (stmt.type != null) {
			Symbol typeSym = typeResolver.resolve(stmt.type, currentScope());
			if (typeSym == null) {
				error(stmt, SemanticErrorCode.UNDEFINED_CLASS, stmt.type.getName());
				return null;
			}

			varType = stmt.type;
			if (stmt.type instanceof ParameterizedRefTyped && typeSym instanceof ClassOrTraitSymbol) {
				substitution = extractSubstitution((ParameterizedRefTyped) stmt.type,
												   (ClassOrTraitSymbol) typeSym, currentScope());
			}
		}

		Result valueResult = stmt.value.accept(this);

		if (varType == null && valueResult != null && valueResult.getType() != null) {
			varType = valueResult.getType();
			substitution = valueResult.getSubstitution();
		}

		if (varType != null && valueResult != null && valueResult.getType() != null) {
			if (!typeChecker.isAssignable(valueResult.getType(), varType, currentScope())) {
				error(stmt, SemanticErrorCode.TYPE_MISMATCH, varType.getName(), valueResult.getType().getName());
				return null;
			}
		}

		if (substitution == null && valueResult != null && valueResult.getSubstitution() != null) {
			substitution = valueResult.getSubstitution();
		}

		VarSymbol varSymbol = new VarSymbol(stmt.name, varType);
		define(varSymbol, substitution);
		varSymbol.setInitialized(true);

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
		enterScope("block", Scope.Kind.BLOCK);
		for (Stmt s : stmt.statements) {
			if (s != null) {
				s.accept(this);
			}
		}
		exitScope();
		return null;
	}

	@Override
	public Void visitIfStmt(IfStmt stmt) {
		stmt.condition.accept(this);
		enterScope("if-then", Scope.Kind.BLOCK);
		stmt.thenBranch.accept(this);
		exitScope();
		if (stmt.elseBranch != null) {
			enterScope("if-else", Scope.Kind.BLOCK);
			stmt.elseBranch.accept(this);
			exitScope();
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(WhileStmt stmt) {
		stmt.condition.accept(this);
		boolean previousInLoop = inLoop;
		inLoop = true;
		enterScope("while", Scope.Kind.LOOP);
		stmt.body.accept(this);
		exitScope();
		inLoop = previousInLoop;
		return null;
	}

	@Override
	public Void visitReturnStmt(ReturnStmt stmt) {
		log("Visiting ReturnStmt");

		if (currentFun == null) {
			error(stmt, SemanticErrorCode.MISSING_RETURN);
			return null;
		}

		if (stmt.value != null) {
			Result valueResult = stmt.value.accept(this);
			Typed expectedReturnType = currentFun.getReturnType();

			if (expectedReturnType == null) {
				error(stmt, SemanticErrorCode.TYPE_MISMATCH, "void",
					  valueResult != null ? valueResult.getType().getName() : "value");
			} else if (valueResult != null) {
				if (!typeChecker.isAssignable(valueResult.getType(), expectedReturnType, currentScope())) {
					error(stmt, SemanticErrorCode.TYPE_MISMATCH,
						  expectedReturnType.getName(), valueResult.getType().getName());
				}
			}
		} else if (currentFun.getReturnType() != null && !currentFun.getReturnType().getName().equals("Void")) {
			error(stmt, SemanticErrorCode.MISSING_RETURN);
		}

		return null;
	}

	@Override
	public Void visitBreakStmt(BreakStmt stmt) {
		if (!inLoop) {
			error(stmt, SemanticErrorCode.UNREACHABLE_CODE);
		}
		return null;
	}

	@Override
	public Void visitContinueStmt(ContinueStmt stmt) {
		if (!inLoop) {
			error(stmt, SemanticErrorCode.UNREACHABLE_CODE);
		}
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt stmt) {
		stmt.expression.accept(this);
		return null;
	}

	// ==================== ExprVisitor Implementation ====================

	@Override
	public Result visitLiteralExpr(LiteralExpr expr) {
		return new Result(getLiteralType(expr));
	}

	@Override
	public Result visitRefExpr(RefExpr expr) {
		Symbol symbol = resolve(expr.name);

		if (symbol == null && currentClassOrTrait != null) {
			symbol = currentClassOrTrait.getMember(expr.name);
		}

		if (symbol == null) {
			ClassOrTraitSymbol classSymbol = symbolTable.get(expr.name);
			if (classSymbol != null) {
				log("  Found as class/trait: " + expr.name);
				Map<String, Symbol> substitution = createSubstitutionFromType(new RefTyped(classSymbol.name, expr.getPosition()), currentScope());
				return new Result(new RefTyped(classSymbol.name, expr.getPosition()), substitution, classSymbol);
			}
		}

		if (symbol == null) {
			ClassOrTraitSymbol anyClass = symbolTable.getAny();
			if (anyClass != null) {
				Symbol anyMember = anyClass.getMember(expr.name);
				if (anyMember != null && anyMember.isShared()) {
					log("  Found as shared member of Any: " + expr.name);
					symbol = anyMember;
				}
			}
		}

		if (symbol == null) {
			error(expr, SemanticErrorCode.UNDEFINED_VARIABLE, expr.name);
			return null;
		}

		Map<String, Symbol> substitution = currentScope().getSymbolSubstitution(expr.name);
		if (substitution == null && currentClassOrTrait != null) {
			substitution = currentScope().getScopeSubstitution();
		}

		if (symbol.isVar()) {
			Typed varType = substitute(((VarSymbol) symbol).getVarType(), substitution);
			Map<String, Symbol> varSubstitution = null;
			if (varType instanceof ParameterizedRefTyped) {
				varSubstitution = createSubstitutionFromType(varType, currentScope());
			}
			return new Result(varType, varSubstitution != null ? varSubstitution : substitution, symbol);
		}

		if (symbol.isFun()) {
			Typed returnType = substitute(((FunSymbol) symbol).getReturnType(), substitution);
			Map<String, Symbol> returnSubstitution = null;
			if (returnType instanceof ParameterizedRefTyped) {
				returnSubstitution = createSubstitutionFromType(returnType, currentScope());
			}
			return new Result(returnType, returnSubstitution != null ? returnSubstitution : substitution, symbol);
		}

		if (symbol.isParam()) {
			Typed paramType = substitute(((ParamSymbol) symbol).getParamType(), substitution);
			return new Result(paramType, substitution, symbol);
		}

		if (symbol.isClassOrTrait()) {
			Map<String, Symbol> classSubstitution = createSubstitutionFromType(new RefTyped(symbol.name, expr.getPosition()), currentScope());
			return new Result(new RefTyped(symbol.name, expr.getPosition()), classSubstitution, symbol);
		}

		if (symbol.isTypeParam()) {
			return new Result(new RefTyped(symbol.name, expr.getPosition()), substitution, symbol);
		}

		return null;
	}

	@Override
	public Result visitAssignExpr(AssignExpr expr) {
		Result targetResult = expr.target.accept(this);
		Result valueResult = expr.value.accept(this);

		if (targetResult != null && valueResult != null) {
			if (!typeChecker.isAssignable(valueResult.getType(), targetResult.getType(), currentScope())) {
				error(expr, "Cannot assign '" + valueResult.getType().getName() + "' to '" + targetResult.getType().getName() + "'");
			}
		}

		if (expr.target instanceof RefExpr) {
			String name = ((RefExpr) expr.target).name;
			Symbol symbol = resolve(name);
			if (symbol == null) {
				error(expr, SemanticErrorCode.UNDEFINED_VARIABLE, name);
			} else if (symbol.isReadonly() && currentClassOrTrait != null) {
				warning(expr, "ASSIGN_TO_READONLY", "Cannot reassign readonly variable '" + name + "'");
			}
		}

		return targetResult;
	}

	@Override
	public Result visitBinaryExpr(BinaryExpr expr) {
		return new Result(getBinaryType(expr));
	}

	@Override
	public Result visitUnaryExpr(UnaryExpr expr) {
		return new Result(getUnaryType(expr));
	}

	@Override
	public Result visitTernaryExpr(TernaryExpr expr) {
		return new Result(getTernaryType(expr));
	}

	@Override
	public Result visitCallExpr(CallExpr expr) {
		log("Visiting CallExpr");

		Result calleeResult = expr.callee.accept(this);

		if (calleeResult == null) {
			for (Expr arg : expr.arguments) {
				arg.accept(this);
			}
			return null;
		}

		Symbol calleeSym = calleeResult.getSymbol();

		if (calleeSym == null) {
			error(expr, "Expression is not callable");
			return null;
		}

		FunSymbol function = null;
		Map<String, Symbol> substitution = calleeResult.getSubstitution();

		if (calleeSym.isFun()) {
			function = (FunSymbol) calleeSym;
		} else if (calleeSym.isClassOrTrait()) {
			ClassOrTraitSymbol classSym = (ClassOrTraitSymbol) calleeSym;
			if (classSym.isTrait()) {
				error(expr, SemanticErrorCode.CANNOT_INSTANTIATE_ABSTRACT, classSym.name);
				return null;
			}
			function = classSym.getFun("init");
			if (function == null && !expr.arguments.isEmpty()) {
				error(expr, SemanticErrorCode.UNDEFINED_FUNCTION,
					  "Constructor for " + classSym.name + " with " + expr.arguments.size() + " arguments");
				return null;
			}
		} else {
			error(expr, SemanticErrorCode.UNDEFINED_FUNCTION, calleeSym.name);
			return null;
		}

		if (function == null) {
			if (expr.arguments.isEmpty()) {
				return new Result(calleeResult.getType(), substitution, calleeSym);
			}
			error(expr, SemanticErrorCode.UNDEFINED_FUNCTION, "callable");
			return null;
		}

		return validateMethodCall(function, expr.arguments, expr, substitution);
	}

	@Override
	public Result visitMemberAccessExpr(MemberAccessExpr expr) {
		log("Visiting MemberAccessExpr: member=" + expr.name);

		Result objectResult = expr.object.accept(this);

		if (objectResult == null) {
			error(expr, SemanticErrorCode.UNDEFINED_MEMBER, expr.name);
			return null;
		}

		Typed objectType = objectResult.getType();
		if (objectType == null) {
			error(expr, SemanticErrorCode.UNDEFINED_MEMBER, expr.name);
			return null;
		}

		Symbol typeSym = typeResolver.resolve(objectType, currentScope());

		if (!(typeSym instanceof ClassOrTraitSymbol)) {
			error(expr, SemanticErrorCode.UNDEFINED_MEMBER, expr.name);
			return null;
		}

		ClassOrTraitSymbol cls = (ClassOrTraitSymbol) typeSym;
		Symbol member = cls.getMember(expr.name);

		if (member == null) {
			error(expr, SemanticErrorCode.UNDEFINED_MEMBER, expr.name);
			return null;
		}

		Map<String, Symbol> substitution = objectResult.getSubstitution();

		if (substitution == null && objectType instanceof ParameterizedRefTyped) {
			substitution = extractSubstitution((ParameterizedRefTyped) objectType, cls, currentScope());
		}

		if (member.isShared() || !(objectResult.getSymbol() instanceof ClassOrTraitSymbol)) {
			if (member.isVar()) {
				Typed varType = substitute(((VarSymbol) member).getVarType(), substitution);
				Map<String, Symbol> varSubstitution = null;
				if (varType instanceof ParameterizedRefTyped) {
					varSubstitution = createSubstitutionFromType(varType, currentScope());
				}
				return new Result(varType, varSubstitution != null ? varSubstitution : substitution, member);
			}

			if (member.isFun()) {
				Typed returnType = substitute(((FunSymbol) member).getReturnType(), substitution);
				Map<String, Symbol> returnSubstitution = null;
				if (returnType instanceof ParameterizedRefTyped) {
					returnSubstitution = createSubstitutionFromType(returnType, currentScope());
				}
				return new Result(returnType, returnSubstitution != null ? returnSubstitution : substitution, member);
			}

			return new Result(new RefTyped(member.name, expr.getPosition()), substitution, member);
		}

		error(expr, "Member '" + expr.name + "' is not shared. Create an instance first");
		return null;
	}

	@Override
	public Result visitThisExpr(ThisExpr expr) {
		if (currentClassOrTrait == null) {
			error(expr, SemanticErrorCode.INVALID_ACCESS, "this");
			return null;
		}

		Map<String, Symbol> substitution = currentScope().getScopeSubstitution();
		Map<String, Symbol> classSubstitution = createSubstitutionFromType(new RefTyped(currentClassOrTrait.name, expr.getPosition()), currentScope());
		return new Result(new RefTyped(currentClassOrTrait.name, expr.getPosition()),
						  classSubstitution != null ? classSubstitution : substitution, currentClassOrTrait);
	}

	@Override
	public Result visitSuperExpr(SuperExpr expr) {
		if (currentClassOrTrait == null) {
			error(expr, SemanticErrorCode.INVALID_ACCESS, "super");
			return null;
		}
		if (currentClassOrTrait.getSuperclass() == null) {
			error(expr, SemanticErrorCode.CANNOT_INHERIT_FROM_FINAL, "super");
			return null;
		}

		Map<String, Symbol> substitution = currentScope().getScopeSubstitution();
		ClassOrTraitSymbol superclass = currentClassOrTrait.getSuperclass();
		Map<String, Symbol> superSubstitution = createSubstitutionFromType(new RefTyped(superclass.name, expr.getPosition()), currentScope());

		return new Result(new RefTyped(superclass.name, expr.getPosition()),
						  superSubstitution != null ? superSubstitution : substitution, superclass);
	}

	@Override
	public Result visitNewExpr(NewExpr expr) {
		String className = expr.type.getName();

		Symbol baseSym = typeResolver.resolve(expr.type, currentScope());

		if (!(baseSym instanceof ClassOrTraitSymbol)) {
			error(expr, SemanticErrorCode.UNDEFINED_CLASS, className);
			return null;
		}

		ClassOrTraitSymbol targetClass = (ClassOrTraitSymbol) baseSym;

		if (targetClass.isTrait()) {
			error(expr, SemanticErrorCode.CANNOT_INSTANTIATE_ABSTRACT, className);
			return null;
		}

		Map<String, Symbol> substitution = null;
		if (expr.type instanceof ParameterizedRefTyped) {
			substitution = extractSubstitution((ParameterizedRefTyped) expr.type, targetClass, currentScope());
		}

		FunSymbol constructor = targetClass.getFun("init");

		if (constructor == null) {
			if (!expr.arguments.isEmpty()) {
				error(expr, SemanticErrorCode.UNDEFINED_FUNCTION,
					  "Constructor for " + className + " with " + expr.arguments.size() + " arguments");
			}
			Map<String, Symbol> resultSubstitution = substitution != null ? substitution :
					createSubstitutionFromType(expr.type, currentScope());
			return new Result(new RefTyped(targetClass.name, expr.getPosition()), resultSubstitution, targetClass);
		}

		List<Typed> argTypes = getArgumentTypes(expr.arguments);
		if (!typeChecker.isApplicable(constructor, argTypes, currentScope())) {
			error(expr, SemanticErrorCode.TYPE_MISMATCH, "constructor arguments", "parameters");
			return new Result(new RefTyped(targetClass.name, expr.getPosition()), substitution, targetClass);
		}

		if (constructor.getArity() != expr.arguments.size()) {
			error(expr, SemanticErrorCode.ARITY_MISMATCH,
				  constructor.getArity(), expr.arguments.size());
			return new Result(new RefTyped(targetClass.name, expr.getPosition()), substitution, targetClass);
		}

		List<ParamSymbol> params = constructor.getParameters();
		for (int i = 0; i < expr.arguments.size(); i++) {
			Expr arg = expr.arguments.get(i);
			Result argResult = arg.accept(this);
			Typed paramType = params.get(i).getParamType();

			if (argResult == null) {
				error(arg, SemanticErrorCode.TYPE_MISMATCH,
					  paramType != null ? paramType.getName() : "?", "null");
				continue;
			}

			if (paramType != null) {
				Typed substitutedParamType = substitute(paramType, substitution);
				if (!typeChecker.isAssignable(argResult.getType(), substitutedParamType, currentScope())) {
					error(arg, SemanticErrorCode.TYPE_MISMATCH,
						  substitutedParamType.getName(), argResult.getType().getName());
				}
			}
		}

		Map<String, Symbol> resultSubstitution = substitution != null ? substitution :
				createSubstitutionFromType(expr.type, currentScope());

		return new Result(new RefTyped(targetClass.name, expr.getPosition()), resultSubstitution, targetClass);
	}
}