package com.lang.ast;

import com.lang.util.Position;

public class Comment extends Node {
    public String text;

    public Comment() {
    }

    public Comment(String text, Position position) {
        this.position = position;
        this.text = text;
    }

    public Comment(String text) {
        this(text, Position.UNKNOWN);
    }

    public String getText() {
        return text;
    }
}
