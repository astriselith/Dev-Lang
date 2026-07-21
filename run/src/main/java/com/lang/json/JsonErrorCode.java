package com.lang.json;

public enum JsonErrorCode {
    UNEXPECTED_TOKEN("Unexpected token: %s"),
    EXPECTED_STRING_KEY("Expected string key"),
    EXPECTED_COLON_AFTER_KEY("Expected ':' after key"),
    EXPECTED_RBRACE("Expected '}'"),
    EXPECTED_RBRACKET("Expected ']'"),
    INVALID_VALUE("Invalid JSON value"),
    UNEXPECTED_CONTENT("Unexpected content");

    public static final String TAG = "JSON";

    private final String message;

    JsonErrorCode(String message) {
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
