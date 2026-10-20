// ./src/main/java/com/lang/analyzer/Analyzer.java
package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.util.*;
import com.lang.unit.CompilationUnit;

public class Analyzer implements DeclVisitor<Void>, StmtVisitor<Void>, ExprVisitor<SymbolResult> {

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
        this.reporter = t -> unit.addDiagnostic(TAG, Diagnostic.error(
                t != null ? t.getPosition() : null,
                UNRESOLVED_TYPE.format(t.getName())));
    }

    public void analyze() {
        scopeStack.push(global);
        substitutionStack.push(new LinkedHashMap<>());
        unit.getClasses().forEach(cls -> cls.accept(this));
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

    private void report(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.error(
                node != null ? node.getPosition() : null, message));
    }

    private void warn(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.warning(
                node != null ? node.getPosition() : null, message));
    }

    private void hint(Position pos, String message) {
        unit.addDiagnostic(TAG, Diagnostic.hint(pos, message));
    }

    private void hint(Positioned node, String message) {
        hint(node != null ? node.getPosition() : null, message);
    }

    private Position zeroWidth(int lineCount, int lineStart, int point) {
        return new Position(
                lineCount, lineStart, point,
                lineCount, lineStart, point);
    }

    private void hintParamAtArg(Expr argExpr, ParamSymbol param, Symbol effectiveType) {
        if (argExpr == null || param == null) return;

        Position pos = argExpr.getPosition();
        if (pos == null) return;

        int start = pos.getStart();
        Position hintPos = zeroWidth(
                pos.getFirstLineCount(),
                pos.getFirstLineStart(),
                start);

        hint(hintPos, param.getName() + ":");
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
        if (currentClass == null)
            return null;

        VarSymbol varSymbol = (VarSymbol) currentClass.getDeclaredMember(decl.name.source);
        if (varSymbol == null) {
            report(decl, UNDEFINED_VARIABLE.format(decl.name.source));
            return null;
        }

        if (decl.value != null) {
            SymbolResult result = decl.value.accept(this);
            Symbol valueType = result != null ? result.getType() : null;
            Symbol varType = varSymbol.getType();

            if (varType != null && valueType != null) {
                if (!table.isAssignable(valueType, varType)) {
                    report(decl.value, INVALID_ASSIGNMENT.format(
                            getTypeName(valueType),
                            getTypeName(varType)));
                }
            }
        }

        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl decl) {
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

    @Override
    public Void visitLetStmt(LetStmt stmt) {
        if (stmt.name == null) {
            return null;
        }

        if (currentScope().hasLocal(stmt.name.source)) {
            report(stmt, "Variable '" + stmt.name.source + "' is already declared in this scope");
            return null;
        }

        Symbol declaredType = null;
        Symbol valueType = null;

        if (stmt.type != null) {
            declaredType = table.resolve(stmt.type, currentSubstitutions(), reporter);
        }

        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            valueType = result != null ? result.getType() : null;
        }

        if (declaredType != null && valueType != null) {
            if (!table.isAssignable(valueType, declaredType)) {
                report(stmt.value, INVALID_ASSIGNMENT.format(
                        getTypeName(valueType),
                        getTypeName(declaredType)));
            }
        }

        Symbol effectiveType = declaredType != null ? declaredType : valueType;

        if (effectiveType == null) {
            report(stmt,
                    "Cannot infer type of '" + stmt.name.source +
                            "': no type annotation and no initializer");
            return null;
        }

        currentScope().putLocal(stmt.name.source, effectiveType);

        String hintText = ": " + getTypeName(effectiveType);

        Position namePos = stmt.name.position;
        if (namePos != null) {
            int end = namePos.getEnd();
            Position hintPos = zeroWidth(
                    namePos.getLastLineCount(),
                    namePos.getLastLineStart(),
                    end);
            hint(hintPos, hintText);
        } else {
            hint(stmt, hintText);
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
                report(stmt.condition,
                        "Condition must be Bool, got " + getTypeName(condType));
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
                report(stmt.condition,
                        "Condition must be Bool, got " + getTypeName(condType));
            }
        }
        if (stmt.body != null)
            stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt stmt) {
        if (currentFun == null) {
            report(stmt, INVALID_FUNCTION_RETURN_OUTSIDE.format());
            return null;
        }

        Symbol returnType = currentFun.getType();

        if (stmt.value != null) {
            SymbolResult result = stmt.value.accept(this);
            Symbol valueType = result != null ? result.getType() : null;

            if (returnType != null && valueType != null) {
                if (!table.isAssignable(valueType, returnType)) {
                    report(stmt.value, INVALID_FUNCTION_RETURN_TYPE.format(
                            currentFun.getName(),
                            getTypeName(returnType),
                            getTypeName(valueType)));
                }
            }
        } else {
            if (returnType != null && !table.isVoid(returnType)) {
                report(stmt, INVALID_FUNCTION_MISSING_RETURN.format(
                        currentFun.getName(),
                        getTypeName(returnType)));
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
    public SymbolResult visitRefExpr(RefExpr expr) {
        String refName = expr.name.source;
        Symbol symbol = currentScope().resolve(refName);

        if (symbol == null) {
            if (currentFun != null) {
                if (currentFun.hasTypeParameter(refName)) {
                    TypeParamSymbol typeParam = currentFun.getTypeParameter(refName);
                    return SymbolResult.of(typeParam, typeParam);
                }
                if (currentFun.hasParameter(refName)) {
                    ParamSymbol param = currentFun.getParameter(refName);
                    return SymbolResult.of(param, table.unwrap(param));
                }
            }

            if (currentClass != null) {
                if (currentClass.hasTypeParameter(refName)) {
                    TypeParamSymbol typeParam = currentClass.getTypeParameter(refName);
                    return SymbolResult.of(typeParam, typeParam);
                }
                if (currentClass.hasParameter(refName)) {
                    ParamSymbol param = currentClass.getParameter(refName);
                    return SymbolResult.of(param, table.unwrap(param));
                }
                MemberSymbol member = currentClass.getMember(refName);
                if (member != null) {
                    return SymbolResult.of(member, table.unwrap(member));
                }
            }

            ClassSymbol classSymbol = table.get(refName);
            if (classSymbol != null) {
                return SymbolResult.of(classSymbol, classSymbol);
            }

            report(expr, UNDEFINED_VARIABLE.format(refName));
            return null;
        }

        return SymbolResult.of(symbol, table.unwrap(symbol));
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
            report(expr, "Cannot access member on non-class type: " + getTypeName(objectType));
            return null;
        }

        MemberSymbol member = classSymbol.getMember(expr.name.source);
        if (member == null) {
            report(expr, UNRESOLVED_MEMBER.format(expr.name.source, classSymbol.getName()));
            return null;
        }

        Symbol type = table.unwrap(member);
        return SymbolResult.of(member, type);
    }

    @Override
    public SymbolResult visitCallExpr(CallExpr expr) {
        SymbolResult calleeResult = expr.callee.accept(this);
        if (calleeResult == null || calleeResult.getRaw() == null) {
            return null;
        }

        Symbol callee = calleeResult.getRaw();

        if (callee.isClass()) {
            return visitClassInstantiation(expr, callee.asClass());
        }

        if (callee.isParameterizedClass()) {
            return visitClassInstantiation(expr, callee.asParameterizedClass().getBase());
        }

        if (callee.isFun()) {
            return visitFunctionCall(expr, callee.asFun());
        }

        report(expr, INVALID_FUNCTION_CALL.format(getTypeName(callee)));
        return null;
    }

    private SymbolResult visitClassInstantiation(CallExpr expr, ClassSymbol classSymbol) {
        Map<String, Symbol> typeSubs = new LinkedHashMap<>();
        List<String> tpNames = new ArrayList<>(classSymbol.getTypeParameters().keySet());
        List<Typed> typeArgs = expr.typeArguments != null
                ? expr.typeArguments
                : Collections.emptyList();

        if (typeArgs.isEmpty()) {
            if (!tpNames.isEmpty()) {
                for (String tpName : tpNames) {
                    TypeParamSymbol tp = classSymbol.getTypeParameter(tpName);
                    Symbol bound = resolveUpperBound(tp);
                    if (bound != null) {
                        typeSubs.put(tpName, bound);
                    }
                }
                warn(expr, INVALID_CLASS_UNCHECKED_CONSTRUCTOR.format(
                        classSymbol.getName(),
                        boundMapToString(tpNames, classSymbol)));
            }
        } else if (tpNames.size() != typeArgs.size()) {
            report(expr, INVALID_CLASS_CONSTRUCTOR_TYPE_ARGUMENT_COUNT.format(
                    classSymbol.getName(), tpNames.size(), typeArgs.size()));
        } else {
            for (int i = 0; i < tpNames.size(); i++) {
                Typed arg = typeArgs.get(i);
                Symbol resolved = table.resolve(arg, currentSubstitutions(), reporter);
                if (resolved != null) {
                    typeSubs.put(tpNames.get(i), resolved);
                }
            }
        }

        List<ParamSymbol> classParams = new ArrayList<>(classSymbol.getParameters().values());
        List<Expr> arguments = expr.arguments != null ? expr.arguments : Collections.emptyList();

        if (arguments.size() != classParams.size()) {
            report(expr, INVALID_CLASS_CONSTRUCTOR_ARGUMENT_COUNT.format(
                    classSymbol.getName(), classParams.size(), arguments.size()));
            return buildInstantiationResult(classSymbol, typeSubs);
        }

        for (int i = 0; i < arguments.size(); i++) {
            Expr argExpr = arguments.get(i);
            SymbolResult argResult = argExpr.accept(this);
            Symbol argType = argResult != null ? argResult.getType() : null;

            ParamSymbol param = classParams.get(i);
            Symbol rawParamType = param.getType();
            if (rawParamType == null) {
                report(expr,
                        "Type of parameter '" + param.getName() +
                                "' of class '" + classSymbol.getName() +
                                "' could not be resolved");
                continue;
            }

            Symbol paramType = substitute(rawParamType, typeSubs);

            if (argType != null && !table.isAssignable(argType, paramType)) {
                report(argExpr, INVALID_CLASS_CONSTRUCTOR_ARGUMENT_TYPE.format(
                        classSymbol.getName(),
                        getTypeName(paramType),
                        getTypeName(argType)));
            }

            hintParamAtArg(argExpr, param, paramType);
        }

        return buildInstantiationResult(classSymbol, typeSubs);
    }

    private SymbolResult buildInstantiationResult(ClassSymbol classSymbol,
                                                  Map<String, Symbol> typeSubs) {
        if (!typeSubs.isEmpty()) {
            ParameterizedClassSymbol pcs = new ParameterizedClassSymbol(classSymbol, typeSubs);
            return SymbolResult.of(classSymbol, pcs);
        }
        return SymbolResult.of(classSymbol, classSymbol);
    }

    private Symbol resolveUpperBound(TypeParamSymbol tp) {
        if (tp == null)
            return table.getAny();

        for (SuperSymbol sup : tp.getSupers().values()) {
            Symbol t = sup.getType();
            if (t != null)
                return t;
        }
        return table.getAny();
    }

    private String boundMapToString(List<String> tpNames, ClassSymbol classSymbol) {
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < tpNames.size(); i++) {
            if (i > 0) sb.append(", ");
            TypeParamSymbol tp = classSymbol.getTypeParameter(tpNames.get(i));
            sb.append(getTypeName(resolveUpperBound(tp)));
        }
        sb.append(">");
        return sb.toString();
    }

    private SymbolResult visitFunctionCall(CallExpr expr, FunSymbol funSymbol) {
        List<ParamSymbol> paramList = new ArrayList<>(funSymbol.getParameters().values());
        List<Expr> arguments = expr.arguments != null ? expr.arguments : Collections.emptyList();

        if (arguments.size() != paramList.size()) {
            report(expr, INVALID_FUNCTION_ARGUMENT_COUNT.format(
                    funSymbol.getName(), paramList.size(), arguments.size()));
            return null;
        }

        for (int i = 0; i < arguments.size(); i++) {
            Expr argExpr = arguments.get(i);
            SymbolResult argResult = argExpr.accept(this);

            Symbol argType = argResult != null ? argResult.getType() : null;
            ParamSymbol param = paramList.get(i);
            Symbol paramType = param.getType();

            if (argType != null && paramType != null && !table.isAssignable(argType, paramType)) {
                report(argExpr, INVALID_FUNCTION_ARGUMENT_TYPE.format(
                        funSymbol.getName(),
                        getTypeName(paramType),
                        getTypeName(argType)));
            }

            hintParamAtArg(argExpr, param, paramType);
        }

        return SymbolResult.of(funSymbol, funSymbol.getType());
    }

    private Symbol substitute(Symbol type, Map<String, Symbol> subs) {
        if (type == null || subs == null || subs.isEmpty())
            return type;

        if (type.isTypeParam()) {
            Symbol replacement = subs.get(type.getName());
            return replacement != null ? replacement : type;
        }

        if (type.isParameterizedClass()) {
            ParameterizedClassSymbol pc = type.asParameterizedClass();
            Map<String, Symbol> newArgs = new LinkedHashMap<>();
            for (Map.Entry<String, Symbol> e : pc.getTypeArguments().entrySet()) {
                newArgs.put(e.getKey(), substitute(e.getValue(), subs));
            }
            return new ParameterizedClassSymbol(pc.getBase(), newArgs);
        }

        return type;
    }

    @Override
    public SymbolResult visitBinaryExpr(BinaryExpr expr) {
        SymbolResult leftResult = expr.left != null ? expr.left.accept(this) : null;
        SymbolResult rightResult = expr.right != null ? expr.right.accept(this) : null;

        Symbol leftType = leftResult != null ? leftResult.getType() : null;
        Symbol rightType = rightResult != null ? rightResult.getType() : null;

        String op = expr.operator.lexeme;

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (leftType != null && rightType != null) {
                if (table.isNumeric(leftType) && table.isNumeric(rightType)) {
                    if (table.isFloat(leftType) || table.isFloat(rightType)) {
                        return SymbolResult.of(null, table.getFloat());
                    }
                    return SymbolResult.of(null, table.getInt());
                }
                report(expr,
                        "Operands of '" + op + "' must be numeric, got " +
                                getTypeName(leftType) + " and " + getTypeName(rightType));
            }
            return null;
        }

        if (op.equals("==") || op.equals("!=")) {
            if (leftType != null && rightType != null) {
                if (!table.isComparable(leftType, rightType)) {
                    report(expr,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType));
                }
            }
            return SymbolResult.of(null, table.getBool());
        }

        if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            if (leftType != null && rightType != null) {
                if (!table.isNumeric(leftType) || !table.isNumeric(rightType)) {
                    report(expr,
                            "Cannot compare " + getTypeName(leftType) +
                                    " with " + getTypeName(rightType) + " numerically");
                }
            }
            return SymbolResult.of(null, table.getBool());
        }

        if (op.equals("&&") || op.equals("||")) {
            if (leftType != null && !table.isBool(leftType)) {
                report(expr.left,
                        "Left operand of '" + op + "' must be Bool, got " + getTypeName(leftType));
            }
            if (rightType != null && !table.isBool(rightType)) {
                report(expr.right,
                        "Right operand of '" + op + "' must be Bool, got " + getTypeName(rightType));
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
                report(expr,
                        "Operand of '!' must be Bool, got " + getTypeName(operandType));
            }
            return SymbolResult.of(null, table.getBool());
        }

        if (op.equals("-") || op.equals("+")) {
            if (operandType != null && !table.isNumeric(operandType)) {
                report(expr,
                        "Operand of '" + op + "' must be numeric, got " + getTypeName(operandType));
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
    public SymbolResult visitAssignExpr(AssignExpr expr) {
        SymbolResult targetResult = expr.target.accept(this);
        Symbol targetType = targetResult != null ? targetResult.getType() : null;

        if (expr.value != null) {
            SymbolResult valueResult = expr.value.accept(this);
            Symbol valueType = valueResult != null ? valueResult.getType() : null;

            if (targetType != null && valueType != null) {
                if (!table.isAssignable(valueType, targetType)) {
                    report(expr.value, INVALID_ASSIGNMENT.format(
                            getTypeName(valueType),
                            getTypeName(targetType)));
                }
            }
            return SymbolResult.of(null, valueType);
        }
        return null;
    }

    private String getTypeName(Symbol sym) {
        if (sym == null)
            return "unknown";
        if (sym.isClass())
            return sym.asClass().getName();
        if (sym.isParameterizedClass()) {
            ParameterizedClassSymbol pcs = sym.asParameterizedClass();
            StringBuilder sb = new StringBuilder(pcs.getBase().getName()).append("<");
            boolean first = true;
            for (Map.Entry<String, Symbol> e : pcs.getTypeArguments().entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(getTypeName(e.getValue()));
            }
            return sb.append(">").toString();
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