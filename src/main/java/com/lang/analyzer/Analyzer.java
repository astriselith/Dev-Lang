package com.lang.analyzer;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lang.ast.*;
import com.lang.symbol.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;
import com.lang.util.Positioned;

public class Analyzer implements DeclVisitor<Void>, StmtVisitor<Void>, ExprVisitor<SymbolResult> {
    private record OperatorResolution(OperatorSymbol symbol, Map<String, Symbol> substitutions) {
    }

    private final SymbolTable table;
    private final CompilationUnit unit;
    private final SymbolScope global = new SymbolScope("global");
    private final Deque<SymbolScope> scopes = new ArrayDeque<>();
    private final Deque<Map<String, Symbol>> substitutions = new ArrayDeque<>();

    private final Set<ClassSymbol> analyzedAnnotationDefaults = new HashSet<>();
    private final Set<TypeParamSymbol> analyzedTypeParams = new HashSet<>();

    private ClassSymbol currentClass;
    private VarSymbol currentVariable;
    private CallableSymbol currentCallable;
    private int loopDepth;

    public Analyzer(SymbolTable table, CompilationUnit unit) {
        this.table = table;
        this.unit = unit;
    }

    public void analyze() {
        scopes.push(global);
        substitutions.push(new LinkedHashMap<>());

        for (ClassDecl declaration : unit.getClasses())
            declaration.accept(this);

        substitutions.pop();
        scopes.pop();
    }

    private SymbolScope currentScope() {
        return scopes.peek();
    }

    private Map<String, Symbol> currentSubstitutions() {
        return substitutions.peek();
    }

    private void pushScope(SymbolScope scope) {
        scopes.push(scope);
    }

    private void popScope() {
        scopes.pop();
    }

    private void pushSubstitutions(Map<String, ? extends Symbol> values) {
        Map<String, Symbol> merged = new LinkedHashMap<>(currentSubstitutions());
        if (values != null)
            merged.putAll(values);
        substitutions.push(merged);
    }

    private void popSubstitutions() {
        substitutions.pop();
    }

