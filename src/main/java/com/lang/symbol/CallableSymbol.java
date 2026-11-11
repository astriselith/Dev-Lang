package com.lang.symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CallableSymbol extends MemberSymbol {

    private final Map<String, TypeParamSymbol> typeParameters = new LinkedHashMap<>();
    private final Map<String, ParamSymbol> parameters = new LinkedHashMap<>();
    private Symbol type;

    protected CallableSymbol(String name) {
        super(name);
    }

    public void addTypeParameter(TypeParamSymbol tp) {
        this.typeParameters.put(tp.getName(), tp);
    }

    public void setTypeParameters(Map<String, TypeParamSymbol> typeParameters) {
        this.typeParameters.clear();
        if (typeParameters != null)
            this.typeParameters.putAll(typeParameters);
    }

    public TypeParamSymbol getTypeParameter(String name) {
        return this.typeParameters.get(name);
    }

    public Map<String, TypeParamSymbol> getTypeParameters() {
        return Collections.unmodifiableMap(typeParameters);
    }

    public boolean hasTypeParameter(String name) {
        return this.typeParameters.containsKey(name);
    }

    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    public void addParameter(ParamSymbol p) {
        this.parameters.put(p.getName(), p);
    }

    public void setParameters(Map<String, ParamSymbol> parameters) {
        this.parameters.clear();
        if (parameters != null)
            this.parameters.putAll(parameters);
    }

    public ParamSymbol getParameter(String name) {
        return this.parameters.get(name);
    }

    public Map<String, ParamSymbol> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public boolean hasParameter(String name) {
        return this.parameters.containsKey(name);
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public void setType(Symbol type) {
        this.type = type;
    }

    public Symbol getType() {
        return type;
    }
}