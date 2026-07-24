package com.lang.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;

public class ClassAnalyzer implements StmtVisitor<Void>, ExprVisitor<Void> {

    private final SymbolTable table;
    private final CompilationUnit unit;

    public ClassAnalyzer(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
    }

    public void analyze() {
        unit.classes.forEach(cls -> {
            String className = cls.name.source;
            List<String> typeParameterTypes = cls.typeParameters.stream().map(tp -> tp.name.source).toList();
            List<Typed> superclassTypes = cls.superclasses;

            if (table.classes.containsKey(className)) {
                unit.error(TAG, REDEFINED_CLASS.format(className), cls);
                return;
            }

            table.classes.put(className, new ClassSymbol(className, typeParameterTypes, superclassTypes));
        });

        unit.classes.forEach(cls -> {
            String className = cls.name.source;

            ClassSymbol classSymbol = table.get(className);

            cls.typeParameters.forEach(tp -> {
                String tpName = tp.name.source;
                List<Typed> superclassTypes = tp.superclasses;

                if (classSymbol.hasTypeParameter(tpName)) {
                    unit.error(TAG, REDEFINED_PARAMETER.format(tp.name.source), tp);
                    return;
                }

                classSymbol.addTypeParameter(tpName, new TypeParamSymbol(tpName, superclassTypes));
            });

            cls.typeParameters.forEach(tp -> {
                String tpName = tp.name.source;

                TypeParamSymbol tpSymbol = classSymbol.getTypeParameter(tpName);

                Map<String, Symbol> tpSubstituitions = new LinkedHashMap<>();
                tpSubstituitions.put(className, classSymbol);
                tpSubstituitions.putAll(classSymbol.getTypeParameters());

                tp.superclasses.forEach(cls0 -> {
                    if (tpSymbol.hasSuperclass(cls0.getName())) {
                        unit.error(TAG, DUPLICATE_SUPERCLASS.format(cls0.getName()), cls0);
                        return;
                    }

                    Symbol resolved = table.resolve(cls0, tpSubstituitions);

                    if (resolved != null) {
                        if (resolved.isTypeParam() && resolved == tpSymbol) {
                            unit.error(TAG, SELF_REFERENTIAL_TYPE_PARAMETER.format(tpName), tp);
                        } else {
                            tpSymbol.addSuperclass(resolved);
                        }
                    } else {
                        unit.error(TAG, UNRESOLVED_SUPERCLASS.format(cls0.getName()), cls0);
                    }
                });

            });

            Map<String, Symbol> clsSubstituitions = new LinkedHashMap<>();
            clsSubstituitions.put(className, classSymbol);
            clsSubstituitions.putAll(classSymbol.getTypeParameters());

            cls.superclasses.forEach(cls0 -> {
                if (classSymbol.hasSuperclass(cls0.getName())) {
                    unit.error(TAG, DUPLICATE_SUPERCLASS.format(cls0.getName()), cls0);
                    return;
                }

                Symbol resolved = table.resolve(cls0, clsSubstituitions);
                if (resolved != null) {
                    if (resolved.isClass() && resolved == classSymbol
                            || resolved.isParameterized() && resolved.asParameterized().getBase() == classSymbol) {
                        unit.error(TAG, CIRCULAR_INHERITANCE.format(className), cls0);
                    } else {
                        classSymbol.addSuperclass(resolved);
                    }
                } else {
                    unit.error(TAG, UNRESOLVED_SUPERCLASS.format(cls0.getName()), cls0);
                }
            });

        });
    }

    @Override
    public Void visitBlockStmt(BlockStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitBreakStmt(BreakStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitClassDeclStmt(ClassDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitContinueStmt(ContinueStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitFunDeclStmt(FunDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLetDeclStmt(LetDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitParamDeclStmt(ParamDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitVarDeclStmt(VarDeclStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt tp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitAssignExpr(AssignExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitBinaryExpr(BinaryExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitCallExpr(CallExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIndexAccessExpr(IndexAccessExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLiteralExpr(LiteralExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitMemberAccessExpr(MemberAccessExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitParenthesisExpr(ParenthesisExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitRefExpr(RefExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTernaryExpr(TernaryExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitUnaryExpr(UnaryExpr expr) {
        // TODO Auto-generated method stub
        return null;
    }

}
