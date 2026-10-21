package com.lang.register;

public enum RegisteringErrorCode {

    REDEFINED_CLASS("Class '%s' is already defined"),
    REDEFINED_TYPE_PARAMETER("Type parameter '%s' is already defined"),
    REDEFINED_SUPER("Super '%s' is already defined"),
    REDEFINED_BOUND("Bound '%s' is already defined"),
    REDEFINED_VARIABLE("Variable '%s' is already defined"),
    REDEFINED_FUNCTION("Function '%s' is already defined"),
    REDEFINED_PARAMETER("Parameter '%s' is already defined");

    public static final String TAG = "REGISTER";

    private final String message;

    RegisteringErrorCode(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }

    public String getMessage() {
        return message;
    }
}