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

public class Analyzer implements DefaultStmtVisitor<Void>, DefaultExprVisitor<SymbolResult> {

    private SymbolTable table;
    private CompilationUnit unit;
    private final SymbolScope global;
    private final Stack<SymbolScope> scopeStack = new Stack<>();
    private final Stack<Map<String, Symbol>> substitutionStack = new Stack<>();
    private ClassSymbol currentClass;
    private FunSymbol currentFun;

    private final Consumer<Typed> typeReporter = t -> unit
            .addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t));

    public Analyzer(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.global = new SymbolScope("global");
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
    public Void visitClassDeclStmt(ClassDecl stmt) {
        currentClass = table.get(stmt.name.source);
        if (currentClass == null)
            return null;

        SymbolScope classScope = currentScope().child(stmt.name.source);
        classScope.putLocal("this", currentClass);

        Map<String, Symbol> subs = new LinkedHashMap<>();
        subs.put(currentClass.getName(), currentClass);
        subs.putAll(currentClass.getTypeParameters());

        pushScope(classScope);
        pushSubstitutions(subs);

        if (stmt.members != null) {
            stmt.members.forEach(member -> member.accept(this));
        }

        popSubstitutions();
        popScope();
        currentClass = null;
        return null;
    }

    @Override
    public Void visitFunDeclStmt(FunDecl stmt) {
        if (currentClass == null)
            return null;

        currentFun = (FunSymbol) currentClass.getDeclaredMember(stmt.name.source);
        if (currentFun == null)
            return null;

        SymbolScope funScope = currentScope().child(stmt.name.source);

        Map<String, Symbol> subs = new LinkedHashMap<>();
        if (currentFun.getTypeParameters() != null) {
            subs.putAll(currentFun.getTypeParameters());
        }

        pushScope(funScope);
        pushSubstitutions(subs);

        if (stmt.body != null) {
            stmt.body.accept(this);
        }

        popSubstitutions();
        popScope();
        currentFun = null;
        return null;
    }

    @Override
    public Void visitVarDeclStmt(VarDecl stmt) {
        VarSymbol varSymbol = (VarSymbol) currentClass.getDeclaredMember(stmt.name.source);
        if (varSymbol == null) {
            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(stmt.name.source), stmt));
            return null;
        }

        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            Symbol valueType = result.getType();
            Symbol varType = varSymbol.getType();

            if (varType != null && valueType != null) {
                if (!table.isAssignable(valueType, varType)) {
                    unit.addError(unit.error(TAG,
                            INVALID_ASSIGNMENT.format(
                                    getTypeName(valueType),
                                    getTypeName(varType)),
                            stmt.value));
                }
            }
        }

        return null;
    }

    @Override
    public Void visitLetDeclStmt(LetDecl stmt) {

        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            Symbol valueType = result.getType();

            if (stmt.type != null) {
                Symbol declaredType = table.resolve(stmt.type, currentSubstitutions(), typeReporter);

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
            Symbol condType = result.getType();            if (condType != null && !table.isBool(condType)) {
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
            Symbol condType = result.getType();
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
            Symbol valueType = result.getType();

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

                if (member != null)
                    return SymbolResult.of(member, table.unwrap(member));
            }

            if (currentFun != null) {
                ParamSymbol param = currentFun.getParameter(expr.name.source);
                if (param != null)
                    return SymbolResult.of(param, table.unwrap(param));
            }

            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(expr.name.source), expr));
            return null;
        }

        return SymbolResult.of(symbol, symbol);
    }

    @Override
    public SymbolResult visitMemberAccessExpr(MemberAccessExpr expr) {
        SymbolResult result = expr.object.accept(this);
        Symbol objectType = result.getType();

        ClassSymbol classSymbol = table.getBaseClass(objectType);
        if (classSymbol == null) {
            unit.addError(unit.error(TAG,
                    "Cannot access member on non-class type: " + getTypeName(rawObjectType),
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

        return SymbolResult.of(member, table.unwrap(member));
    }

    @Override
    public Symbol visitCallExpr(CallExpr expr) {
        Symbol callee = expr.callee.accept(this);
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
                    Symbol argType = expr.arguments.get(i).accept(this);
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
                    System.out.print("");
                }
            }

            return funSymbol.getReturnType();
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
    public Symbol visitBinaryExpr(BinaryExpr expr) {
        Symbol leftType = expr.left != null ? expr.left.accept(this) : null;
        Symbol rightType = expr.right != null ? expr.right.accept(this) : null;

        String op = expr.operator.lexeme;

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (leftType != null && rightType != null) {
                if (table.isNumeric(leftType) && table.isNumeric(rightType)) {
                    if (table.isFloat(leftType) || table.isFloat(rightType)) {
                        return table.getFloat();
                    }
                    return table.getInt();
                }
                unit.addError(unit.error(TAG,
                        INVALID_OPERATOR.format(op, getTypeName(leftType)),
                        expr));
            }
            return null;
        }

        if (op.equals("==") || op.equals("!=")) {
            if (leftType != null && rightType != null) {
                if (!table.isComparable(leftType, rightType)) {
                    unit.addError(unit.error(TAG,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType),
                            expr));
                }
            }
            return table.getBool();
        }

        if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            if (leftType != null && rightType != null) {
                if (!table.isNumeric(leftType) || !table.isNumeric(rightType)) {
                    unit.addError(unit.error(TAG,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType) + " numerically",
                            expr));
                }
            }
            return table.getBool();
        }

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
            return table.getBool();
        }

        return null;
    }

    @Override
    public Symbol visitUnaryExpr(UnaryExpr expr) {
        Symbol operandType = expr.operand.accept(this);
        String op = expr.operator.lexeme;

        if (op.equals("!")) {
            if (operandType != null && !table.isBool(operandType)) {
                unit.addError(unit.error(TAG,
                        "Operand of '!' must be Bool, got " + getTypeName(operandType),
                        expr));
            }
            return table.getBool();
        }

        if (op.equals("-") || op.equals("+")) {
            if (operandType != null && !table.isNumeric(operandType)) {
                unit.addError(unit.error(TAG,
                        "Operand of '" + op + "' must be numeric, got " + getTypeName(operandType),
                        expr));
            }
            return operandType;
        }

        return operandType;
    }

    @Override
    public Symbol visitLiteralExpr(LiteralExpr expr) {

        switch (expr.type) {
            case LiteralExpr.NULL:
                return table.getNull();
            case LiteralExpr.BOOL:
                return table.getBool();
            case LiteralExpr.CHAR:
                return table.getChar();
            case LiteralExpr.FLOAT:
                return table.getFloat();
            case LiteralExpr.INT:
                return table.getInt();
            case LiteralExpr.STRING:
                return table.getString();
            default:
                return null;
        }
    }

    @Override
    public Symbol visitParenthesisExpr(ParenthesisExpr expr) {
        if (expr.inner != null)
            return expr.inner.accept(this);
        return null;
    }

    @Override
    public Symbol visitTernaryExpr(TernaryExpr expr) {
        if (expr.condition != null) {
            Symbol condType = expr.condition.accept(this);
            if (condType != null && !table.isBool(condType)) {
                unit.addError(unit.error(TAG,
                        "Ternary condition must be Bool, got " + getTypeName(condType),
                        expr.condition));
            }
        }

        Symbol thenType = expr.thenExpr != null ? expr.thenExpr.accept(this) : null;
        Symbol elseType = expr.elseExpr != null ? expr.elseExpr.accept(this) : null;

        if (thenType != null && elseType != null) {
            if (table.isAssignable(thenType, elseType)) {
                return elseType;
            }
            if (table.isAssignable(elseType, thenType)) {
                return thenType;
            }

            Symbol lub = table.getLeastUpperBound(thenType, elseType);
            if (lub != null) {
                return lub;
            }

            unit.addError(unit.error(TAG,
                    "Ternary branches have incompatible types: " +
                            getTypeName(thenType) + " and " + getTypeName(elseType),
                    expr));
            return null;
        }

        return thenType != null ? thenType : elseType;
    }

    @Override
    public Symbol visitAssignExpr(AssignExpr expr) {
        Symbol targetType = expr.target.accept(this);

        if (expr.value != null) {
            Symbol valueType = expr.value.accept(this);

            if (targetType != null && valueType != null) {
                if (!table.isAssignable(valueType, targetType)) {
                    unit.addError(unit.error(TAG,
                            INVALID_ASSIGNMENT.format(
                                    getTypeName(valueType),
                                    getTypeName(targetType)),
                            expr.value));
                }
            }
            return valueType;
        }
        return null;
    }

    @Override
    public Symbol visitIndexAccessExpr(IndexAccessExpr expr) {
        Symbol targetType = expr.target.accept(this);
        Symbol indexType = expr.index.accept(this);

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
                return fun.getReturnType();
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
    public Void visitParamDeclStmt(ParamDecl stmt) {
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDecl stmt) {
        return null;
    }

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