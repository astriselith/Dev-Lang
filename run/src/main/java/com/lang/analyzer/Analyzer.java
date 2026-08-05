package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class Analyzer implements DefaultDeclVisitor<Void>, DefaultStmtVisitor<Void>, DefaultExprVisitor<SymbolResult> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private final SymbolScope global;
    private final Stack<SymbolScope> scopeStack = new Stack<>();
    private final Stack<Map<String, Symbol>> substitutionStack = new Stack<>();
    private final Consumer<Typed> reporter;

    private ClassSymbol currentClass;
    private FunSymbol currentFun;

    public Analyzer(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.global = new SymbolScope("global");
        this.reporter = t -> unit.addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t));
    }

    public void analyze() {
        scopeStack.push(global);
        substitutionStack.push(new LinkedHashMap<>());
        unit.classes.forEach(cls -> cls.accept(this));
    }

    private void pushScope(SymbolScope scope) {
        scopeStack.push(scope);
    }

    private void popScope() {
        scopeStack.pop();
    }

    private void pushSubstitutions(Map<String, Symbol> subs) {
        Map<String, Symbol> newSubs = new LinkedHashMap<>(substitutionStack.peek());
        newSubs.putAll(subs);
        substitutionStack.push(newSubs);
    }

    private void popSubstitutions() {
        substitutionStack.pop();
    }

    private SymbolScope currentScope() {
        return scopeStack.peek();
    }

    private Map<String, Symbol> currentSubstitutions() {
        return substitutionStack.peek();
    }

    @Override
    public Void visitClassDecl(ClassDecl decl) {
        currentClass = table.get(decl.name.source);
        if (currentClass == null)
            return null;

        SymbolScope classScope = currentScope().child(decl.name.source);
        classScope.putLocal("this", currentClass);

        Map<String, Symbol> subs = new LinkedHashMap<>();
        subs.put(currentClass.getName(), currentClass);
        subs.putAll(currentClass.getTypeParameters());

        pushScope(classScope);
        pushSubstitutions(subs);

        if (decl.members != null) {
            decl.members.forEach(member -> member.accept(this));
        }

        popSubstitutions();
        popScope();
        currentClass = null;
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl decl) {
        if (currentClass == null)
            return null;

        currentFun = (FunSymbol) currentClass.getDeclaredMember(decl.name.source);
        if (currentFun == null)
            return null;

        SymbolScope funScope = currentScope().child(decl.name.source);

        Map<String, Symbol> subs = new LinkedHashMap<>();
        if (currentFun.getTypeParameters() != null) {
            subs.putAll(currentFun.getTypeParameters());
        }

        // Adiciona parâmetros da função ao escopo
        if (currentFun.getParameters() != null) {
            for (ParamSymbol param : currentFun.getParameters().values()) {
                funScope.putLocal(param.getName(), param);
            }
        }

        pushScope(funScope);
        pushSubstitutions(subs);

        if (decl.parameters != null) {
            decl.parameters.forEach(p -> p.accept(this));
        }

        if (decl.body != null) {
            decl.body.accept(this);
        }

        popSubstitutions();
        popScope();
        currentFun = null;
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl decl) {
        VarSymbol varSymbol = (VarSymbol) currentClass.getDeclaredMember(decl.name.source);
        if (varSymbol == null) {
            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(decl.name.source), decl));
            return null;
        }

        if (decl.value != null) {
            SymbolResult result = decl.value.accept(this);
            Symbol valueType = result != null ? result.getType() : null;
            Symbol varType = varSymbol.getType();

            if (varType != null && valueType != null) {
                if (!table.isAssignable(valueType, varType)) {
                    unit.addError(unit.error(TAG,
                            INVALID_ASSIGNMENT.format(
                                    getTypeName(valueType),
                                    getTypeName(varType)),
                            decl.value));
                }
            }
        }

        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl decl) {
        // Parâmetros já são registrados no escopo em visitFunDecl
        return null;
    }

    @Override
    public Void visitLetStmt(LetStmt stmt) {
        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            Symbol valueType = result != null ? result.getType() : null;

            if (stmt.type != null) {
                Symbol declaredType = table.resolve(stmt.type, currentSubstitutions(), reporter);

                if (declaredType != null && valueType != null) {
                    if (!table.isAssignable(valueType, declaredType)) {
                        unit.addError(unit.error(TAG,
                                INVALID_ASSIGNMENT.format(
                                        getTypeName(valueType),
                                        getTypeName(declaredType)),
                                stmt.value));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(BlockStmt stmt) {
        SymbolScope blockScope = currentScope().child("block");
        pushScope(blockScope);

        if (stmt.statements != null) {
            stmt.statements.forEach(s -> s.accept(this));
        }

        popScope();
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmt stmt) {
        if (stmt.condition != null) {
            SymbolResult result = stmt.condition.accept(this);
            Symbol condType = result != null ? result.getType() : null;
            if (condType != null && !table.isBool(condType)) {
                unit.addError(unit.error(TAG,
                        "Condition must be Bool, got " + getTypeName(condType),
                        stmt.condition));
            }
        }
        if (stmt.thenBranch != null)
            stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null)
            stmt.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt stmt) {
        if (stmt.condition != null) {
            SymbolResult result = stmt.condition.accept(this);
            Symbol condType = result != null ? result.getType() : null;
            if (condType != null && !table.isBool(condType)) {
                unit.addError(unit.error(TAG,
                        "Condition must be Bool, got " + getTypeName(condType),
                        stmt.condition));
            }
        }
        if (stmt.body != null)
            stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt stmt) {
        if (currentFun == null) {
            unit.addError(unit.error(TAG, "Return outside function", stmt));
            return null;
        }

        Symbol returnType = currentFun.getReturnType();

        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            Symbol valueType = result != null ? result.getType() : null;

            if (returnType != null && valueType != null) {
                if (!table.isAssignable(valueType, returnType)) {
                    unit.addError(unit.error(TAG,
                            INVALID_RETURN_TYPE.format(
                                    currentFun.getName(),
                                    getTypeName(returnType),
                                    getTypeName(valueType)),
                            stmt.value));
                }
            }
        } else {
            if (returnType != null && !table.isVoid(returnType)) {
                unit.addError(unit.error(TAG,
                        "Function '" + currentFun.getName() +
                                "' expects return type " + getTypeName(returnType) +
                                " but no value returned",
                        stmt));
            }
        }
        return null;
    }

    @Override
    public SymbolResult visitRefExpr(RefExpr expr) {
        Symbol symbol = currentScope().resolve(expr.name.source);

        if (symbol == null) {
            if (currentClass != null) {
                MemberSymbol member = currentClass.getMember(expr.name.source);
                if (member != null) {
                    Symbol type = table.unwrap(member);
                    return SymbolResult.of(member, type);
                }
            }

            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(expr.name.source), expr));
            return null;
        }

        Symbol type = table.unwrap(symbol);
        return SymbolResult.of(symbol, type);
    }

    @Override
    public SymbolResult visitMemberAccessExpr(MemberAccessExpr expr) {
        SymbolResult result = expr.object.accept(this);
        if (result == null)
            return null;

        Symbol objectType = result.getType();
        if (objectType == null)
            return null;

        ClassSymbol classSymbol = table.getBaseClass(objectType);
        if (classSymbol == null) {
            unit.addError(unit.error(TAG,
                    "Cannot access member on non-class type: " + getTypeName(objectType),
                    expr));
            return null;
        }

        MemberSymbol member = classSymbol.getMember(expr.name.source);
        if (member == null) {
            unit.addError(unit.error(TAG,
                    UNRESOLVED_MEMBER.format(expr.name.source, classSymbol.getName()),
                    expr));
            return null;
        }

        Symbol type = table.unwrap(member);
        return SymbolResult.of(member, type);
    }

    @Override
    public SymbolResult visitCallExpr(CallExpr expr) {
        SymbolResult calleeResult = expr.callee.accept(this);
        if (calleeResult == null)
            return null;

        Symbol callee = calleeResult.getRaw();

        if (callee == null)
            return null;

        if (callee.isFun()) {
            FunSymbol funSymbol = callee.asFun();

            if (expr.arguments != null) {
                Map<String, ParamSymbol> params = funSymbol.getParameters();
                List<ParamSymbol> paramList = new ArrayList<>(params.values());

                if (expr.arguments.size() != paramList.size()) {
                    unit.addError(unit.error(TAG,
                            "Function '" + funSymbol.getName() +
                                    "' expects " + paramList.size() +
                                    " arguments but got " + expr.arguments.size(),
                            expr));
                    return null;
                }

                for (int i = 0; i < expr.arguments.size(); i++) {
                    SymbolResult argResult = expr.arguments.get(i).accept(this);
                    Symbol argType = argResult != null ? argResult.getType() : null;
                    Symbol paramType = paramList.get(i).getType();

                    if (argType != null && paramType != null) {
                        if (!table.isAssignable(argType, paramType)) {
                            unit.addError(unit.error(TAG,
                                    INVALID_ARGUMENT_TYPE.format(
                                            funSymbol.getName(),
                                            getTypeName(paramType),
                                            getTypeName(argType)),
                                    expr.arguments.get(i)));
                        }
                    }
                }
            }

            Symbol returnType = funSymbol.getReturnType();
            return SymbolResult.of(funSymbol, returnType);
        }

        if (callee.isClass()) {
            unit.addError(unit.error(TAG,
                    "Cannot call class constructor: " + getTypeName(callee),
                    expr));
            return null;
        }

        unit.addError(unit.error(TAG,
                INVALID_CALL.format(getTypeName(callee)),
                expr));
        return null;
    }

    @Override
    public SymbolResult visitBinaryExpr(BinaryExpr expr) {
        SymbolResult leftResult = expr.left != null ? expr.left.accept(this) : null;
        SymbolResult rightResult = expr.right != null ? expr.right.accept(this) : null;

        Symbol leftType = leftResult != null ? leftResult.getType() : null;
        Symbol rightType = rightResult != null ? rightResult.getType() : null;

        String op = expr.operator.lexeme;

        // Operadores aritméticos
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (leftType != null && rightType != null) {
                if (table.isNumeric(leftType) && table.isNumeric(rightType)) {
                    if (table.isFloat(leftType) || table.isFloat(rightType)) {
                        return SymbolResult.of(null, table.getFloat());
                    }
                    return SymbolResult.of(null, table.getInt());
                }
                unit.addError(unit.error(TAG,
                        "Operands of '" + op + "' must be numeric, got " +
                                getTypeName(leftType) + " and " + getTypeName(rightType),
                        expr));
            }
            return null;
        }

        // Operadores de igualdade
        if (op.equals("==") || op.equals("!=")) {
            if (leftType != null && rightType != null) {
                if (!table.isComparable(leftType, rightType)) {
                    unit.addError(unit.error(TAG,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType),
                            expr));
                }
            }
            return SymbolResult.of(null, table.getBool());
        }

        // Operadores de comparação
        if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            if (leftType != null && rightType != null) {
                if (!table.isNumeric(leftType) || !table.isNumeric(rightType)) {
                    unit.addError(unit.error(TAG,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType) + " numerically",
                            expr));
                }
            }
            return SymbolResult.of(null, table.getBool());
        }

        // Operadores lógicos
        if (op.equals("&&") || op.equals("||")) {
            if (leftType != null && !table.isBool(leftType)) {
                unit.addError(unit.error(TAG,
                        "Left operand of '" + op + "' must be Bool, got " + getTypeName(leftType),
                        expr.left));
            }
            if (rightType != null && !table.isBool(rightType)) {
                unit.addError(unit.error(TAG,
                        "Right operand of '" + op + "' must be Bool, got " + getTypeName(rightType),
                        expr.right));
            }
            return SymbolResult.of(null, table.getBool());
        }

        return null;
    }

    @Override
    public SymbolResult visitUnaryExpr(UnaryExpr expr) {
        SymbolResult operandResult = expr.operand.accept(this);
        Symbol operandType = operandResult != null ? operandResult.getType() : null;
        String op = expr.operator.lexeme;

        if (op.equals("!")) {
            if (operandType != null && !table.isBool(operandType)) {
                unit.addError(unit.error(TAG,
                        "Operand of '!' must be Bool, got " + getTypeName(operandType),
                        expr));
            }
            return SymbolResult.of(null, table.getBool());
        }

        if (op.equals("-") || op.equals("+")) {
            if (operandType != null && !table.isNumeric(operandType)) {
                unit.addError(unit.error(TAG,
                        "Operand of '" + op + "' must be numeric, got " + getTypeName(operandType),
                        expr));
            }
            return SymbolResult.of(null, operandType);
        }

        return operandResult;
    }

    @Override
    public SymbolResult visitLiteralExpr(LiteralExpr expr) {
        Symbol type;
        switch (expr.type) {
            case LiteralExpr.NULL:
                type = table.getNull();
                break;
            case LiteralExpr.BOOL:
                type = table.getBool();
                break;
            case LiteralExpr.CHAR:
                type = table.getChar();
                break;
            case LiteralExpr.FLOAT:
                type = table.getFloat();
                break;
            case LiteralExpr.INT:
                type = table.getInt();
                break;
            case LiteralExpr.STRING:
                type = table.getString();
                break;
            default:
                type = null;
        }
        return SymbolResult.of(null, type);
    }

    @Override
    public SymbolResult visitParenthesisExpr(ParenthesisExpr expr) {
        if (expr.inner != null)
            return expr.inner.accept(this);
        return null;
    }

    @Override
    public SymbolResult visitTernaryExpr(TernaryExpr expr) {
        if (expr.condition != null) {
            SymbolResult condResult = expr.condition.accept(this);
            Symbol condType = condResult != null ? condResult.getType() : null;
            if (condType != null && !table.isBool(condType)) {
                unit.addError(unit.error(TAG,
                        "Ternary condition must be Bool, got " + getTypeName(condType),
                        expr.condition));
            }
        }

        SymbolResult thenResult = expr.thenExpr != null ? expr.thenExpr.accept(this) : null;
        SymbolResult elseResult = expr.elseExpr != null ? expr.elseExpr.accept(this) : null;

        Symbol thenType = thenResult != null ? thenResult.getType() : null;
        Symbol elseType = elseResult != null ? elseResult.getType() : null;

        if (thenType != null && elseType != null) {
            if (table.isAssignable(thenType, elseType)) {
                return SymbolResult.of(null, elseType);
            }
            if (table.isAssignable(elseType, thenType)) {
                return SymbolResult.of(null, thenType);
            }

            Symbol lub = table.getLeastUpperBound(thenType, elseType);
            if (lub != null) {
                return SymbolResult.of(null, lub);
            }

            unit.addError(unit.error(TAG,
                    "Ternary branches have incompatible types: " +
                            getTypeName(thenType) + " and " + getTypeName(elseType),
                    expr));
            return null;
        }

        return thenType != null ? thenResult : elseResult;
    }

    @Override
    public SymbolResult visitAssignExpr(AssignExpr expr) {
        SymbolResult targetResult = expr.target.accept(this);
        Symbol targetType = targetResult != null ? targetResult.getType() : null;

        if (expr.value != null) {
            SymbolResult valueResult = expr.value.accept(this);
            Symbol valueType = valueResult != null ? valueResult.getType() : null;

            if (targetType != null && valueType != null) {
                if (!table.isAssignable(valueType, targetType)) {
                    unit.addError(unit.error(TAG,
                            INVALID_ASSIGNMENT.format(
                                    getTypeName(valueType),
                                    getTypeName(targetType)),
                            expr.value));
                }
            }
            return SymbolResult.of(null, valueType);
        }
        return null;
    }

    @Override
    public SymbolResult visitIndexAccessExpr(IndexAccessExpr expr) {
        SymbolResult targetResult = expr.target.accept(this);
        Symbol targetType = targetResult != null ? targetResult.getType() : null;

        SymbolResult indexResult = expr.index.accept(this);
        Symbol indexType = indexResult != null ? indexResult.getType() : null;

        if (targetType != null && !table.isClassType(targetType)) {
            unit.addError(unit.error(TAG,
                    INVALID_INDEX_ACCESS.format(getTypeName(targetType)),
                    expr));
            return null;
        }

        if (indexType != null && !table.isNumeric(indexType)) {
            unit.addError(unit.error(TAG,
                    "Index must be numeric, got " + getTypeName(indexType),
                    expr.index));
            return null;
        }

        ClassSymbol classSymbol = table.getBaseClass(targetType);
        if (classSymbol != null) {
            MemberSymbol getter = classSymbol.getDeclaredMember("get");
            if (getter != null && getter.isFun()) {
                FunSymbol fun = getter.asFun();
                return SymbolResult.of(null, fun.getReturnType());
            }
        }

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
        if (stmt.expression != null)
            stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl decl) {
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl decl) {
        return null;
    }

    // Métodos auxiliares

    private String getTypeName(Symbol sym) {
        if (sym == null)
            return "unknown";
        if (sym.isClass())
            return sym.asClass().getName();
        if (sym.isParameterizedClass()) {
            ParameterizedClassSymbol pcs = sym.asParameterizedClass();
            return pcs.getBase().getName() + "<...>";
        }
        if (sym.isTypeParam())
            return sym.getName();
        if (sym.isFun())
            return "function";
        if (sym.isVar()) {
            VarSymbol vs = sym.asVar();
            Symbol type = vs.getType();
            return type != null ? getTypeName(type) : "var";
        }
        if (sym.isParam()) {
            ParamSymbol ps = sym.asParam();
            Symbol type = ps.getType();
            return type != null ? getTypeName(type) : "param";
        }
        return sym.getName();
    }
}