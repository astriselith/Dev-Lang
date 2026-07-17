package com.lang.parser;

public enum ParsingErrorCode {
	EXPECTED_TOKEN("Expected %s, found %s"),
	UNEXPECTED_TOKEN("Unexpected token: %s"),
	EXPECTED_AFTER("Expected %s after %s"),
	EXPECTED_BEFORE("Expected %s before %s"),
	INVALID_EXPR_STMT("Expression cannot be used as a statement"),
	UNEXPECTED_CONTENT("Unexpected content"),
	NO_CLASS("No class declared in file"),
	EXPECTED_FUNCTION_BODY("Expected function body"),
	EXPECTED_MEMBER("Expected member"),
	EXPECTED_PARAM("Expected parameter"),
	EXPECTED_TYPE("Expected type"),
	EXPECTED_SUPERCLASS("Expected superclass"),
	EXPECTED_GENERIC_PARAM("Expected generic parameter"),
	EXPECTED_GENERIC_ARG("Expected generic argument"),
	EXPECTED_ASSIGN("Expected assignment target"),
	DUPLICATE_MODIFIER("Duplicate modifier: %s"),
	CONFLICTING_MODIFIERS("Conflicting modifiers: %s");

	public static final String TAG = "PARSER";

	private final String message;

	ParsingErrorCode(String message) {
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