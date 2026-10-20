package com.lang.source;

public enum SourceErrorCode {

    INVALID_LEADING_BYTE("Invalid leading byte at position %d"),
    INVALID_CONTINUATION("Invalid continuation byte at position %d"),
    OVERLONG_ENCODING("Overlong UTF-8 encoding at position %d"),
    SURROGATE_CODEPOINT("Surrogate codepoint U+%04X at position %d"),
    OUT_OF_RANGE_CODEPOINT("Codepoint U+%04X out of range at position %d"),
    TRUNCATED_SEQUENCE("Truncated UTF-8 sequence at position %d"),
    UNPAIRED_SURROGATE("Unpaired surrogate at position %d"),

    BOM_PRESENT("Byte order mark at position %d"),
    CONTROL_CHARACTER("Control character U+%04X at position %d"),
    NULL_BYTE("Null byte at position %d"),
    REPLACEMENT_CHARACTER("Replacement character U+FFFD at position %d");

    public static final String TAG = "SOURCE";

    private final String message;

    SourceErrorCode(String message) {
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