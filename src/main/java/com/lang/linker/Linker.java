package com.lang.linker;

import static com.lang.linker.LinkingErrorCode.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

public class Linker implements DeclVisitor<Void> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private ClassSymbol currentClass;
    private FunSymbol currentFunction;
    private TypeParamSymbol currentTypeParameter;

    private final Consumer<Typed> reporter;

    public Linker(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.reporter = t -> unit.addDiagnostic(TAG, Diagnostic.error(
                t != null ? t.getPosition() : null,
                UNRESOLVED_TYPE.format(t.getName())));

        this.currentClass = null;
        this.currentFunction = null;
        this.currentTypeParameter = null;
    }

    public void link() {
        unit.getClasses().forEach(cls -> cls.accept(this));
    }

    private Map<String, Symbol> substitutions() {
        Map<String, Symbol> subs = new LinkedHashMap<>();
        if (currentClass != null)
            subs.putAll(currentClass.getTypeParameters());
        if (currentFunction != null)
            subs.putAll(currentFunction.getTypeParameters());
        return subs;
    }

    @Override
    public Void visitClassDecl(ClassDecl decl) {
        String className = decl.name.source;
        currentClass = table.get(className);
        if (currentClass == null)
            return null;

        currentClass.node = decl;

        if (decl.typeParameters != null) {
            decl.typeParameters.forEach(tp -> tp.accept(this));
        }

        if (decl.parameters != null) {
            decl.parameters.forEach(param -> param.accept(this));
        }

        if (decl.supers != null) {
            decl.supers.forEach(superClass -> superClass.accept(this));
        }

        if (currentClass != table.getAny() && !currentClass.hasSupers()) {
            ClassSymbol anyClass = table.getAny();
            SuperSymbol anySuper = new SuperSymbol(anyClass.getName());
            anySuper.setType(anyClass);
            currentClass.addSuper(anySuper);
        }

        if (decl.members != null) {
            decl.members.forEach(member -> member.accept(this));
        }

        currentClass.setPure(true);

        for (MemberSymbol member : currentClass.getDeclaredMembers().values()) {
            if (member.isFun()) {
                if (!member.asFun().isPure()) {
                    currentClass.setPure(false);
                    break;
                }
            } else if (member.isVar()) {
                currentClass.setPure(false);
                break;
            }
        }

        currentClass = null;
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl decl) {
        if (currentClass == null)
            return null;

        String superName = decl.type.getName();
        SuperSymbol sSymbol = currentClass.getSuper(superName);
        if (sSymbol == null)
            return null;

        sSymbol.node = decl;

        Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
        if (resolved != null)
            sSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitBoundDecl(BoundDecl decl) {
        if (currentTypeParameter == null)
            return null;

        String boundName = decl.type.getName();
        BoundSymbol bSymbol = currentTypeParameter.getBound(boundName);
        if (bSymbol == null)
            return null;

        bSymbol.node = decl;

        Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
        if (resolved != null)
            bSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl decl) {
        TypeParamSymbol tpSymbol = null;

        if (currentFunction != null) {
            tpSymbol = currentFunction.getTypeParameter(decl.name.source);
        } else if (currentClass != null) {
            tpSymbol = currentClass.getTypeParameter(decl.name.source);
        }

        if (tpSymbol == null)
            return null;

        tpSymbol.node = decl;

        if (decl.bounds == null)
            return null;

        TypeParamSymbol previous = currentTypeParameter;
        currentTypeParameter = tpSymbol;
        decl.bounds.forEach(bound -> bound.accept(this));
        currentTypeParameter = previous;
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl decl) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(decl.name.source);
        if (!(sym instanceof VarSymbol varSymbol))
            return null;

        varSymbol.node = decl;

        if (decl.type == null)
            return null;

        Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
        if (resolved != null)
            varSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl decl) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(decl.name.source);
        if (!(sym instanceof FunSymbol funSymbol))
            return null;

        funSymbol.node = decl;

        FunSymbol previous = currentFunction;
        currentFunction = funSymbol;

        if (decl.type != null) {
            Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
            if (resolved != null)
                currentFunction.setType(resolved);
        }

        if (decl.parameters != null) {
            decl.parameters.forEach(param -> param.accept(this));
        }

        if (decl.typeParameters != null) {
            decl.typeParameters.forEach(tp -> tp.accept(this));
        }

        currentFunction.setPure(decl.body == null);

        currentFunction = previous;
        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl decl) {
        if (currentFunction != null) {
            ParamSymbol pSymbol = currentFunction.getParameter(decl.name.source);
            if (pSymbol == null)
                return null;

            pSymbol.node = decl;

            if (decl.type == null)
                return null;

            Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
            if (resolved != null)
                pSymbol.setType(resolved);

            return null;
        }

        if (currentClass != null) {
            ParamSymbol pSymbol = currentClass.getParameter(decl.name.source);
            if (pSymbol == null)
                return null;

            pSymbol.node = decl;

            if (decl.type == null)
                return null;

            Symbol resolved = table.resolve(decl.type, substitutions(), reporter);
            if (resolved != null)
                pSymbol.setType(resolved);

            return null;
        }

        return null;
    }
}