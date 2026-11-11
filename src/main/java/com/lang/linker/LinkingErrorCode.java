package com.lang.linker;

public enum LinkingErrorCode {

    UNRESOLVED_TYPE("Type '%s' could not be resolved"),
    INVALID_TYPE_PARAMETER_BOUND("Bound '%s' of type parameter '%s' must be a class type, not a type parameter"),
    INVALID_SUPER_TYPE("Super type '%s' of class '%s' is not a class type"),
    INVALID_INFERRED_VARIABLE_INITIALIZER(
            "Global or class variable '%s' requires an explicit type unless its initializer is a literal"),
    INVALID_ANNOTATION_TYPE("Annotation '%s' must refer to a class type");

    public static final String TAG = "LINKER";

    private final String message;

    LinkingErrorCode(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }

    public String getMessage() {
        return message;
    }

    public String getTag() {
        return TAG;
    }
}