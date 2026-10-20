package com.lang.util;

import java.util.Objects;

public final class Diagnostic {

    public static final int ERROR   = 1;
    public static final int WARNING = 2;
    public static final int INFO    = 3;
    public static final int HINT    = 4;

    private final Position position;
    private final String message;
    private final int severity;

    public Diagnostic(Position position, String message, int severity) {
        this.position = position;
        this.message  = message;
        this.severity = severity;
    }


    public Position getPosition() { return position; }
    public String getMessage()    { return message; }
    public int getSeverity()      { return severity; }

    public static Diagnostic of(Position position, String message, int severity) {
        return new Diagnostic(position, message, severity);
    }

    public static Diagnostic error(Position position, String message) {
        return new Diagnostic(position, message, ERROR);
    }

    public static Diagnostic warning(Position position, String message) {
        return new Diagnostic(position, message, WARNING);
    }

    public static Diagnostic info(Position position, String message) {
        return new Diagnostic(position, message, INFO);
    }

    public static Diagnostic hint(Position position, String message) {
        return new Diagnostic(position, message, HINT);
    }

    public boolean isError()   { return severity == ERROR; }
    public boolean isWarning() { return severity == WARNING; }
    public boolean isInfo()    { return severity == INFO; }
    public boolean isHint()    { return severity == HINT; }

    public static String severityName(int severity) {
        switch (severity) {
            case ERROR:   return "error";
            case WARNING: return "warning";
            case INFO:    return "info";
            case HINT:    return "hint";
            default:      return "unknown(" + severity + ")";
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
        sb.append(' ').append(severityName(severity)).append(": ").append(message);
        return sb.toString();
    }

    @Override public String toString() { return format(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Diagnostic)) return false;
        Diagnostic that = (Diagnostic) obj;
        return severity == that.severity
                && Objects.equals(position, that.position)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, message, severity);
    }
}