package com.lang.token;

public enum Type {

	UNDEFINED("undefined"),
	// Literais
	NULL("null"),
	BOOL("bool"),
	CHAR("char"),
	INT("int"),
	FLOAT("float"),
	STRING("string"),

	// Identificador
	IDENTIFIER("identifier"),

	// Comentários
	COMMENT("comment"),
	MULTILINE_COMMENT("multiline_comment"),

	// Símbolos Únicos (cada caractere é um token)
	PLUS("+"),
	MINUS("-"),
	STAR("*"),
	SLASH("/"),
	PERCENT("%"),
	EQUALS("="),
	BANG("!"),
	LANGLE("<"),
	RANGLE(">"),
	AMP("&"),
	BAR("|"),
	QUESTION("?"),
	COLON(":"),
	LPAREN("("),
	RPAREN(")"),
	LBRACE("{"),
	RBRACE("}"),
	LBRACKET("["),
	RBRACKET("]"),
	COMMA(","),
	DOT("."),
	SEMICOLON(";"),
	DOLLAR("$"),
	AT("@"),

	EOF("eof");

	private final String symbol;

	Type(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	@Override
	public String toString() {
		return symbol;
	}
}