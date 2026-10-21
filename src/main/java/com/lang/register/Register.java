package com.lang.register;

import static com.lang.register.RegisteringErrorCode.*;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

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
        unit.getClasses().forEach(cls -> cls.accept(this));
    }

    @Override
    public Void visitClassDecl(ClassDecl decl) {
        String className = decl.name.source;

        if (table.has(className)) {
            unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                    REDEFINED_CLASS.format(className)));
            return null;
        }

        currentClass = new ClassSymbol(className);
        currentClass.node = decl;
        table.add(currentClass);

        if (decl.typeParameters != null) {
            decl.typeParameters.forEach(tp -> tp.accept(this));
        }

        if (decl.parameters != null) {
            decl.parameters.forEach(p -> p.accept(this));
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
        tpSymbol.node = decl;
        currentTypeParameter = tpSymbol;

        if (decl.bounds != null) {
            decl.bounds.forEach(b -> b.accept(this));
        }

        if (currentFunction != null) {
            if (currentFunction.hasTypeParameter(tpName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_TYPE_PARAMETER.format(tpName)));
                currentTypeParameter = null;
                return null;
            }
            currentFunction.addTypeParameter(tpSymbol);
        } else if (currentClass != null) {
            if (currentClass.hasTypeParameter(tpName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_TYPE_PARAMETER.format(tpName)));
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
        String sName = decl.type.getName();
        SuperSymbol sSymbol = new SuperSymbol(sName);
        sSymbol.node = decl;

        if (currentClass != null) {
            if (currentClass.hasSuper(sName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_SUPER.format(sName)));
                return null;
            }
            currentClass.addSuper(sSymbol);
        }
        return null;
    }

    @Override
    public Void visitBoundDecl(BoundDecl decl) {
        String bName = decl.type.getName();
        BoundSymbol bSymbol = new BoundSymbol(bName);
        bSymbol.node = decl;

        if (currentTypeParameter != null) {
            if (currentTypeParameter.hasBound(bName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_BOUND.format(bName)));
                return null;
            }
            currentTypeParameter.addBound(bSymbol);
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl decl) {
        String varName = decl.name.source;

        if (currentClass != null) {
            if (currentClass.hasDeclaredMember(varName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_VARIABLE.format(varName)));
                return null;
            }
            VarSymbol varSymbol = new VarSymbol(varName);
            varSymbol.node = decl;
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
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_FUNCTION.format(funName)));
                return null;
            }

            FunSymbol funSymbol = new FunSymbol(funName);
            funSymbol.node = decl;
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
        ParamSymbol pSymbol = new ParamSymbol(pName);
        pSymbol.node = decl;

        if (currentFunction != null) {
            if (currentFunction.hasTypeParameter(pName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_PARAMETER.format(pName)));
                currentTypeParameter = null;
                return null;
            }
            currentFunction.addParameter(pSymbol);
        } else if (currentClass != null) {
            if (currentClass.hasTypeParameter(pName)) {
                unit.addDiagnostic(TAG, Diagnostic.error(decl.getPosition(),
                        REDEFINED_PARAMETER.format(pName)));
                currentTypeParameter = null;
                return null;
            }
            currentClass.addParameter(pSymbol);
        }

        currentTypeParameter = null;
        return null;
    }
}