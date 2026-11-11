package com.lang.util;

import java.util.Objects;

public final class Message {
    public static final int TOAST  = 1;
    public static final int POPUP  = 2;
    public static final int BANNER = 3;
    public static final int HINT   = 4;

    private final int type;
    private final String title;
    private final String message;
    private final Position position;

    public Message(int type, String title, String message, Position position) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.position = position;
    }

    public Message(int type, String title, String message) {
        this(type, title, message, null);
    }

    public static Message toast(String title, String message) {
        return new Message(TOAST, title, message);
    }

    public static Message popup(String title, String message) {
        return new Message(POPUP, title, message);
    }

    public static Message banner(String title, String message) {
        return new Message(BANNER, title, message);
    }

    public static Message hint(String title, String message, Position position) {
        return new Message(HINT, title, message, position);
    }

    public int getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Position getPosition() { return position; }

    public static String typeName(int type) {
        switch (type) {
            case TOAST:  return "TOAST";
            case POPUP:  return "POPUP";
            case BANNER: return "BANNER";
            case HINT:   return "HINT";
            default:     return "UNKNOWN(" + type + ")";
        }
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        if (position != null) {
            sb.append(position.getFirstLineCount())
              .append(':')
              .append(position.getColumnStart())
              .append(':');
        }
        sb.append(" [").append(typeName(type)).append("]");
        if (title != null && !title.isEmpty()) {
            sb.append(" ").append(title).append(":");
        }
        sb.append(" ").append(message);
        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Message)) return false;
        Message other = (Message) obj;
        return type == other.type &&
                Objects.equals(title, other.title) &&
                Objects.equals(message, other.message) &&
                Objects.equals(position, other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, message, position);
    }
}
