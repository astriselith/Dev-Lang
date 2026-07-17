package com.lang.ast;

import com.lang.util.Position;

public class LiteralExpr extends Expr {

	public static final int NULL = 0;
	public static final int BOOLEAN = 1;
	public static final int CHAR = 2;
	public static final int INT = 3;
	public static final int FLOAT = 4;
	public static final int STRING = 5;

	public final int type;
	public final Object value;

	public LiteralExpr(int type, Object value, Position position) {
		super(position);
		this.type = type;
		this.value = value;
	}

	public boolean isNull() {
		return type == NULL;
	}
	public boolean isBoolean() {
		return type == BOOLEAN;
	}
	public boolean isChar() {
		return type == CHAR;
	}
	public boolean isInt()     {
		return type == INT;
	}
	public boolean isFloat()   {
		return type == FLOAT;
	}
	public boolean isString()  {
		return type == STRING;
	}

	public String getTypeName() {
		switch (type) {
		case NULL:
			return "Null";
		case BOOLEAN:
			return "Boolean";
		case CHAR:
			return "Char";
		case INT:
			return "Int";
		case FLOAT:
			return "Float";
		case STRING:
			return "String";
		default:
			return null;
		}
	}
}