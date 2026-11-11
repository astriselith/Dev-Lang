package com.lang.ast;

import com.lang.util.Position;

public class Modifier extends Node {

    public static final int PUBLIC = 1 << 0;
    public static final int PRIVATE = 1 << 1;
    public static final int FINAL = 1 << 2;
    public static final int STATIC = 1 << 3;

    public static final int ALL = PUBLIC | PRIVATE | FINAL | STATIC;
    public static final int VISIBILITY = PUBLIC | PRIVATE;

    private static final int[] TYPES = { PUBLIC, PRIVATE, FINAL, STATIC };
    private static final String[] NAMES = { "public", "private", "final", "static" };

    public int type;

    public Modifier() {
    }

    public Modifier(int type, Position position) {
        super(position);
        this.type = type;
    }

    public static boolean isPublic(int type) {
        return (type & PUBLIC) != 0;
    }

    public boolean isPublic() {
        return (type & PUBLIC) != 0;
    }

    public static boolean isPrivate(int type) {
        return (type & PRIVATE) != 0;
    }

    public boolean isPrivate() {
        return (type & PRIVATE) != 0;
    }

    public static boolean isFinal(int type) {
        return (type & FINAL) != 0;
    }

    public boolean isFinal() {
        return (type & FINAL) != 0;
    }

    public static boolean isStatic(int type) {
        return (type & STATIC) != 0;
    }

    public boolean isStatic() {
        return (type & STATIC) != 0;
    }

    public static boolean has(int type, int flag) {
        return (type & flag) != 0;
    }

    public boolean has(int flag) {
        return (type & flag) != 0;
    }

    public boolean isValid() {
        return (type & ~ALL) == 0;
    }

    public boolean hasConflict() {
        return (type & VISIBILITY) == VISIBILITY;
    }

    public static String name(int type) {
        for (int i = 0; i < TYPES.length; i++)
            if (TYPES[i] == type)
                return NAMES[i];
        return "";
    }

    public static int type(String name) {
        if (name == null)
            return 0;
        for (int i = 0; i < NAMES.length; i++)
            if (NAMES[i].equals(name))
                return TYPES[i];
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TYPES.length; i++) {
            if ((type & TYPES[i]) == 0)
                continue;
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(NAMES[i]);
        }
        return sb.toString();
    }
}