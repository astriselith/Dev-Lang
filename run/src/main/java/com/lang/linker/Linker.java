package com.lang.linker;

import static com.lang.linker.LinkingErrorCode.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class Linker implements DefaultStmtVisitor<Void> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private ClassSymbol currentClass;
    private FunSymbol currentFunction;
    private Map<String, Symbol> substitutions;

    private final Consumer<Typed> reporter;

    public Linker(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.reporter = t -> unit.addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t));

        this.currentClass = null;
        this.currentFunction = null;
        this.substitutions = new LinkedHashMap<>();
    }

    public void link() {
        unit.classes.forEach(cls -> cls.accept(this));
    }

    @Override
    public Void visitClassDeclStmt(ClassDeclStmt stmt) {
        String className = stmt.name.source;
        currentClass = table.get(className);
        if (currentClass == null)
            return null;

        substitutions = new LinkedHashMap<>();
        substitutions.put(className, currentClass);
        substitutions.putAll(currentClass.getTypeParameters());

        if (stmt.typeParameters != null) {
            stmt.typeParameters.forEach(tp -> tp.accept(this));
        }

        if (stmt.superclasses != null) {
            stmt.superclasses.forEach(superClass -> {
                String name = superClass.getName();
                if (currentClass.hasSuperclass(name)) {
                    unit.addError(unit.error(TAG, DUPLICATE_SUPERCLASS.format(name), superClass));
                    return;
                }

                Symbol resolved = table.resolve(superClass, substitutions, reporter);
                if (resolved == null)
                    return;
                if (resolved.isClass() && resolved == currentClass) {
                    unit.addError(unit.error(TAG, CIRCULAR_INHERITANCE.format(className), superClass));
                    return;
                }
                currentClass.addSuperclass(resolved);
            });
        }

        if (stmt.members != null) {
            stmt.members.forEach(member -> member.accept(this));
        }

        currentClass = null;
        substitutions = null;
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
        if (currentClass != null) {
            TypeParamSymbol tpSymbol = currentClass.getTypeParameter(stmt.name.source);
            if (tpSymbol == null)
                return null;

            if (stmt.superclasses == null)
                return null;

            stmt.superclasses.forEach(superClass -> {
                String name = superClass.getName();
                if (tpSymbol.hasSuperclass(name)) {
                    unit.addError(unit.error(TAG, DUPLICATE_SUPERCLASS.format(name), superClass));
                    return;
                }

                Symbol resolved = table.resolve(superClass, substitutions, reporter);
                if (resolved == null)
                    return;
                if (resolved.isTypeParam() && resolved == tpSymbol) {
                    unit.addError(unit.error(TAG, SELF_REFERENTIAL_TYPE_PARAMETER.format(stmt.name.source), stmt));
                    return;
                }
                tpSymbol.addSuperclass(resolved);
            });
        } else if (currentFunction != null) {
            TypeParamSymbol tpSymbol = currentFunction.getTypeParameter(stmt.name.source);
            if (tpSymbol == null)
                return null;

            if (stmt.superclasses == null)
                return null;

            Map<String, Symbol> funSubs = new LinkedHashMap<>(substitutions);
            funSubs.putAll(currentFunction.getTypeParameters());

            stmt.superclasses.forEach(superClass -> {
                String name = superClass.getName();
                if (tpSymbol.hasSuperclass(name)) {
                    unit.addError(unit.error(TAG, DUPLICATE_SUPERCLASS.format(name), superClass));
                    return;
                }

                Symbol resolved = table.resolve(superClass, funSubs, reporter);
                if (resolved == null)
                    return;
                if (resolved.isTypeParam() && resolved == tpSymbol) {
                    unit.addError(unit.error(TAG, SELF_REFERENTIAL_TYPE_PARAMETER.format(stmt.name.source), stmt));
                    return;
                }
                tpSymbol.addSuperclass(resolved);
            });
        }
        return null;
    }

    @Override
    public Void visitVarDeclStmt(VarDeclStmt stmt) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(stmt.name.source);
        if (!(sym instanceof VarSymbol))
            return null;

        VarSymbol varSymbol = (VarSymbol) sym;
        if (stmt.type == null)
            return null;

        Symbol resolved = table.resolve(stmt.type, substitutions, reporter);
        if (resolved != null)
            varSymbol.setType(resolved);

        return null;
    }

    @Override
    public Void visitFunDeclStmt(FunDeclStmt stmt) {
        if (currentClass == null)
            return null;

        Symbol sym = currentClass.getDeclaredMember(stmt.name.source);
        if (!(sym instanceof FunSymbol))
            return null;

        currentFunction = (FunSymbol) sym;

        Map<String, Symbol> funSubs = new LinkedHashMap<>(substitutions);
        funSubs.putAll(currentFunction.getTypeParameters());

        if (stmt.returnType != null) {
            Symbol resolved = table.resolve(stmt.returnType, funSubs, reporter);
            if (resolved != null)
                currentFunction.setReturnType(resolved);
        }

        if (stmt.parameters != null) {
            stmt.parameters.forEach(param -> param.accept(this));
        }

        if (stmt.typeParameters != null) {
            stmt.typeParameters.forEach(tp -> tp.accept(this));
        }

        currentFunction = null;
        return null;
    }

    @Override
    public Void visitParamDeclStmt(ParamDeclStmt stmt) {
        if (currentFunction == null)
            return null;

        if (stmt.type == null)
            return null;

        ParamSymbol paramSymbol = currentFunction.getParameter(stmt.name.source);
        if (paramSymbol == null)
            return null;

        Map<String, Symbol> funSubs = new LinkedHashMap<>(substitutions);
        funSubs.putAll(currentFunction.getTypeParameters());

        Symbol resolved = table.resolve(stmt.type, funSubs, reporter);
        if (resolved != null)
            paramSymbol.setType(resolved);

        return null;
    }
}