package com.lang.ast;

import com.lang.util.Position;

public class Comment extends Node {
    private final String text;

    public Comment(String text, Position position) {
        super(position);
        this.text = text;
    }

    public Comment(String text) {
        this(text, Position.UNKNOWN);
    }

    public String getText() {
        return text;
    }

}
