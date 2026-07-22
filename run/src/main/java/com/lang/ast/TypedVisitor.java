package com.lang.ast;

public interface TypedVisitor<T> {
    T visitRefTyped(RefTyped typed);

    T visitParameterizedRefTyped(ParameterizedRefTyped typed);
}
