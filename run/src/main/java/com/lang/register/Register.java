package com.lang.register;

import static com.lang.register.RegisteringErrorCode.*;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class Register implements DefaultStmtVisitor<Void> {

    private final SymbolTable table;
    private final CompilationUnit unit;
    private ClassSymbol currentClass;
    private FunSymbol currentFunction;

    public Register(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
        this.currentClass = null;
        this.currentFunction = null;
    }

    public void register() {
        unit.classes.forEach(cls -> cls.accept(this));
    }

    @Override
    public Void visitClassDeclStmt(ClassDeclStmt stmt) {
        String className = stmt.name.source;

        if (table.has(className)) {
            unit.addError(unit.error(TAG, REDEFINED_CLASS.format(className), stmt));
            return null;
        }

        currentClass = new ClassSymbol(className);
        table.add(currentClass);

        if (stmt.typeParameters != null) {
            stmt.typeParameters.forEach(tp -> tp.accept(this));
        }

        if (stmt.members != null) {
            stmt.members.forEach(member -> member.accept(this));
        }

        currentClass = null;
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
        String tpName = stmt.name.source;

        if (currentClass != null) {
            if (currentClass.hasTypeParameter(tpName)) {
                unit.addError(unit.error(TAG, REDEFINED_TYPE_PARAMETER.format(tpName), stmt));
                return null;
            }
            currentClass.addTypeParameter(new TypeParamSymbol(tpName));
        } else if (currentFunction != null) {
            if (currentFunction.hasTypeParameter(tpName)) {
                unit.addError(unit.error(TAG, REDEFINED_TYPE_PARAMETER.format(tpName), stmt));
                return null;
            }
            currentFunction.addTypeParameter(new TypeParamSymbol(tpName));
        }
        return null;
    }

    @Override
    public Void visitVarDeclStmt(VarDeclStmt stmt) {
        String varName = stmt.name.source;

        if (currentClass != null) {
            if (currentClass.hasDeclaredMember(varName)) {
                unit.addError(unit.error(TAG, REDEFINED_VARIABLE.format(varName), stmt));
                return null;
            }
            currentClass.addDeclaredMember(new VarSymbol(varName));
        }
        return null;
    }

    @Override
    public Void visitFunDeclStmt(FunDeclStmt stmt) {
        String funName = stmt.name.source;

        if (currentClass != null) {
            if (currentClass.hasDeclaredMember(funName)) {
                unit.addError(unit.error(TAG, REDEFINED_FUNCTION.format(funName), stmt));
                return null;
            }

            currentFunction = new FunSymbol(funName);

            if (stmt.typeParameters != null) {
                stmt.typeParameters.forEach(tp -> tp.accept(this));
            }

            if (stmt.parameters != null) {
                stmt.parameters.forEach(p -> p.accept(this));
            }

            currentClass.addDeclaredMember(currentFunction);
            currentFunction = null;
        }
        return null;
    }

    @Override
    public Void visitParamDeclStmt(ParamDeclStmt stmt) {
        String pName = stmt.name.source;

        if (currentFunction != null) {
            if (currentFunction.hasParameter(pName)) {
                unit.addError(unit.error(TAG, REDEFINED_PARAMETER.format(pName), stmt));
                return null;
            }
            currentFunction.addParameter(new ParamSymbol(pName));
        }
        return null;
    }
}