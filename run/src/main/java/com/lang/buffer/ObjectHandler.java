package com.lang.buffer;

@FunctionalInterface
public interface ObjectHandler<T> {
    boolean handle(T object);
}