package com.lang.register;

import static com.lang.register.RegisteringErrorCode.*;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;
import com.lang.util.Positioned;

public class Register implements DeclVisitor<Void> {
    private final SymbolTable table;
    private final CompilationUnit unit;

    private ClassSymbol currentClass;
    private VarSymbol currentVariable;
    private CallableSymbol currentCallable;

    public Register(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
    }

    public void register() {
        for (ClassDecl declaration : unit.getClasses())
            declaration.accept(this);
    }

    private MemberSymbol currentMember() {
        if (currentVariable != null)
            return currentVariable;
        if (currentCallable != null)
            return currentCallable;
        return currentClass;
    }

    private void report(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.error(
                node != null ? node.getPosition() : null, message));
    }

    @Override
    public Void visitClassDecl(ClassDecl declaration) {
        String name = declaration.name.source;
        if (table.hasClass(name)) {
            report(declaration, REDEFINED_CLASS.format(name));
            return null;
        }

        ClassSymbol symbol = new ClassSymbol(name);
        symbol.node = declaration;
        symbol.setModifiers(declaration.modifiers);
        if (!table.addClass(symbol))
            return null;

        ClassSymbol previousClass = currentClass;
        currentClass = symbol;
        declaration.symbol = symbol;

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

        currentClass = previousClass;
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl declaration) {
        String name = declaration.name.source;

        boolean alreadyDefined;
        if (currentCallable != null)
            alreadyDefined = currentCallable.hasTypeParameter(name);
        else if (currentClass != null)
            alreadyDefined = currentClass.hasTypeParameter(name);
        else
            alreadyDefined = false;

        if (alreadyDefined) {
            report(declaration, REDEFINED_TYPE_PARAMETER.format(name));
            return null;
        }

        TypeParamSymbol symbol = new TypeParamSymbol(name);
        symbol.node = declaration;
        declaration.symbol = symbol;

        if (currentCallable != null)
            currentCallable.addTypeParameter(symbol);
        else if (currentClass != null)
            currentClass.addTypeParameter(symbol);
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl declaration) {
        if (currentClass == null)
            return null;
        String name = declaration.type.getName();
        if (currentClass.hasSuper(name)) {
            report(declaration, REDEFINED_SUPER.format(name));
            return null;
        }

        SuperSymbol symbol = new SuperSymbol(name);
        symbol.node = declaration;
        currentClass.addSuper(symbol);
        declaration.symbol = symbol;
        return null;
    }

    @Override
    public Void visitAnnoDecl(AnnoDecl declaration) {
        if (declaration == null || declaration.type == null)
            return null;

        MemberSymbol owner = currentMember();
        if (owner == null)
            return null;

        String name = declaration.type.getName();
        if (owner.hasAnnotation(name)) {
            report(declaration, REDEFINED_ANNOTATION.format(name));
            return null;
        }

        AnnoSymbol symbol = new AnnoSymbol(name);
        symbol.node = declaration;
        declaration.symbol = symbol;
        owner.addAnnotation(symbol);
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl declaration) {
        if (currentClass == null)
            return null;
        String name = declaration.name.source;
        if (currentClass.hasDeclaredFun(name) || currentClass.hasDeclaredVar(name)) {
            report(declaration, REDEFINED_VARIABLE.format(name));
            return null;
        }

        VarSymbol symbol = new VarSymbol(name);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        symbol.setModifiers(declaration.modifiers);
        currentClass.addDeclaredVar(symbol);
        declaration.symbol = symbol;

        VarSymbol previousVariable = currentVariable;
        currentVariable = symbol;

        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));

        currentVariable = previousVariable;
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl declaration) {
        if (currentClass == null)
            return null;
        String name = declaration.name.source;
        if (currentClass.hasDeclaredFun(name) || currentClass.hasDeclaredVar(name)) {
            report(declaration, REDEFINED_FUNCTION.format(name));
            return null;
        }

        FunSymbol symbol = new FunSymbol(name);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        symbol.setModifiers(declaration.modifiers);
        currentClass.addDeclaredFun(symbol);
        declaration.symbol = symbol;

        bindCallable(declaration, symbol);
        return null;
    }

    @Override
    public Void visitOperatorDecl(OperatorDecl declaration) {
        if (currentClass == null)
            return null;
        String signature = OperatorSymbol.signatureKey(
                declaration.kind, declaration.operator,
                declaration.parameters.stream().map(p -> p.type).toList());

        if (currentClass.hasDeclaredOperator(signature)) {
            report(declaration, REDEFINED_OPERATOR.format(declaration.operator.lexeme));
            return null;
        }

        OperatorSymbol symbol = new OperatorSymbol(declaration.kind, declaration.operator);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        symbol.setModifiers(declaration.modifiers);
        currentClass.addDeclaredOperator(signature, symbol);
        declaration.symbol = symbol;

        bindCallable(declaration, symbol);
        return null;
    }

    private void bindCallable(CallableDecl declaration, CallableSymbol symbol) {
        CallableSymbol previous = currentCallable;
        currentCallable = symbol;

        if (declaration.annotations != null)
            declaration.annotations.forEach(a -> a.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(p -> p.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(p -> p.accept(this));

        currentCallable = previous;
    }

    @Override
    public Void visitParamDecl(ParamDecl declaration) {
        String name = declaration.name.source;

        boolean alreadyDefined;
        if (currentCallable != null)
            alreadyDefined = currentCallable.hasParameter(name)
                    || currentCallable.hasTypeParameter(name);
        else if (currentClass != null)
            alreadyDefined = currentClass.hasParameter(name)
                    || currentClass.hasTypeParameter(name);
        else
            alreadyDefined = false;

        if (alreadyDefined) {
            report(declaration, REDEFINED_PARAMETER.format(name));
            return null;
        }

        ParamSymbol symbol = new ParamSymbol(name);
        symbol.node = declaration;
        symbol.setInit(declaration.init);
        declaration.symbol = symbol;

        if (currentCallable != null)
            currentCallable.addParameter(symbol);
        else if (currentClass != null)
            currentClass.addParameter(symbol);
        return null;
    }
}