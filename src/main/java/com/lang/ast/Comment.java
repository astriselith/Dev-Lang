package com.lang.ast;

import com.lang.util.Position;

public class Comment extends Node {
    public String text;

    public Comment(String text, Position position) {
        super(position);
        this.text = text;
    }
}
