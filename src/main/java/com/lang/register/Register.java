package com.lang.register;

import static com.lang.register.RegisteringErrorCode.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;
import com.lang.util.Positioned;

public class Register implements DeclVisitor<Void> {
    private final SymbolTable table;
    private final CompilationUnit unit;
    private ClassSymbol currentClass;
    private FunSymbol currentFunction;
    private OperatorSymbol currentOperator;
    private TypeParamSymbol currentTypeParameter;

    public Register(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
    }

    public void register() {
        for (ClassDecl decl : unit.getClasses()) {
            String name = decl.name.source;
            if (table.hasClass(name)) {
                report(decl, REDEFINED_CLASS.format(name));
                continue;
            }

            ClassSymbol symbol = new ClassSymbol(name);
            symbol.node = decl;
            if (!table.addClass(symbol))
                continue;

            ClassSymbol previousClass = currentClass;
            currentClass = symbol;
            decl.symbol = symbol;
            registerTypeParameters(decl.typeParameters, symbol, null);
            registerClassParameters(decl.parameters);
            registerSupers(decl.supers);
            if (decl.members != null) {
                for (MemberDecl member : decl.members) {
                    if (member instanceof VarDecl variable)
                        registerClassVariable(variable);
                    else if (member instanceof FunDecl function)
                        registerClassFunction(function);
                    else if (member instanceof OperatorDecl operator)
                        registerClassOperator(operator);
                }
            }
            currentClass = previousClass;
        }

        for (VarDecl declaration : unit.getVars()) {
            String name = declaration.name.source;
            if (table.getVar(name) != null || table.getFun(name) != null) {
                report(declaration, REDEFINED_VARIABLE.format(name));
                continue;
            }
            VarSymbol symbol = new VarSymbol(name);
            symbol.node = declaration;
            symbol.setInit(declaration.value);
            if (table.addVar(symbol))
                declaration.symbol = symbol;
        }
        for (FunDecl declaration : unit.getFuns()) {
            String name = declaration.name.source;
            if (table.getFun(name) != null || table.getVar(name) != null) {
                report(declaration, REDEFINED_FUNCTION.format(name));
                continue;
            }
            FunSymbol symbol = new FunSymbol(name);
            symbol.node = declaration;
            symbol.setBody(declaration.body);
            if (!table.addFun(symbol))
                continue;
            declaration.symbol = symbol;
            registerTypeParameters(declaration.typeParameters, null, symbol);
            registerParameters(declaration.parameters, symbol);
        }
        for (OperatorDecl declaration : unit.getOperators()) {
            String signature = operatorSignature(declaration);
            if (table.getOperator(signature) != null) {
                report(declaration, REDEFINED_OPERATOR.format(declaration.operator.lexeme));
                continue;
            }
            OperatorSymbol symbol = new OperatorSymbol(declaration.kind, declaration.operator);
            symbol.node = declaration;
            if (!table.addOperator(signature, symbol))
                continue;
            declaration.symbol = symbol;
            registerTypeParameters(declaration.typeParameters, null, symbol);
            registerOperatorParameters(declaration.operands, symbol);
        }

        for (ClassDecl declaration : unit.getClasses()) {
            if (declaration.symbol instanceof ClassSymbol symbol && symbol.node == declaration)
                declaration.accept(this);
        }
        for (VarDecl declaration : unit.getVars())
            declaration.accept(this);
        for (FunDecl declaration : unit.getFuns())
            declaration.accept(this);
        for (OperatorDecl declaration : unit.getOperators())
            declaration.accept(this);
    }