    private void report(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.error(
                node != null ? node.getPosition() : null, message));
    }

    private void warn(Positioned node, String message) {
        unit.addDiagnostic(TAG, Diagnostic.warning(
                node != null ? node.getPosition() : null, message));
    }

    private Symbol resolveType(Typed type, Map<String, Symbol> localSubstitutions) {
        Map<String, Symbol> active = new LinkedHashMap<>();
        if (!substitutions.isEmpty())
            active.putAll(currentSubstitutions());
        if (localSubstitutions != null)
            active.putAll(localSubstitutions);
        return table.resolve(type, active, unresolved ->
                report(unresolved, UNRESOLVED_TYPE.format(unresolved.getName())));
    }

    private Symbol substitute(Symbol type, Map<String, Symbol> replacements) {
        if (type == null || replacements == null || replacements.isEmpty())
            return type;
        if (type.isTypeParam())
            return replacements.getOrDefault(type.getName(), type);
        if (type.isParameterizedClass()) {
            ParameterizedClassSymbol parameterized = type.asParameterizedClass();
            Map<String, Symbol> arguments = new LinkedHashMap<>();
            parameterized.getTypeArguments().forEach((name, value) ->
                    arguments.put(name, substitute(value, replacements)));
            return new ParameterizedClassSymbol(parameterized.getBase(), arguments);
        }
        return type;
    }

    private Map<String, Symbol> substitutionsOf(Symbol type) {
        if (type != null && type.isParameterizedClass())
            return new LinkedHashMap<>(type.asParameterizedClass().getTypeArguments());
        return Collections.emptyMap();
    }

    private Symbol effectiveType(Symbol symbol) {
        return table.unwrap(symbol);
    }

    private boolean isAssignmentCompatible(Symbol targetType, Expr value, Symbol valueType) {
        return table.isAssignable(valueType, targetType)
                || isImplicitConstructorCompatible(targetType, value, valueType);
    }

    private boolean isImplicitConstructorCompatible(Symbol targetType, Expr value, Symbol valueType) {
        if (!(value instanceof LiteralExpr) || targetType == null || valueType == null
                || !targetType.isClass())
            return false;
        Map<String, ParamSymbol> parameters = targetType.asClass().getParameters();
        if (parameters.size() != 1)
            return false;
        Symbol parameterType = parameters.values().iterator().next().getType();
        return parameterType != null && table.isAssignable(valueType, parameterType);
    }

    private MemberSymbol currentMember() {
        if (currentVariable != null)
            return currentVariable;
        if (currentCallable != null)
            return currentCallable;
        return currentClass;
    }

    private boolean isStaticContext() {
        MemberSymbol member = currentMember();
        if (member == null || member == currentClass)
            return true;
        return member.hasModifier(Modifier.STATIC);
    }

    private void checkModifiers(Decl decl, List<Modifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty())
            return;

        int seen = 0;
        for (Modifier m : modifiers) {
            if ((seen & m.type) != 0) {
                report(m, DUPLICATE_MODIFIER.format(m.toString()));
                continue;
            }
            if ((m.type & Modifier.VISIBILITY) != 0
                    && (seen & Modifier.VISIBILITY) != 0) {
                report(m, CONFLICTING_MODIFIERS.format("public/private"));
                continue;
            }
            seen |= m.type;
        }

        if (decl instanceof ClassDecl) {
            if (Modifier.isPrivate(seen))
                report(decl, INVALID_MODIFIER_IN_CONTEXT.format("private"));
            if (Modifier.isStatic(seen))
                report(decl, INVALID_MODIFIER_IN_CONTEXT.format("static"));
        } else if (decl instanceof OperatorDecl) {
            if (Modifier.isFinal(seen))
                report(decl, INVALID_MODIFIER_IN_CONTEXT.format("final"));
        }
    }

    private boolean isAccessible(Symbol target) {
        if (target == null)
            return true;

        if (target instanceof MemberSymbol member) {
            if (!member.hasModifier(Modifier.PRIVATE))
                return true;
            return currentClass != null && member.getDeclaringClass() == currentClass;
        }
        if (target instanceof ClassSymbol clazz) {
            if (!clazz.hasModifier(Modifier.PRIVATE))
                return true;
            return currentClass != null && clazz == currentClass;
        }
        return true;
    }

    private boolean isThisReceiver(Expr object) {
        return object instanceof RefExpr ref
                && ref.name != null && "this".equals(ref.name.source);
    }

    private MemberSymbol currentMemberOwner() {
        return currentMember();
    }

    private boolean isPlaceholder(Expr expression) {
        return expression instanceof RefExpr ref
                && ref.name != null && "_".equals(ref.name.source);
    }

    private void analyzeAnnotationParameterDefaults(ClassSymbol baseClass,
            Map<String, Symbol> typeArguments) {
        if (!analyzedAnnotationDefaults.add(baseClass))
            return;
        for (ParamSymbol parameter : baseClass.getParameters().values()) {
            if (!parameter.hasInit())
                continue;
            Symbol expected = substitute(parameter.getType(), typeArguments);
            SymbolResult result = parameter.getInit().accept(this);
            Symbol actual = result != null ? result.getType() : null;
            if (expected != null && actual != null && !table.isAssignable(actual, expected))
                report(parameter.getInit(), INVALID_ANNOTATION_ARGUMENT_TYPE.format(
                        baseClass.getName(), parameter.getName(),
                        getTypeName(expected), getTypeName(actual)));
        }
    }

    private void checkAnnotationApplicability(AnnoDecl declaration,
            ClassSymbol annotationClass, MemberSymbol owner) {
    }

    @Override
    public Void visitAnnoDecl(AnnoDecl declaration) {
        if (declaration == null || declaration.type == null)
            return null;

        AnnoSymbol annotation = declaration.symbol != null
                ? declaration.symbol.asAnno() : null;

        if (annotation == null) {
            if (declaration.arguments != null)
                declaration.arguments.forEach(arg -> arg.accept(this));
            return null;
        }

        Symbol targetType = annotation.getType();
        if (targetType == null) {
            if (declaration.arguments != null)
                declaration.arguments.forEach(arg -> arg.accept(this));
            return null;
        }

        ClassSymbol baseClass = table.getBaseClass(targetType);
        Map<String, Symbol> typeArguments;
        if (targetType.isParameterizedClass()) {
            typeArguments = new LinkedHashMap<>(
                    targetType.asParameterizedClass().getTypeArguments());
        } else {
            typeArguments = Collections.emptyMap();
        }

        if (!table.isSubtype(targetType, table.getAnnotation())) {
            String superName = "<none>";
            if (baseClass != null && !baseClass.getBaseSupers().isEmpty())
                superName = getTypeName(baseClass.getBaseSupers().get(0));
            String baseName = baseClass != null
                    ? baseClass.getName() : getTypeName(targetType);
            report(declaration, INVALID_ANNOTATION_BASE.format(baseName, superName));
            if (declaration.arguments != null)
                declaration.arguments.forEach(arg -> arg.accept(this));
            return null;
        }

        if (baseClass == null) {
            report(declaration, INVALID_ANNOTATION_TYPE.format(annotation.getName()));
            if (declaration.arguments != null)
                declaration.arguments.forEach(arg -> arg.accept(this));
            return null;
        }

        analyzeAnnotationParameterDefaults(baseClass, typeArguments);

        MemberSymbol owner = currentMemberOwner();
        if (owner != null)
            checkAnnotationApplicability(declaration, baseClass, owner);

        List<ParamSymbol> parameters = new ArrayList<>(baseClass.getParameters().values());
        List<Expr> arguments = declaration.arguments != null
                ? declaration.arguments : List.of();

        if (arguments.size() != parameters.size()) {
            report(declaration, INVALID_ANNOTATION_ARGUMENT_COUNT.format(
                    baseClass.getName(), parameters.size(), arguments.size()));
            for (Expr argument : arguments)
                argument.accept(this);
            return null;
        }

        for (int i = 0; i < arguments.size(); i++) {
            Expr argument = arguments.get(i);
            ParamSymbol parameter = parameters.get(i);
            Symbol parameterType = substitute(parameter.getType(), typeArguments);

            if (isPlaceholder(argument)) {
                if (!parameter.hasInit())
                    report(argument, ANNOTATION_REQUIRES_ARGUMENT.format(
                            baseClass.getName(), parameter.getName()));
                continue;
            }

            SymbolResult result = argument.accept(this);
            Symbol argumentType = result != null ? result.getType() : null;
            if (argumentType != null && parameterType != null
                    && !table.isAssignable(argumentType, parameterType))
                report(argument, INVALID_ANNOTATION_ARGUMENT_TYPE.format(
                        baseClass.getName(), i + 1,
                        getTypeName(parameterType), getTypeName(argumentType)));
        }
        return null;
    }

    @Override
    public Void visitTypeParamDecl(TypeParamDecl declaration) {
        TypeParamSymbol parameter = declaration.symbol != null
                ? declaration.symbol.asTypeParam() : null;
        if (parameter == null)
            return null;

        if (!analyzedTypeParams.add(parameter))
            return null;

        for (Symbol bound : parameter.getBounds().values()) {
            if (bound == null)
                continue;
            if (bound.isClass() || bound.isParameterizedClass())
                continue;
            report(declaration, INVALID_TYPE_PARAMETER_BOUND.format(
                    getTypeName(bound), parameter.getName()));
        }
        return null;
    }

    @Override
    public Void visitClassDecl(ClassDecl declaration) {
        ClassSymbol symbol = table.getClass(declaration.name.source);
        if (symbol == null || symbol.node != declaration)
            return null;

        checkModifiers(declaration, declaration.modifiers);

        declaration.symbol = symbol;
        currentClass = symbol;

        SymbolScope classScope = currentScope().child(symbol.getName());
        classScope.putLocal("this", symbol);
        for (TypeParamSymbol parameter : symbol.getTypeParameters().values())
            classScope.putLocal(parameter.getName(), parameter);
        for (ParamSymbol parameter : symbol.getParameters().values())
            classScope.putLocal(parameter.getName(), parameter);

        Map<String, Symbol> classSubstitutions = new LinkedHashMap<>();
        classSubstitutions.putAll(symbol.getTypeParameters());
        classSubstitutions.put("Self", symbol);
        pushScope(classScope);
        pushSubstitutions(classSubstitutions);

        if (declaration.annotations != null)
            declaration.annotations.forEach(annotation -> annotation.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(parameter -> parameter.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(parameter -> parameter.accept(this));
        if (declaration.supers != null)
            declaration.supers.forEach(superDecl -> superDecl.accept(this));
        if (declaration.members != null)
            declaration.members.forEach(member -> member.accept(this));

        popSubstitutions();
        popScope();
        currentClass = null;
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl declaration) {
        FunSymbol function = currentClass != null
                ? currentClass.getDeclaredFun(declaration.name.source) : null;
        if (function == null || function.node != declaration)
            return null;
        analyzeCallable(declaration, function);
        return null;
    }

    @Override
    public Void visitOperatorDecl(OperatorDecl declaration) {
        OperatorSymbol operator = currentClass != null
                ? currentClass.getDeclaredOperators().values().stream()
                        .filter(candidate -> candidate.node == declaration)
                        .findFirst().orElse(null)
                : null;
        if (operator == null || operator.node != declaration)
            return null;
        analyzeCallable(declaration, operator);
        return null;
    }

    private void analyzeCallable(CallableDecl declaration, CallableSymbol symbol) {
        checkModifiers(declaration, declaration.modifiers);

        BlockStmt body = null;
        if (declaration instanceof FunDecl fun) {
            if (fun.body == null)
                warn(declaration, FUNCTION_WITHOUT_BODY.format(symbol.getName()));
            body = fun.body;
        } else if (declaration instanceof OperatorDecl op) {
            if (op.body == null)
                warn(declaration, OPERATOR_WITHOUT_BODY.format(op.operator.lexeme));
            body = op.body;
        }

        declaration.symbol = symbol;
        currentCallable = symbol;

        SymbolScope callableScope = currentScope().child(symbol.getName());
        for (TypeParamSymbol parameter : symbol.getTypeParameters().values())
            callableScope.putLocal(parameter.getName(), parameter);
        for (ParamSymbol parameter : symbol.getParameters().values())
            callableScope.putLocal(parameter.getName(), parameter);
        pushScope(callableScope);
        pushSubstitutions(symbol.getTypeParameters());

        if (declaration.annotations != null)
            declaration.annotations.forEach(annotation -> annotation.accept(this));
        if (declaration.typeParameters != null)
            declaration.typeParameters.forEach(parameter -> parameter.accept(this));
        if (declaration.parameters != null)
            declaration.parameters.forEach(parameter -> parameter.accept(this));

        if (declaration instanceof OperatorDecl operatorDecl)
            checkOperatorReceiver(operatorDecl);

        if (body != null)
            body.accept(this);

        popSubstitutions();
        popScope();
        currentCallable = null;
    }

    private void checkOperatorReceiver(OperatorDecl declaration) {
        if (currentClass == null || declaration.parameters == null
                || declaration.parameters.isEmpty())
            return;
        ParamDecl first = declaration.parameters.get(0);
        if (first.symbol instanceof ParamSymbol parameter) {
            Symbol operandType = parameter.getType();
            if (operandType != null) {
                ClassSymbol operandBase = table.getBaseClass(operandType);
                if (operandBase != currentClass)
                    report(first, INVALID_OPERATOR_RECEIVER_TYPE.format(
                            declaration.operator.lexeme,
                            currentClass.getName(),
                            getTypeName(operandType)));
            }
        }
    }

    @Override
    public Void visitVarDecl(VarDecl declaration) {
        VarSymbol variable = currentClass != null
                ? currentClass.getDeclaredVar(declaration.name.source) : null;
        if (variable == null || variable.node != declaration)
            return null;

        checkModifiers(declaration, declaration.modifiers);

        if (variable.hasModifier(Modifier.FINAL) && declaration.value == null)
            report(declaration, FINAL_WITHOUT_INIT.format(variable.getName()));

        declaration.symbol = variable;
        currentVariable = variable;

        if (declaration.annotations != null)
            declaration.annotations.forEach(annotation -> annotation.accept(this));

        if (declaration.value != null) {
            SymbolResult value = declaration.value.accept(this);
            Symbol valueType = value != null ? value.getType() : null;
            if (valueType != null && variable.getType() != null
                    && !isAssignmentCompatible(variable.getType(), declaration.value, valueType))
                report(declaration.value, INVALID_ASSIGNMENT.format(
                        getTypeName(valueType), getTypeName(variable.getType())));
        }
        currentVariable = null;
        return null;
    }

    @Override
    public Void visitParamDecl(ParamDecl declaration) {
        Symbol symbol = declaration.symbol;
        if (symbol == null) {
            if (currentCallable != null)
                symbol = currentCallable.getParameter(declaration.name.source);
            else if (currentClass != null)
                symbol = currentClass.getParameter(declaration.name.source);
        }
        if (symbol == null)
            return null;
        declaration.symbol = symbol;

        ParamSymbol parameter = symbol.asParam();
        if (parameter != null && parameter.hasInit()) {
            SymbolResult result = parameter.getInit().accept(this);
            Symbol valueType = result != null ? result.getType() : null;
            if (valueType != null && parameter.getType() != null
                    && !isAssignmentCompatible(parameter.getType(), parameter.getInit(), valueType))
                report(parameter.getInit(), INVALID_ASSIGNMENT.format(
                        getTypeName(valueType), getTypeName(parameter.getType())));
        }
        return null;
    }

    @Override
    public Void visitSuperDecl(SuperDecl declaration) {
        if (currentClass != null) {
            declaration.symbol = currentClass.getSuper(declaration.type.getName());
            Symbol superType = declaration.symbol instanceof SuperSymbol s ? s.getType() : null;
            if (superType instanceof ClassSymbol c && c.hasModifier(Modifier.FINAL))
                report(declaration, FINAL_SUPER.format(c.getName()));
            if (declaration.arguments != null)
                declaration.arguments.forEach(argument -> argument.accept(this));
        }
        return null;
    }

    @Override
    public Void visitLetStmt(LetStmt statement) {
        if (statement.name == null)
            return null;
        if (currentScope().hasLocal(statement.name.source)) {
            report(statement, REDEFINED_VARIABLE.format(statement.name.source));
            return null;
        }

        Symbol declaredType = statement.type != null
                ? resolveType(statement.type, Map.of()) : null;
        SymbolResult value = statement.value != null ? statement.value.accept(this) : null;
        Symbol valueType = value != null ? value.getType() : null;
        if (declaredType != null && valueType != null
                && !isAssignmentCompatible(declaredType, statement.value, valueType))
            report(statement.value, INVALID_ASSIGNMENT.format(
                    getTypeName(valueType), getTypeName(declaredType)));

        Symbol type = declaredType != null ? declaredType : valueType;
        if (type == null) {
            report(statement, UNIREFERED_LOCAL.format(statement.name.source));
            return null;
        }

        VarSymbol variable = new VarSymbol(statement.name.source);
        variable.node = statement;
        variable.setType(type);
        statement.symbol = variable;
        currentScope().putLocal(variable.getName(), variable);
        return null;
    }

    @Override
    public Void visitBlockStmt(BlockStmt statement) {
        pushScope(currentScope().child("block"));
        if (statement.statements != null)
            statement.statements.forEach(child -> child.accept(this));
        popScope();
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmt statement) {
        checkCondition(statement.condition);
        if (statement.thenBranch != null)
            statement.thenBranch.accept(this);
        if (statement.elseBranch != null)
            statement.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt statement) {
        checkCondition(statement.condition);
        loopDepth++;
        if (statement.body != null)
            statement.body.accept(this);
        loopDepth--;
        return null;
    }

    private void checkCondition(Expr condition) {
        if (condition == null)
            return;
        SymbolResult result = condition.accept(this);
        Symbol type = result != null ? result.getType() : null;
        if (type != null && !table.isBool(type))
            report(condition, EXPECTED_TYPE.format(getTypeName(table.getBool()), getTypeName(type)));
    }

    @Override
    public Void visitReturnStmt(ReturnStmt statement) {
        if (currentCallable == null) {
            report(statement, INVALID_FUNCTION_RETURN_OUTSIDE.format());
            return null;
        }
        Symbol returnType = currentCallable.getType();
        if (statement.value == null) {
            if (returnType != null && !table.isVoid(returnType))
                report(statement, INVALID_FUNCTION_MISSING_RETURN.format(
                        currentCallable.getName(), getTypeName(returnType)));
            return null;
        }

        SymbolResult result = statement.value.accept(this);
        Symbol valueType = result != null ? result.getType() : null;
        if (valueType != null && returnType != null
                && !table.isAssignable(valueType, returnType))
            report(statement.value, INVALID_FUNCTION_RETURN_TYPE.format(
                    currentCallable.getName(), getTypeName(returnType), getTypeName(valueType)));
        return null;
    }

    @Override
    public Void visitBreakStmt(BreakStmt statement) {
        if (loopDepth == 0)
            report(statement, BREAK_OUTSIDE_LOOP.format());
        return null;
    }

    @Override
    public Void visitContinueStmt(ContinueStmt statement) {
        if (loopDepth == 0)
            report(statement, CONTINUE_OUTSIDE_LOOP.format());
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt statement) {
        if (statement.expression != null)
            statement.expression.accept(this);
        return null;
    }

    @Override
    public SymbolResult visitLiteralExpr(LiteralExpr expression) {
        ClassSymbol type = switch (expression.type) {
            case LiteralExpr.NULL -> table.getNull();
            case LiteralExpr.BOOL -> table.getBool();
            case LiteralExpr.INT -> table.getInt();
            case LiteralExpr.FLOAT -> table.getFloat();
            case LiteralExpr.STRING -> table.getString();
            default -> null;
        };
        if (type == null)
            report(expression, UNRESOLVED_TYPE.format(expression.getTypeName()));
        expression.symbol = type;
        return type != null ? SymbolResult.of(type, type) : null;
    }

    @Override
    public SymbolResult visitRefExpr(RefExpr expression) {
        if (expression.name == null)
            return null;
        String name = expression.name.source;
        Symbol symbol = currentScope().resolve(name);

        if (symbol == null && currentClass != null) {
            symbol = currentClass.getTypeParameter(name);
            if (symbol == null)
                symbol = currentClass.getParameter(name);
            if (symbol == null)
                symbol = currentClass.getMember(name);
        }
        if (symbol == null)
            symbol = table.getClass(name);
        if (symbol == null) {
            report(expression, UNDEFINED_VARIABLE.format(name));
            return null;
        }
        if (!isAccessible(symbol)) {
            report(expression, INACCESSIBLE_MEMBER.format(name));
            return null;
        }

        if (symbol instanceof MemberSymbol member
                && currentClass != null
                && member.getDeclaringClass() == currentClass) {
            boolean memberIsStatic = member.hasModifier(Modifier.STATIC);
            if (!memberIsStatic && isStaticContext())
                report(expression, INSTANCE_ACCESS_ON_STATIC.format(member.getName()));
        }

        expression.symbol = symbol;
        Symbol type = effectiveType(symbol);
        Map<String, Symbol> receiverSubstitutions = substitutionsOf(type);
        return SymbolResult.of(symbol, type, receiverSubstitutions);
    }

    @Override
    public SymbolResult visitMemberExpr(MemberExpr expression) {
        SymbolResult objectResult = expression.object != null
                ? expression.object.accept(this) : null;
        Symbol objectType = objectResult != null ? objectResult.getType() : null;
        if (objectType == null || expression.name == null)
            return null;

        ClassSymbol owner = table.getBaseClass(objectType);
        MemberSymbol member = owner != null ? owner.getMember(expression.name.source) : null;
        if (member == null && objectType.isTypeParam()) {
            TypeParamSymbol typeParameter = objectType.asTypeParam();
            member = typeParameter.getFun(expression.name.source);
            if (member == null)
                member = typeParameter.getVar(expression.name.source);
        }
        if (member == null) {
            report(expression, UNRESOLVED_MEMBER.format(
                    expression.name.source, getTypeName(objectType)));
            return null;
        }
        if (!isAccessible(member)) {
            report(expression, INACCESSIBLE_MEMBER.format(member.getName()));
            return null;
        }

        boolean memberIsStatic = member.hasModifier(Modifier.STATIC);
        boolean receiverIsClass = objectResult.getRaw() instanceof ClassSymbol
                && !isThisReceiver(expression.object);
        boolean receiverIsThis = isThisReceiver(expression.object);

        if (memberIsStatic && !receiverIsClass && !receiverIsThis)
            report(expression, STATIC_ACCESS_ON_INSTANCE.format(member.getName()));
        if (!memberIsStatic && receiverIsClass)
            report(expression, INSTANCE_ACCESS_ON_CLASS.format(member.getName()));
        if (receiverIsThis && !memberIsStatic && isStaticContext())
            report(expression, INSTANCE_ACCESS_ON_STATIC.format(member.getName()));

        expression.symbol = member;
        Map<String, Symbol> memberSubstitutions = substitutionsOf(objectType);
        Symbol type = substitute(table.unwrap(member), memberSubstitutions);
        return SymbolResult.of(member, type, memberSubstitutions);
    }

    @Override
    public SymbolResult visitCallExpr(CallExpr expression) {
        SymbolResult calleeResult = expression.callee != null
                ? expression.callee.accept(this) : null;
        if (calleeResult == null || calleeResult.getRaw() == null)
            return null;

        Symbol callee = calleeResult.getRaw();
        if (callee.isClass())
            return instantiateClass(expression, callee.asClass());
        if (callee.isParameterizedClass())
            return SymbolResult.of(callee, callee);
        if (callee.isFun()) {
            FunSymbol function = callee.asFun();
            if (!isAccessible(function)) {
                report(expression, INACCESSIBLE_MEMBER.format(function.getName()));
                return null;
            }
            return callFunction(expression, function, calleeResult.getSubstituitions());
        }
        report(expression, INVALID_FUNCTION_CALL.format(getTypeName(callee)));
        return null;
    }

    private SymbolResult instantiateClass(CallExpr expression, ClassSymbol type) {
        expression.symbol = type;
        Map<String, Symbol> typeArguments = resolveTypeArguments(expression.typeArguments,
                type.getTypeParameters(), type.getName(), true);
        validateTypeArgumentBounds(type.getTypeParameters(), typeArguments,
                expression.typeArguments, expression, type.getName());
        if ((expression.typeArguments == null || expression.typeArguments.isEmpty())
                && type.hasTypeParameters()) {
            for (String name : type.getTypeParameters().keySet()) {
                Symbol bound = resolveUpperBound(type.getTypeParameter(name));
                if (bound != null)
                    typeArguments.put(name, bound);
            }
            warn(expression, INVALID_CLASS_UNCHECKED_CONSTRUCTOR.format(
                    type.getName(), formatTypeArguments(type.getTypeParameters().keySet(), typeArguments)));
        }
        List<ParamSymbol> parameters = new ArrayList<>(type.getParameters().values());
        List<Expr> arguments = expression.arguments != null ? expression.arguments : List.of();
        if (arguments.size() != parameters.size()) {
            report(expression, INVALID_CLASS_CONSTRUCTOR_ARGUMENT_COUNT.format(
                    type.getName(), parameters.size(), arguments.size()));
            return SymbolResult.of(type, instantiatedType(type, typeArguments), typeArguments);
        }

        for (int i = 0; i < arguments.size(); i++) {
            Expr argument = arguments.get(i);
            ParamSymbol parameter = parameters.get(i);
            SymbolResult result = argument.accept(this);
            Symbol argumentType = result != null ? result.getType() : null;
            Symbol parameterType = substitute(parameter.getType(), typeArguments);
            if (argumentType != null && parameterType != null
                    && !table.isAssignable(argumentType, parameterType))
                report(argument, INVALID_CLASS_CONSTRUCTOR_ARGUMENT_TYPE.format(
                        type.getName(), i + 1, getTypeName(parameterType), getTypeName(argumentType)));
        }
        Symbol resultType = instantiatedType(type, typeArguments);
        return SymbolResult.of(type, resultType, typeArguments);
    }

    private SymbolResult callFunction(CallExpr expression, FunSymbol function,
            Map<String, Symbol> receiverSubstitutions) {
        expression.symbol = function;
        Map<String, Symbol> typeArguments = new LinkedHashMap<>();
        if (receiverSubstitutions != null)
            typeArguments.putAll(receiverSubstitutions);
        boolean hasExplicitTypeArguments = expression.typeArguments != null
                && !expression.typeArguments.isEmpty();
        for (String name : function.getTypeParameters().keySet())
            typeArguments.remove(name);
        Map<String, Symbol> explicitTypeArguments = resolveTypeArguments(expression.typeArguments,
                function.getTypeParameters(), function.getName(), false);
        typeArguments.putAll(explicitTypeArguments);

        List<ParamSymbol> parameters = new ArrayList<>(function.getParameters().values());
        List<Expr> arguments = expression.arguments != null ? expression.arguments : List.of();
        if (arguments.size() != parameters.size()) {
            report(expression, INVALID_FUNCTION_ARGUMENT_COUNT.format(
                    function.getName(), parameters.size(), arguments.size()));
            return null;
        }

        List<SymbolResult> argumentResults = new ArrayList<>(arguments.size());
        for (Expr argument : arguments)
            argumentResults.add(argument.accept(this));

        if (!hasExplicitTypeArguments && !function.getTypeParameters().isEmpty()) {
            Map<String, Symbol> inferred = new LinkedHashMap<>();
            for (int i = 0; i < parameters.size(); i++) {
                SymbolResult argumentResult = argumentResults.get(i);
                Symbol actualType = argumentResult != null ? argumentResult.getType() : null;
                inferTypeArguments(parameters.get(i).getType(), actualType,
                        function.getTypeParameters().keySet(), inferred);
            }
            typeArguments.putAll(inferred);
            for (String name : function.getTypeParameters().keySet()) {
                if (!typeArguments.containsKey(name))
                    report(expression, UNINFERRED_TYPE_ARGUMENT.format(name, function.getName()));
            }
        }

        validateTypeArgumentBounds(function.getTypeParameters(), typeArguments,
                expression.typeArguments, expression, function.getName());

        for (int i = 0; i < arguments.size(); i++) {
            Expr argument = arguments.get(i);
            SymbolResult result = argumentResults.get(i);
            Symbol argumentType = result != null ? result.getType() : null;
            Symbol parameterType = substitute(parameters.get(i).getType(), typeArguments);
            if (argumentType != null && parameterType != null
                    && !table.isAssignable(argumentType, parameterType))
                report(argument, INVALID_FUNCTION_ARGUMENT_TYPE.format(
                        function.getName(), i + 1, getTypeName(parameterType), getTypeName(argumentType)));
        }
        return SymbolResult.of(function, substitute(function.getType(), typeArguments), typeArguments);
    }

    private Map<String, Symbol> resolveTypeArguments(List<Typed> arguments,
            Map<String, TypeParamSymbol> parameters, String callableName, boolean classConstructor) {
        Map<String, Symbol> resolved = new LinkedHashMap<>();
        if (arguments == null || arguments.isEmpty())
            return resolved;
        if (arguments.size() != parameters.size()) {
            String message = classConstructor
                    ? INVALID_CLASS_CONSTRUCTOR_TYPE_ARGUMENT_COUNT.format(
                            callableName, parameters.size(), arguments.size())
                    : INVALID_FUNCTION_TYPE_ARGUMENT_COUNT.format(
                            callableName, parameters.size(), arguments.size());
            report(arguments.get(0), message);
            return resolved;
        }
        int index = 0;
        for (String name : parameters.keySet()) {
            Symbol type = resolveType(arguments.get(index++), Map.of());
            if (type != null)
                resolved.put(name, type);
        }
        return resolved;
    }

    private void validateTypeArgumentBounds(Map<String, TypeParamSymbol> parameters,
            Map<String, Symbol> arguments, List<Typed> explicitArguments,
            Positioned call, String callableName) {
        int index = 0;
        for (Map.Entry<String, TypeParamSymbol> entry : parameters.entrySet()) {
            Symbol argument = arguments.get(entry.getKey());
            if (argument == null) {
                index++;
                continue;
            }
            Positioned argumentPosition = explicitArguments != null
                    && index < explicitArguments.size() ? explicitArguments.get(index) : call;
            for (Symbol bound : entry.getValue().getBounds().values()) {
                if (bound != null && !table.isSubtype(argument, bound))
                    report(argumentPosition, INVALID_TYPE_ARGUMENT_BOUND.format(
                            getTypeName(argument), getTypeName(bound), entry.getKey()));
            }
            index++;
        }
    }

    private Symbol instantiatedType(ClassSymbol type, Map<String, Symbol> arguments) {
        return arguments == null || arguments.isEmpty()
                ? type : new ParameterizedClassSymbol(type, arguments);
    }

    @Override
    public SymbolResult visitParenthesisExpr(ParenthesisExpr expression) {
        return expression.inner != null ? expression.inner.accept(this) : null;
    }

    @Override
    public SymbolResult visitOperatorExpr(OperatorExpr expression) {
        List<Expr> operands = expression.operands != null ? expression.operands : List.of();
        List<Symbol> operandTypes = new ArrayList<>(operands.size());
        for (Expr operand : operands) {
            SymbolResult result = operand != null ? operand.accept(this) : null;
            operandTypes.add(result != null ? result.getType() : null);
        }
        Symbol receiverType = !operandTypes.isEmpty() ? operandTypes.get(0) : null;

        OperatorResolution resolution = findOperator(expression, operandTypes, receiverType);
        if (resolution == null) {
            report(expression, INVALID_OPERATOR.format(
                    expression.operator.lexeme, getTypeName(receiverType)));
            return null;
        }
        OperatorSymbol operator = resolution.symbol();
        if (!isAccessible(operator)) {
            report(expression, INACCESSIBLE_MEMBER.format(operator.getLexeme()));
            return null;
        }
        expression.symbol = operator;
        validateTypeArgumentBounds(operator.getTypeParameters(), resolution.substitutions(),
                expression.typeArguments, expression, operator.getLexeme());

        Map<String, Symbol> operatorSubstitutions = resolution.substitutions();
        List<ParamSymbol> parameters = new ArrayList<>(operator.getParameters().values());
        for (int i = 0; i < operands.size(); i++) {
            ParamSymbol parameter = i < parameters.size() ? parameters.get(i) : null;
            checkOperatorOperand(expression.operator.lexeme, operands.get(i), parameter,
                    operandTypes.get(i), operatorSubstitutions);
        }
        Symbol resultType = substitute(operator.getType(), operatorSubstitutions);
        return SymbolResult.of(operator, resultType, operatorSubstitutions);
    }

    private void checkOperatorOperand(String operatorName, Expr expression, ParamSymbol parameter,
            Symbol actualType, Map<String, Symbol> operatorSubstitutions) {
        if (expression == null || parameter == null)
            return;
        Symbol expectedType = substitute(parameter.getType(), operatorSubstitutions);
        if (actualType != null && expectedType != null && !table.isAssignable(actualType, expectedType))
            report(expression, INVALID_OPERATOR_ARGUMENT_TYPE.format(operatorName,
                    getTypeName(expectedType), getTypeName(actualType)));
    }

    private Symbol resolveUpperBound(TypeParamSymbol parameter) {
        if (parameter != null) {
            for (Symbol bound : parameter.getBounds().values()) {
                if (bound != null)
                    return bound;
            }
        }
        return table.getAny();
    }

    private String formatTypeArguments(Iterable<String> names, Map<String, Symbol> arguments) {
        List<String> rendered = new ArrayList<>();
        for (String name : names)
            rendered.add(getTypeName(arguments.get(name)));
        return "<" + String.join(", ", rendered) + ">";
    }

    private OperatorResolution findOperator(OperatorExpr expression, List<Symbol> operandTypes,
            Symbol receiverType) {
        List<OperatorSymbol> candidates = new ArrayList<>();
        ClassSymbol base = table.getBaseClass(receiverType);
        if (base != null)
            collectOperators(base, candidates, new HashSet<>());
        if (receiverType != null && receiverType.isTypeParam()) {
            for (Symbol bound : receiverType.asTypeParam().getBounds().values()) {
                ClassSymbol boundClass = table.getBaseClass(bound);
                if (boundClass != null)
                    collectOperators(boundClass, candidates, new HashSet<>());
            }
        }

        for (OperatorSymbol candidate : candidates) {
            if (candidate.getKind() != expression.kind
                    || !candidate.getLexeme().equals(expression.operator.lexeme))
                continue;
            Map<String, Symbol> candidateSubstitutions =
                    operatorSubstitutions(candidate, expression, operandTypes, receiverType);
            List<ParamSymbol> parameters = new ArrayList<>(candidate.getParameters().values());
            if (candidateSubstitutions == null || parameters.size() != operandTypes.size())
                continue;
            boolean matches = true;
            for (int i = 0; i < operandTypes.size(); i++) {
                Symbol actualType = operandTypes.get(i);
                Symbol expectedType = substitute(parameters.get(i).getType(), candidateSubstitutions);
                if ((actualType == null && expectedType != null)
                        || (actualType != null && expectedType != null
                                && !table.isAssignable(actualType, expectedType))) {
                    matches = false;
                    break;
                }
            }
            if (!matches)
                continue;
            return new OperatorResolution(candidate, candidateSubstitutions);
        }
        return null;
    }

    private Map<String, Symbol> operatorSubstitutions(OperatorSymbol operator, OperatorExpr expression,
            List<Symbol> operandTypes, Symbol receiverType) {
        Map<String, Symbol> resolved = new LinkedHashMap<>(substitutionsOf(receiverType));
        Map<String, TypeParamSymbol> typeParameters = operator.getTypeParameters();
        List<Typed> typeArguments = expression.typeArguments != null
                ? expression.typeArguments : List.of();

        if (!typeArguments.isEmpty()) {
            if (typeArguments.size() != typeParameters.size())
                return null;
            int index = 0;
            for (String name : typeParameters.keySet()) {
                Symbol type = table.resolve(typeArguments.get(index++), currentSubstitutions(), null);
                if (type == null)
                    return null;
                resolved.put(name, type);
            }
            return resolved;
        }

        if (typeParameters.isEmpty())
            return resolved;

        List<ParamSymbol> parameters = new ArrayList<>(operator.getParameters().values());
        Map<String, Symbol> inferred = new LinkedHashMap<>();
        Set<String> inferableTypeParameters = new LinkedHashSet<>();
        for (Map.Entry<String, TypeParamSymbol> entry : typeParameters.entrySet()) {
            TypeParamSymbol parameter = entry.getValue();
            if (parameter.hasBounds())
                resolved.put(entry.getKey(), parameter);
            else
                inferableTypeParameters.add(entry.getKey());
        }
        if (parameters.size() != operandTypes.size())
            return null;
        for (int i = 0; i < parameters.size(); i++) {
            inferTypeArguments(parameters.get(i).getType(), operandTypes.get(i),
                    inferableTypeParameters, inferred);
        }

        for (String name : typeParameters.keySet()) {
            if (resolved.containsKey(name))
                continue;
            Symbol argument = inferred.get(name);
            if (argument == null)
                argument = resolveUpperBound(typeParameters.get(name));
            if (argument == null)
                return null;
            resolved.put(name, argument);
        }
        return resolved;
    }

    private void inferTypeArguments(Symbol expected, Symbol actual, Set<String> typeParameters,
            Map<String, Symbol> inferred) {
        if (expected == null || actual == null)
            return;
        if (expected.isTypeParam() && typeParameters.contains(expected.getName())) {
            inferred.putIfAbsent(expected.getName(), actual);
            return;
        }
        if (expected.isParameterizedClass() && actual.isParameterizedClass()) {
            ParameterizedClassSymbol expectedClass = expected.asParameterizedClass();
            ParameterizedClassSymbol actualClass = actual.asParameterizedClass();
            if (expectedClass.getBase() != actualClass.getBase())
                return;
            for (String name : expectedClass.getTypeArguments().keySet()) {
                inferTypeArguments(expectedClass.getTypeArguments().get(name),
                        actualClass.getTypeArguments().get(name), typeParameters, inferred);
            }
        }
    }

    private void collectOperators(ClassSymbol type, List<OperatorSymbol> result, Set<ClassSymbol> seen) {
        if (type == null || !seen.add(type))
            return;
        result.addAll(type.getDeclaredOperators().values());
        for (ClassSymbol parent : type.getBaseSupers())
            collectOperators(parent, result, seen);
    }

    private String getTypeName(Symbol symbol) {
        if (symbol == null)
            return "unknown";
        if (symbol.isParameterizedClass()) {
            ParameterizedClassSymbol parameterized = symbol.asParameterizedClass();
            String arguments = String.join(", ", parameterized.getTypeArguments().values().stream()
                    .map(this::getTypeName).toList());
            return parameterized.getBase().getName() + "<" + arguments + ">";
        }
        if (symbol.isClass())
            return symbol.asClass().getName();
        return symbol.getName();
    }
}