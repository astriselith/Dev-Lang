package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassSymbol extends Symbol {

    private final Map<String, TypeParamSymbol> typeParameters;
    private final Map<String, ParamSymbol> parameters;
    private final Map<String, SuperSymbol> supers;

    private final Map<String, FunSymbol> declaredFuns;
    private final Map<String, VarSymbol> declaredVars;
    private final Map<String, OperatorSymbol> declaredOperators;
    private boolean pure;

    public ClassSymbol(String name) {
        super(name);
        this.typeParameters = new LinkedHashMap<>();
        this.parameters = new LinkedHashMap<>();
        this.supers = new LinkedHashMap<>();
        this.declaredFuns = new LinkedHashMap<>();
        this.declaredVars = new LinkedHashMap<>();
        this.declaredOperators = new LinkedHashMap<>();
        this.pure = true;
    }

    public void addTypeParameter(TypeParamSymbol tpSymbol) {
        this.typeParameters.put(tpSymbol.getName(), tpSymbol);
    }

    public void setTypeParameters(Map<String, TypeParamSymbol> typeParameters) {
        this.typeParameters.clear();
        if (typeParameters != null) this.typeParameters.putAll(typeParameters);
    }

    public TypeParamSymbol getTypeParameter(String tpName) {
        return this.typeParameters.get(tpName);
    }

    public Map<String, TypeParamSymbol> getTypeParameters() {
        return Collections.unmodifiableMap(typeParameters);
    }

    public boolean hasTypeParameter(String tpName) {
        return this.typeParameters.containsKey(tpName);
    }

    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    public void addParameter(ParamSymbol pSymbol) {
        this.parameters.put(pSymbol.getName(), pSymbol);
    }

    public void setParameters(Map<String, ParamSymbol> parameters) {
        this.parameters.clear();
        if (parameters != null) this.parameters.putAll(parameters);
    }

    public ParamSymbol getParameter(String pName) {
        return this.parameters.get(pName);
    }

    public Map<String, ParamSymbol> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public boolean hasParameter(String pName) {
        return this.parameters.containsKey(pName);
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public void addSuper(SuperSymbol symbol) {
        this.supers.put(symbol.getName(), symbol);
    }

    public void setSupers(Map<String, SuperSymbol> supers) {
        this.supers.clear();
        if (supers != null) this.supers.putAll(supers);
    }

    public boolean hasSuper(String className) {
        return this.supers.containsKey(className);
    }

    public SuperSymbol getSuper(String className) {
        return this.supers.get(className);
    }

    public Map<String, SuperSymbol> getSupers() {
        return Collections.unmodifiableMap(supers);
    }

    public boolean hasSupers() {
        return !supers.isEmpty();
    }

    public List<ClassSymbol> getBaseSupers() {
        List<ClassSymbol> result = new ArrayList<>();
        for (SuperSymbol sup : supers.values()) {
            Symbol base = sup.getType();
            if (base == null) continue;
            if (base.isClass()) {
                result.add(base.asClass());
            } else if (base.isParameterizedClass()) {
                result.add(base.asParameterizedClass().getBase());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public void addDeclaredFun(FunSymbol fun) {
        if (fun != null) declaredFuns.put(fun.getName(), fun);
    }

    public FunSymbol getDeclaredFun(String name) {
        return declaredFuns.get(name);
    }

    public Map<String, FunSymbol> getDeclaredFuns() {
        return Collections.unmodifiableMap(declaredFuns);
    }

    public boolean hasDeclaredFun(String name) {
        return declaredFuns.containsKey(name);
    }

    public boolean hasDeclaredFuns() {
        return !declaredFuns.isEmpty();
    }

    public void addDeclaredVar(VarSymbol var) {
        if (var != null) declaredVars.put(var.getName(), var);
    }

    public VarSymbol getDeclaredVar(String name) {
        return declaredVars.get(name);
    }

    public Map<String, VarSymbol> getDeclaredVars() {
        return Collections.unmodifiableMap(declaredVars);
    }

    public boolean hasDeclaredVar(String name) {
        return declaredVars.containsKey(name);
    }

    public boolean hasDeclaredVars() {
        return !declaredVars.isEmpty();
    }

    public void addDeclaredOperator(String signature, OperatorSymbol operator) {
        if (signature != null && operator != null)
            declaredOperators.put(signature, operator);
    }

    public OperatorSymbol getDeclaredOperator(String signature) {
        return declaredOperators.get(signature);
    }

    public Map<String, OperatorSymbol> getDeclaredOperators() {
        return Collections.unmodifiableMap(declaredOperators);
    }

    public boolean hasDeclaredOperator(String signature) {
        return declaredOperators.containsKey(signature);
    }

    public boolean hasDeclaredMember(String name) {
        return hasDeclaredFun(name) || hasDeclaredVar(name);
    }

    public MemberSymbol getDeclaredMember(String name) {
        Symbol function = getDeclaredFun(name);
        return function != null ? (MemberSymbol) function : getDeclaredVar(name);
    }

    public Map<String, MemberSymbol> getDeclaredMembers() {
        Map<String, MemberSymbol> members = new LinkedHashMap<>();
        members.putAll(declaredFuns);
        members.putAll(declaredVars);
        return Collections.unmodifiableMap(members);
    }

    public MemberSymbol getMember(String name) {
        return getMember(name, new LinkedHashSet<>());
    }

    private MemberSymbol getMember(String name, Set<ClassSymbol> seen) {
        if (!seen.add(this))
            return null;
        MemberSymbol declared = getDeclaredMember(name);
        if (declared != null)
            return declared;
        for (ClassSymbol parent : getBaseSupers()) {
            MemberSymbol inherited = parent.getMember(name, seen);
            if (inherited != null)
                return inherited;
        }
        return null;
    }

    public void setPure(boolean pure) {
        this.pure = pure;
    }

    public boolean isPure() {
        return pure;
    }
}