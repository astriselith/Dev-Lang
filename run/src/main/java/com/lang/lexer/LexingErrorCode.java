package com.lang.lexer;

public enum LexingErrorCode {
	UNEXPECTED_CHARACTER("Unexpected character: %s"),
	UNTERMINATED_STRING("Unterminated string"),
	INVALID_NUMBER("Invalid number: %s"),
	UNKNOWN_ESCAPE_SEQUENCE("Unknown escape sequence: %s"),
	MALFORMED_COMMENT("Malformed comment"),
	UNTERMINATED_COMMENT("Unterminated comment"),
	INVALID_CHARACTER_LITERAL("Invalid character literal"),
	UNTERMINATED_CHARACTER("Unterminated character"),
	INVALID_TOKEN("Invalid token: %s");

	public static final String TAG = "LEXER";

	private final String message;

	LexingErrorCode(String message) {
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