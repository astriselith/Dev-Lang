package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lang.ast.Modifier;

public abstract class MemberSymbol extends Symbol {
    private Map<String, AnnoSymbol> annotations;
    private List<Modifier> modifiers;
    private ClassSymbol declaringClass;

    protected MemberSymbol(String name) {
        super(name);
        this.annotations = new LinkedHashMap<>();
        this.modifiers = new ArrayList<>();
        this.declaringClass = null;
    }

    public void addAnnotation(AnnoSymbol annotation) {
        if (annotation != null)
            this.annotations.put(annotation.getName(), annotation);
    }
    
    public void setAnnotations(Map<String, AnnoSymbol> annotations) {
        this.annotations.clear();
        if (annotations != null)
            this.annotations.putAll(annotations);
    }

    public boolean hasAnnotation(String className) {
        return this.annotations.containsKey(className);
    }

    public AnnoSymbol getAnnotation(String className) {
        return this.annotations.get(className);
    }

    public Map<String, AnnoSymbol> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }


    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public boolean hasModifier(int type) {
        return modifiers != null && modifiers.stream().anyMatch(m -> m.type == type);
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public void setDeclaringClass(ClassSymbol declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ClassSymbol getDeclaringClass() {
        return declaringClass;
    }

    
}
