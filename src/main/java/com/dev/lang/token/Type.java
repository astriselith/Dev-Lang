package com.dev.lang.token;

public enum Type {

	UNDEFINED("undefined"),
	// Literais e Identificadores
	NULL("null"),
	BOOLEAN("boolean"),
	CHAR("char"),
	INT("int"),
	FLOAT("float"),
	STRING("string"),
	IDENTIFIER("identifier"),

	// Modificadores
	CLASS("class"),

	// Membros
	VAR("var"),
	FUN("fun"),

	// Palavras-chave
	LET("let"),
	IF("if"),
	ELSE("else"),
	WHILE("while"),
	BREAK("break"),
	CONTINUE("continue"),
	RETURN("return"),

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