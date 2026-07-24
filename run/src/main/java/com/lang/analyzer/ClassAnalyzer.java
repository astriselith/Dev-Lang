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

            if (table.has(className)) {
                unit.addError(unit.error(TAG, REDEFINED_CLASS.format(className), cls));
                return;
            }

            System.out.println("Registering class " + className);

            table.add(new ClassSymbol(className,
                    cls.typeParameters != null ? cls.typeParameters.stream().map(tp -> tp.name.source).toList()
                            : new ArrayList<>(),
                    cls.superclasses));
        });

        unit.classes.forEach(cls -> {
            String className = cls.name.source;
            System.out.println("Additing structure to class " + className);

            ClassSymbol classSymbol = table.get(className);
            if (cls.typeParameters != null) {
                cls.typeParameters.forEach(tp -> {
                    String tpName = tp.name.source;

                    if (classSymbol.hasTypeParameter(tpName)) {
                        unit.addError(unit.error(TAG, REDEFINED_PARAMETER.format(tpName), tp));
                        return;
                    }

                    classSymbol.addTypeParameter(new TypeParamSymbol(tpName, tp.superclasses));
                });

                cls.typeParameters.forEach(tp -> {
                    String tpName = tp.name.source;

                    TypeParamSymbol tpSymbol = classSymbol.getTypeParameter(tpName);

                    Map<String, Symbol> tpSubstituitions = new LinkedHashMap<>();
                    tpSubstituitions.put(className, classSymbol);
                    tpSubstituitions.putAll(classSymbol.getTypeParameters());

                    tp.superclasses.forEach(cls0 -> {
                        if (tpSymbol.hasSuperclass(cls0.getName())) {
                            unit.addError(unit.error(TAG, DUPLICATE_SUPERCLASS.format(cls0.getName()), cls0));
                            return;
                        }

                        Symbol resolved = table.resolve(cls0, tpSubstituitions,
                                t -> unit.addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t)));

                        if (resolved != null) {
                            if (resolved.isTypeParam() && resolved == tpSymbol || resolved.isParameterized()) {
                                unit.addError(unit.error(TAG, SELF_REFERENTIAL_TYPE_PARAMETER.format(tpName), tp));
                            } else {
                                tpSymbol.addSuperclass(resolved);
                            }
                        }
                    });

                });
            }
            Map<String, Symbol> clsSubstituitions = new LinkedHashMap<>();
            clsSubstituitions.put(className, classSymbol);
            clsSubstituitions.putAll(classSymbol.getTypeParameters());

            if (cls.superclasses != null) {
                cls.superclasses.forEach(cls0 -> {
                    if (classSymbol.hasSuperclass(cls0.getName())) {
                        unit.addError(unit.error(TAG, DUPLICATE_SUPERCLASS.format(cls0.getName()), cls0));
                        return;
                    }

                    Symbol resolved = table.resolve(cls0, clsSubstituitions,
                            t -> unit.addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t)));

                    if (resolved != null) {
                        if (resolved.isClass() && resolved == classSymbol
                                || resolved.isParameterized() && resolved.asParameterized().getBase() == classSymbol) {
                            unit.addError(unit.error(TAG, CIRCULAR_INHERITANCE.format(className), cls0));
                        } else {
                            classSymbol.addSuperclass(resolved);
                        }
                    }
                });
            }

            if (cls.vars != null) {
                cls.vars.forEach(var -> {
                    String varName = var.name.source;
                    Typed typeType = var.type;

                    if (classSymbol.hasDeclaredVar(varName)) {
                        unit.addError(unit.error(TAG, REDEFINED_VARIABLE.format(varName), var));
                        return;
                    }

                    classSymbol.addDeclaredVar(new VarSymbol(varName, typeType));
                });

                cls.vars.forEach(var -> {
                    String varName = var.name.source;
                    Typed typeType = var.type;

                    VarSymbol varSymbol = classSymbol.getDeclaredVar(varName);

                    varSymbol.setType(table.resolve(typeType, clsSubstituitions,
                            t -> unit.addError(unit.error(TAG, UNRESOLVED_TYPE.format(t.getName()), t))));
                });
            }

            if (cls.funs != null) {
                cls.funs.forEach(fun -> {
                    String funName = fun.name.source;
                    List<String> typeParameterTypes = fun.typeParameters != null
                            ? fun.typeParameters.stream().map(tp -> tp.name.source).toList()
                            : new ArrayList<>();
                    List<String> parameterTypes = fun.parameters != null
                            ? fun.parameters.stream().map(p -> p.name.source).toList()
                            : new ArrayList<>();
                    Typed returnTypeType = fun.returnType;

                    if (classSymbol.hasDeclaredFun(funName)) {
                        unit.addError(unit.error(TAG, REDEFINED_FUNCTION.format(funName), fun));
                    }

                    classSymbol
                            .addDeclaredFun(new FunSymbol(funName, typeParameterTypes, parameterTypes, returnTypeType));

                });
            }

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
