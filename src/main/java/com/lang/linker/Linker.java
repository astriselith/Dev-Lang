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

    @Override
    public Void visitClassDecl(ClassDecl decl) {
        String className = decl.name.source;
        currentClass = table.get(className);
        if (currentClass == null)
            return null;

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
                // Qualquer campo quebra pureza.
                currentClass.setPure(false);
                break;
            }
        }

        currentClass = null;
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl decl) {
        String superName = decl.type.getName();

        SuperSymbol sSymbol;
        if (currentTypeParameter != null) {
            sSymbol = currentTypeParameter.getSuper(superName);
        } else if (currentClass != null) {
            sSymbol = currentClass.getSuper(superName);
        } else {
            return null;
        }

        if (sSymbol == null)
            return null;

        Map<String, Symbol> substitutions = new LinkedHashMap<>();

        if (currentClass != null)
            substitutions.putAll(currentClass.getTypeParameters());

        if (currentFunction != null)
            substitutions.putAll(currentFunction.getTypeParameters());

        Symbol resolved = table.resolve(decl.type, substitutions, reporter);
        if (resolved != null)
            sSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl decl) {
        if (currentFunction != null) {
            TypeParamSymbol tpSymbol = currentFunction.getTypeParameter(decl.name.source);
            if (tpSymbol == null)
                return null;

            if (decl.supers == null)
                return null;

            currentTypeParameter = tpSymbol;
            decl.supers.forEach(superClass -> superClass.accept(this));
            currentTypeParameter = null;
        } else if (currentClass != null) {
            TypeParamSymbol tpSymbol = currentClass.getTypeParameter(decl.name.source);
            if (tpSymbol == null)
                return null;

            if (decl.supers == null)
                return null;

            currentTypeParameter = tpSymbol;
            decl.supers.forEach(superClass -> superClass.accept(this));
            currentTypeParameter = null;
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl decl) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(decl.name.source);
        if (!(sym instanceof VarSymbol))
            return null;

        VarSymbol varSymbol = (VarSymbol) sym;
        if (decl.type == null)
            return null;

        Map<String, Symbol> substitutions = new LinkedHashMap<>();

        if (currentClass != null)
            substitutions.putAll(currentClass.getTypeParameters());

        if (currentFunction != null)
            substitutions.putAll(currentFunction.getTypeParameters());

        Symbol resolved = table.resolve(decl.type, substitutions, reporter);
        if (resolved != null)
            varSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl decl) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(decl.name.source);
        if (!(sym instanceof FunSymbol))
            return null;

        currentFunction = (FunSymbol) sym;

        Map<String, Symbol> substitutions = new LinkedHashMap<>();
        if (currentClass != null)
            substitutions.putAll(currentClass.getTypeParameters());
        if (currentFunction != null)
            substitutions.putAll(currentFunction.getTypeParameters());

        if (decl.type != null) {
            Symbol resolved = table.resolve(decl.type, substitutions, reporter);
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

        currentFunction = null;
        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl decl) {
        if (currentFunction != null) {
            ParamSymbol pSymbol = currentFunction.getParameter(decl.name.source);
            if (pSymbol == null)
                return null;

            Map<String, Symbol> substitutions = new LinkedHashMap<>();
            if (currentClass != null)
                substitutions.putAll(currentClass.getTypeParameters());
            if (currentFunction != null)
                substitutions.putAll(currentFunction.getTypeParameters());

            if (decl.type != null) {
                Symbol resolved = table.resolve(decl.type, substitutions, reporter);
                if (resolved != null)
                    pSymbol.setType(resolved);
            }
        } else if (currentClass != null) {
            ParamSymbol pSymbol = currentClass.getParameter(decl.name.source);
            if (pSymbol == null)
                return null;

            Map<String, Symbol> substitutions = new LinkedHashMap<>();
            if (currentClass != null)
                substitutions.putAll(currentClass.getTypeParameters());
            if (currentFunction != null)
                substitutions.putAll(currentFunction.getTypeParameters());

            if (decl.type != null) {
                Symbol resolved = table.resolve(decl.type, substitutions, reporter);
                if (resolved != null)
                    pSymbol.setType(resolved);
            }
        }
        return null;
    }
}