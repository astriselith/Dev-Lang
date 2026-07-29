package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class Analyzer implements DefaultStmtVisitor<Void>, DefaultExprVisitor<Symbol> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private final SymbolScope global;
    private final Stack<SymbolScope> scopeStack = new Stack<>();
    private final Stack<Map<String, Symbol>> substitutionStack = new Stack<>();
    private ClassSymbol currentClass;
    private FunSymbol currentFun;

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
    public Void visitClassDeclStmt(ClassDeclStmt stmt) {
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
    public Void visitFunDeclStmt(FunDeclStmt stmt) {
        if (currentClass == null)
            return null;

        currentFun = (FunSymbol) currentClass.getDeclaredMember(stmt.name.source);
        if (currentFun == null)
            return null;

        SymbolScope funScope = currentScope().child(stmt.name.source);

        if (currentFun.getTypeParameters() != null) {
            currentFun.getTypeParameters().values().forEach(tp -> funScope.putLocal(tp.getName(), tp));
        }

        if (currentFun.getParameters() != null) {
            currentFun.getParameters().values().forEach(param -> funScope.putLocal(param.getName(), param));
        }

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
    public Void visitVarDeclStmt(VarDeclStmt stmt) {
        if (stmt.value != null) {
            stmt.value.accept(this);
        }

        Symbol varSymbol = currentScope().resolveLocal(stmt.name.source);
        if (varSymbol == null) {
            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(stmt.name.source), stmt));
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
        if (stmt.condition != null)
            stmt.condition.accept(this);
        if (stmt.thenBranch != null)
            stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null)
            stmt.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt stmt) {
        if (stmt.condition != null)
            stmt.condition.accept(this);
        if (stmt.body != null)
            stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt stmt) {
        if (stmt.value != null) {
            stmt.value.accept(this);
        }
        return null;
    }

    @Override
    public Void visitLetDeclStmt(LetDeclStmt stmt) {
        if (stmt.value != null)
            stmt.value.accept(this);
        return null;
    }

    @Override
    public Symbol visitRefExpr(RefExpr expr) {
        Symbol symbol = currentScope().resolve(expr.name.source);

        if (symbol == null && currentClass != null) {
            MemberSymbol member = currentClass.getDeclaredMember(expr.name.source);
            if (member != null)
                return member;
        }

        if (symbol == null) {
            Symbol substSymbol = currentSubstitutions().get(expr.name.source);
            if (substSymbol != null)
                return substSymbol;
        }

        if (symbol == null) {
            unit.addError(unit.error(TAG, UNDEFINED_VARIABLE.format(expr.name.source), expr));
            return null;
        }
        return symbol;
    }

    @Override
    public Symbol visitMemberAccessExpr(MemberAccessExpr expr) {
        Symbol objectType = expr.object.accept(this);
        if (objectType == null)
            return null;

        ClassSymbol classSymbol = null;
        if (objectType.isClass()) {
            classSymbol = objectType.asClass();
        } else if (objectType.isParameterizedClass()) {
            classSymbol = objectType.asParameterizedClass().getBase();
        }

        if (classSymbol == null) {
            unit.addError(unit.error(TAG, UNRESOLVED_MEMBER.format(expr.name.source, "unknown"), expr));
            return null;
        }

        MemberSymbol member = classSymbol.getDeclaredMember(expr.name.source);
        if (member == null) {
            unit.addError(unit.error(TAG, UNRESOLVED_MEMBER.format(expr.name.source, classSymbol.getName()), expr));
            return null;
        }

        return member;
    }

    @Override
    public Symbol visitCallExpr(CallExpr expr) {
        Symbol callee = expr.callee.accept(this);
        if (callee == null)
            return null;

        if (expr.arguments != null) {
            expr.arguments.forEach(arg -> arg.accept(this));
        }

        if (callee.isFun()) {
            FunSymbol funSymbol = callee.asFun();
            if (funSymbol.getReturnType() != null) {
                return funSymbol.getReturnType();
            }
        }

        return null;
    }

    @Override
    public Symbol visitBinaryExpr(BinaryExpr expr) {
        if (expr.left != null)
            expr.left.accept(this);
        if (expr.right != null)
            expr.right.accept(this);
        return null;
    }

    @Override
    public Symbol visitUnaryExpr(UnaryExpr expr) {
        if (expr.operand != null)
            return expr.operand.accept(this);
        return null;
    }

    @Override
    public Symbol visitLiteralExpr(LiteralExpr expr) {
        String typeName = expr.getTypeName();
        if (typeName != null) {
            ClassSymbol classSymbol = table.get(typeName);
            if (classSymbol != null)
                return classSymbol;
        }
        return null;
    }

    @Override
    public Symbol visitParenthesisExpr(ParenthesisExpr expr) {
        if (expr.inner != null)
            return expr.inner.accept(this);
        return null;
    }

    @Override
    public Symbol visitTernaryExpr(TernaryExpr expr) {
        if (expr.condition != null)
            expr.condition.accept(this);
        if (expr.thenExpr != null)
            expr.thenExpr.accept(this);
        if (expr.elseExpr != null)
            expr.elseExpr.accept(this);
        return null;
    }

    @Override
    public Symbol visitAssignExpr(AssignExpr expr) {
        if (expr.target != null)
            expr.target.accept(this);
        if (expr.value != null)
            return expr.value.accept(this);
        return null;
    }

    @Override
    public Symbol visitIndexAccessExpr(IndexAccessExpr expr) {
        if (expr.target != null)
            expr.target.accept(this);
        if (expr.index != null)
            expr.index.accept(this);
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
    public Void visitParamDeclStmt(ParamDeclStmt stmt) {
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
        return null;
    }
}