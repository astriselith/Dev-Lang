package com.lang.register;

import static com.lang.register.RegisteringErrorCode.*;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class Register implements DeclVisitor<Void> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private ClassSymbol currentClass;
    private FunSymbol currentFunction;
    private TypeParamSymbol currentTypeParameter;

    public Register(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.currentClass = null;
        this.currentFunction = null;
        this.currentTypeParameter = null;
    }

    public void register() {
        unit.classes.forEach(cls -> cls.accept(this));
    }

    @Override
    public Void visitClassDecl(ClassDecl decl) {
        String className = decl.name.source;

        if (table.has(className)) {
            unit.addError(unit.error(TAG, REDEFINED_CLASS.format(className), decl));
            return null;
        }

        currentClass = new ClassSymbol(className);
        table.add(currentClass);

        if (decl.typeParameters != null) {
            decl.typeParameters.forEach(tp -> tp.accept(this));
        }

        if (decl.supers != null) {
            decl.supers.forEach(s -> s.accept(this));
        }

        if (decl.members != null) {
            decl.members.forEach(member -> member.accept(this));
        }

        currentClass = null;
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl decl) {
        String tpName = decl.name.source;
        TypeParamSymbol tpSymbol = new TypeParamSymbol(tpName);
        currentTypeParameter = tpSymbol;
        decl.supers.forEach(s -> s.accept(this));

        if (currentFunction != null) {
            if (currentFunction.hasTypeParameter(tpName)) {
                unit.addError(unit.error(TAG, REDEFINED_TYPE_PARAMETER.format(tpName), decl));
                currentTypeParameter = null;
                return null;
            }

            currentFunction.addTypeParameter(tpSymbol);
        } else if (currentClass != null) {
            if (currentClass.hasTypeParameter(tpName)) {
                unit.addError(unit.error(TAG, REDEFINED_TYPE_PARAMETER.format(tpName), decl));
                currentTypeParameter = null;
                return null;
            }
            currentClass.addTypeParameter(tpSymbol);
        }

        currentTypeParameter = null;
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl decl) {
        String sName = decl.name.source;
        SuperSymbol sSymbol = new SuperSymbol(sName);
        if (currentTypeParameter != null) {
            if (currentTypeParameter.hasSuper(sName)) {
                unit.addError(unit.error(TAG, REDEFINED_SUPER.format(sName), decl));
                return null;
            }
            currentTypeParameter.addSuper(sSymbol);
        } else if (currentClass != null) {
            if (currentClass.hasSuper(sName)) {
                unit.addError(unit.error(TAG, REDEFINED_SUPER.format(sName), decl));
                return null;
            }
            currentClass.addSuper(sSymbol);
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl decl) {
        String varName = decl.name.source;

        if (currentClass != null) {
            if (currentClass.hasDeclaredMember(varName)) {
                unit.addError(unit.error(TAG, REDEFINED_VARIABLE.format(varName), decl));
                return null;
            }
            VarSymbol varSymbol = new VarSymbol(varName);
            varSymbol.setDeclaringClass(currentClass);
            currentClass.addDeclaredMember(varSymbol);
        }
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl decl) {
        String funName = decl.name.source;

        if (currentClass != null) {
            if (currentClass.hasDeclaredMember(funName)) {
                unit.addError(unit.error(TAG, REDEFINED_FUNCTION.format(funName), decl));
                return null;
            }

            FunSymbol funSymbol = new FunSymbol(funName);
            funSymbol.setDeclaringClass(currentClass);

            currentFunction = funSymbol;
            if (decl.typeParameters != null) {
                decl.typeParameters.forEach(tp -> tp.accept(this));
            }

            if (decl.parameters != null) {
                decl.parameters.forEach(p -> p.accept(this));
            }

            currentClass.addDeclaredMember(currentFunction);
            currentFunction = null;
        }
        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl decl) {
        String pName = decl.name.source;

        if (currentFunction != null) {
            if (currentFunction.hasParameter(pName)) {
                unit.addError(unit.error(TAG, REDEFINED_PARAMETER.format(pName), decl));
                return null;
            }
            currentFunction.addParameter(new ParamSymbol(pName));
        }
        return null;
    }
}