    private void report(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.error(
                node != null ? node.getPosition() : null, message));
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
        return table.resolve(type, substitutions(), unresolvedType -> {
            String name = unresolvedType != null ? unresolvedType.getName() : "<unknown>";
            report(unresolvedType, UNRESOLVED_TYPE.format(name));
        });
    }

    private String operatorSignature(OperatorDecl declaration) {
        return OperatorSymbol.signatureKey(declaration.kind, declaration.operator,
                declaration.operands.stream().map(operand -> operand.type).toList());
    }

    private void registerTypeParameters(List<TypeParamDecl> declarations, ClassSymbol classOwner,
            MemberSymbol member) {
        if (declarations == null)
            return;
        for (TypeParamDecl declaration : declarations) {
            String name = declaration.name.source;
            boolean alreadyDefined = classOwner != null
                    ? classOwner.hasTypeParameter(name)
                    : member instanceof FunSymbol function
                            ? function.hasTypeParameter(name)
                            : member instanceof OperatorSymbol operator
                                    && operator.hasTypeParameter(name);
            if (alreadyDefined) {
                report(declaration, REDEFINED_TYPE_PARAMETER.format(name));
                continue;
            }

            TypeParamSymbol parameter = new TypeParamSymbol(name);
            parameter.node = declaration;
            declaration.symbol = parameter;
            if (classOwner != null)
                classOwner.addTypeParameter(parameter);
            else if (member instanceof FunSymbol function)
                function.addTypeParameter(parameter);
            else if (member instanceof OperatorSymbol operator)
                operator.addTypeParameter(parameter);
        }
    }

    private void registerClassParameters(List<ParamDecl> declarations) {
        if (declarations == null)
            return;
        for (ParamDecl declaration : declarations) {
            String name = declaration.name.source;
            if (currentClass.hasParameter(name) || currentClass.hasTypeParameter(name)) {
                report(declaration, REDEFINED_PARAMETER.format(name));
                continue;
            }
            ParamSymbol parameter = declareParameter(declaration);
            currentClass.addParameter(parameter);
        }
    }

    private void registerSupers(List<SuperDecl> declarations) {
        if (declarations == null)
            return;
        for (SuperDecl declaration : declarations) {
            String name = declaration.type.getName();
            if (currentClass.hasSuper(name)) {
                report(declaration, REDEFINED_SUPER.format(name));
                continue;
            }
            SuperSymbol symbol = new SuperSymbol(name);
            symbol.node = declaration;
            currentClass.addSuper(symbol);
            declaration.symbol = symbol;
        }
    }

    private void registerClassVariable(VarDecl declaration) {
        String name = declaration.name.source;
        if (currentClass.hasDeclaredFun(name) || currentClass.hasDeclaredVar(name)) {
            report(declaration, REDEFINED_VARIABLE.format(name));
            return;
        }
        VarSymbol symbol = new VarSymbol(name);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        symbol.setInit(declaration.value);
        currentClass.addDeclaredVar(symbol);
        declaration.symbol = symbol;
    }

    private void registerClassFunction(FunDecl declaration) {
        String name = declaration.name.source;
        if (currentClass.hasDeclaredFun(name) || currentClass.hasDeclaredVar(name)) {
            report(declaration, REDEFINED_FUNCTION.format(name));
            return;
        }
        FunSymbol symbol = new FunSymbol(name);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        symbol.setBody(declaration.body);
        currentClass.addDeclaredFun(symbol);
        declaration.symbol = symbol;

        FunSymbol previousFunction = currentFunction;
        currentFunction = symbol;
        registerTypeParameters(declaration.typeParameters, null, symbol);
        registerParameters(declaration.parameters, symbol);
        symbol.setPure(declaration.body == null);
        currentFunction = previousFunction;
    }

    private void registerClassOperator(OperatorDecl declaration) {
        String signature = operatorSignature(declaration);
        if (currentClass.hasDeclaredOperator(signature)) {
            report(declaration, REDEFINED_OPERATOR.format(declaration.operator.lexeme));
            return;
        }
        OperatorSymbol symbol = new OperatorSymbol(declaration.kind, declaration.operator);
        symbol.node = declaration;
        symbol.setDeclaringClass(currentClass);
        currentClass.addDeclaredOperator(signature, symbol);
        declaration.symbol = symbol;

        OperatorSymbol previousOperator = currentOperator;
        currentOperator = symbol;
        registerTypeParameters(declaration.typeParameters, null, symbol);
        registerOperatorParameters(declaration.operands, symbol);
        currentOperator = previousOperator;
    }

    private void registerParameters(List<ParamDecl> declarations, FunSymbol function) {
        if (declarations == null)
            return;
        for (ParamDecl declaration : declarations) {
            String name = declaration.name.source;
            if (function.hasParameter(name) || function.hasTypeParameter(name)) {
                report(declaration, REDEFINED_PARAMETER.format(name));
                continue;
            }
            function.addParameter(declareParameter(declaration));
        }
    }

    private ParamSymbol declareParameter(ParamDecl declaration) {
        ParamSymbol parameter = new ParamSymbol(declaration.name.source);
        parameter.node = declaration;
        parameter.setInit(declaration.init);
        declaration.symbol = parameter;
        return parameter;
    }

    private void registerOperatorParameter(ParamDecl declaration, OperatorSymbol operator) {
        if (declaration == null)
            return;
        String name = declaration.name.source;
        if (operator.getParameter(name) != null || operator.hasTypeParameter(name)) {
            report(declaration, REDEFINED_PARAMETER.format(name));
            return;
        }
        operator.addParameter(declareParameter(declaration));
    }

    private void registerOperatorParameters(List<ParamDecl> declarations, OperatorSymbol operator) {
        if (declarations != null)
            declarations.forEach(declaration -> registerOperatorParameter(declaration, operator));
    }

    private void addImplicitAnySuper() {
        ClassSymbol any = table.getAny();
        if (currentClass == any || currentClass.hasSupers() || any == null)
            return;
        SuperSymbol implicitSuper = new SuperSymbol(any.getName());
        implicitSuper.setType(any);
        currentClass.addSuper(implicitSuper);
    }

    private void updatePurity() {
        currentClass.setPure(currentClass.getDeclaredVars().isEmpty()
                && currentClass.getDeclaredFuns().values().stream().allMatch(FunSymbol::isPure));
    }

    @Override
    public Void visitClassDecl(ClassDecl declaration) {
        if (currentClass != null || !(declaration.symbol instanceof ClassSymbol symbol)
                || symbol.node != declaration)
            return null;

        currentClass = symbol;
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(parameter -> parameter.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(parameter -> parameter.accept(this));
        if (declaration.supers != null)
            declaration.supers.forEach(superDeclaration -> superDeclaration.accept(this));
        if (declaration.members != null)
            declaration.members.forEach(member -> member.accept(this));
        addImplicitAnySuper();
        updatePurity();
        currentClass = null;
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl declaration) {
        if (currentClass == null && currentFunction == null && currentOperator == null
                || !(declaration.symbol instanceof TypeParamSymbol parameter))
            return null;
        TypeParamSymbol previous = currentTypeParameter;
        currentTypeParameter = parameter;
        if (declaration.bounds != null)
            for (Typed boundType : declaration.bounds) {
                String name = boundType.getName();
                if (parameter.hasBound(name)) {
                    report(boundType, REDEFINED_BOUND.format(name));
                    continue;
                }
                Symbol type = resolveType(boundType);
                if (type == null)
                    continue;
                BoundSymbol bound = new BoundSymbol(name);
                bound.setType(type);
                parameter.addBound(bound);
            }
        currentTypeParameter = previous;
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl declaration) {
        if (currentClass == null || !(declaration.symbol instanceof SuperSymbol symbol))
            return null;
        Symbol type = resolveType(declaration.type);
        if (type != null)
            symbol.setType(type);
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl declaration) {
        if (currentFunction != null || currentOperator != null)
            return null;
        VarSymbol symbol;
        if (currentClass == null) {
            if (!(declaration.symbol instanceof VarSymbol declared)
                    || declared.node != declaration)
                return null;
            symbol = declared;
        } else if (declaration.symbol instanceof VarSymbol declared
                && declared.node == declaration) {
            symbol = declared;
        } else {
            return null;
        }
        if (declaration.type != null) {
            Symbol type = resolveType(declaration.type);
            if (type != null)
                symbol.setType(type);
        } else if (declaration.value instanceof LiteralExpr literal) {
            String typeName = literal.getTypeName();
            Symbol type = typeName != null
                    ? resolveType(new RefTyped(
                            new Identifier(typeName, literal.getPosition()),
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
        if (currentFunction != null || currentOperator != null)
            return null;
        FunSymbol function;
        if (currentClass == null) {
            if (!(declaration.symbol instanceof FunSymbol declared)
                    || declared.node != declaration)
                return null;
            function = declared;
        } else if (declaration.symbol instanceof FunSymbol declared
                && declared.node == declaration) {
            function = declared;
        } else {
            return null;
        }

        FunSymbol previousFunction = currentFunction;
        currentFunction = function;
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(parameter -> parameter.accept(this));

        if (declaration.parameters != null) {
            declaration.parameters.forEach(parameter -> parameter.accept(this));
        }
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
        if (currentClass == null && currentFunction == null && currentOperator == null
                || !(declaration.symbol instanceof ParamSymbol parameter)
                || declaration.type == null)
            return null;
        Symbol type = resolveType(declaration.type);
        if (type != null)
            parameter.setType(type);
        return null;
    }

    @Override
    public Void visitOperatorDecl(OperatorDecl declaration) {
        if (currentFunction != null || currentOperator != null)
            return null;
        OperatorSymbol operator;
        if (currentClass == null) {
            if (!(declaration.symbol instanceof OperatorSymbol declared)
                    || declared.node != declaration)
                return null;
            operator = declared;
        } else if (declaration.symbol instanceof OperatorSymbol declared
                && declared.node == declaration) {
            operator = declared;
        } else {
            return null;
        }

        OperatorSymbol previousOperator = currentOperator;
        currentOperator = operator;
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(parameter -> parameter.accept(this));
        if (declaration.operands != null)
            declaration.operands.forEach(operand -> operand.accept(this));
        if (declaration.type != null) {
            Symbol type = resolveType(declaration.type);
            if (type != null)
                operator.setType(type);
        }
        currentOperator = previousOperator;
        return null;
    }

}
