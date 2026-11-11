package com.lang.linker;

import static com.lang.linker.LinkingErrorCode.*;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;
import com.lang.util.Positioned;

public class Linker implements DeclVisitor<Void> {
    private final SymbolTable table;
    private final CompilationUnit unit;

    private ClassSymbol currentClass;
    private FunSymbol currentFunction;
    private OperatorSymbol currentOperator;

    public Linker(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
    }

    public void link() {
        for (ClassDecl declaration : unit.getClasses())
            declaration.accept(this);
    }

    @Override
    public Void visitClassDecl(ClassDecl declaration) {
        ClassSymbol symbol = declaration.symbol != null
                ? declaration.symbol.asClass()
                : null;
        if (symbol == null || symbol.node != declaration)
            return null;

        ClassSymbol previousClass = currentClass;
        currentClass = symbol;

        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(p -> p.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(p -> p.accept(this));
        if (declaration.supers != null)
            declaration.supers.forEach(s -> s.accept(this));
        if (declaration.members != null)
            declaration.members.forEach(m -> m.accept(this));

        addImplicitAnySuper(symbol);

        currentClass = previousClass;
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl declaration) {
        TypeParamSymbol parameter = declaration.symbol != null
                ? declaration.symbol.asTypeParam()
                : null;
        if (parameter == null || declaration.bounds == null)
            return null;
        for (Typed boundType : declaration.bounds) {
            String name = boundType.getName();
            if (parameter.hasBound(name))
                continue; // Register já reportou duplicata
            Symbol type = resolveType(boundType);
            if (type == null)
                continue;
            if (type.isTypeParam()) {
                report(boundType, INVALID_TYPE_PARAMETER_BOUND.format(
                        type.getName(), parameter.getName()));
                continue;
            }
            parameter.addBound(type);
        }
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl declaration) {
        if (!(declaration.symbol instanceof SuperSymbol symbol))
            return null;
        Symbol type = resolveType(declaration.type);
        if (type == null)
            return null;
        if (!type.isClass() && !type.isParameterizedClass()) {
            report(declaration, INVALID_SUPER_TYPE.format(
                    type.getName(), currentClass != null ? currentClass.getName() : "<unknown>"));
            return null;
        }
        symbol.setType(type);
        return null;
    }

    @Override
    public Void visitAnnoDecl(AnnoDecl declaration) {
        if (!(declaration.symbol instanceof AnnoSymbol symbol))
            return null;
        Symbol type = resolveType(declaration.type);
        if (type == null)
            return null;
        if (!type.isClass() && !type.isParameterizedClass()) {
            report(declaration, INVALID_ANNOTATION_TYPE.format(
                    type.getName()));
            return null;
        }
        symbol.setType(type);
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl declaration) {
        if (!(declaration.symbol instanceof VarSymbol symbol)
                || symbol.node != declaration)
            return null;
        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));

        if (declaration.type != null) {
            Symbol type = resolveType(declaration.type);
            if (type != null)
                symbol.setType(type);
        } else if (declaration.value instanceof LiteralExpr literal) {
            String typeName = literal.getTypeName();
            Symbol type = typeName != null
                    ? resolveType(new RefTyped(new Identifier(typeName, literal.getPosition()),
                            literal.getPosition()))
                    : null;
            if (type != null)
                symbol.setType(type);
        } else if (declaration.value != null) {
            report(declaration.value, INVALID_INFERRED_VARIABLE_INITIALIZER.format(
                    declaration.name.source));
        }
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl declaration) {
        FunSymbol function = currentClass != null
                ? currentClass.getDeclaredFun(declaration.name.source)
                : null;
        if (function == null || function.node != declaration)
            return null;

        FunSymbol previousFunction = currentFunction;
        currentFunction = function;
        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(p -> p.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(p -> p.accept(this));

        if (declaration.type != null) {
            Symbol type = resolveType(declaration.type);
            if (type != null)
                function.setType(type);
        }

        currentFunction = previousFunction;
        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl declaration) {
        if (!(declaration.symbol instanceof ParamSymbol parameter))
            return null;
        if (declaration.type == null)
            return null;
        Symbol type = resolveType(declaration.type);
        if (type != null)
            parameter.setType(type);
        return null;
    }

    @Override
    public Void visitOperatorDecl(OperatorDecl declaration) {
        OperatorSymbol operator = currentClass != null
                ? currentClass.getDeclaredOperators().values().stream()
                        .filter(candidate -> candidate.node == declaration)
                        .findFirst().orElse(null)
                : null;
        if (operator == null || operator.node != declaration)
            return null;

        OperatorSymbol previousOperator = currentOperator;
        currentOperator = operator;
        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(p -> p.accept(this));
        if (declaration.operands != null)
            declaration.operands.forEach(o -> o.accept(this));

        if (declaration.type != null) {
            Symbol type = resolveType(declaration.type);
            if (type != null)
                operator.setType(type);
        }

        currentOperator = previousOperator;
        return null;
    }

    // ==================== helpers ====================

    private void addImplicitAnySuper(ClassSymbol symbol) {
        ClassSymbol any = table.getAny();
        if (any == null || symbol == any || symbol.hasSupers())
            return;
        SuperSymbol implicitSuper = new SuperSymbol(any.getName());
        implicitSuper.setType(any);
        symbol.addSuper(implicitSuper);
    }

    private Map<String, Symbol> substitutions() {
        Map<String, Symbol> substitutions = new LinkedHashMap<>();
        if (currentClass != null)
            substitutions.putAll(currentClass.getTypeParameters());
        if (currentFunction != null)
            substitutions.putAll(currentFunction.getTypeParameters());
        if (currentOperator != null)
            substitutions.putAll(currentOperator.getTypeParameters());
        if (currentClass != null)
            substitutions.put("Self", currentClass);
        return substitutions;
    }

    private Symbol resolveType(Typed type) {
        return table.resolve(type, substitutions(), unresolved -> {
            String name = unresolved != null ? unresolved.getName() : "<unknown>";
            report(unresolved, UNRESOLVED_TYPE.format(name));
        });
    }

    private void report(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.error(
                node != null ? node.getPosition() : null, message));
    }
}