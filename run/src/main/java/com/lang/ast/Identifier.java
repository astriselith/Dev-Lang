package com.lang.ast;

import com.lang.util.Position;

public class Identifier extends Node {
    public String source;

    public Identifier() {
    }

    public Identifier(String source, Position position) {
        super(position);
        this.source = source;
    }

    public boolean sourceEquals(Identifier other) {
        return this.sourceEquals(source);
    }

    public boolean sourceEquals(String source) {
        return this.source.equals(source);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof Identifier))
            return false;

        return sourceEquals((Identifier) obj);
    }

    @Override
    public String toString() {
        return source;
    }
}
