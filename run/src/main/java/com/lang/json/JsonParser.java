package com.lang.json;

import com.lang.token.Token;
import com.lang.token.TokenStream;
import com.lang.unit.CompilationUnit;

import java.util.*;

import static com.lang.json.JsonErrorCode.*;
import static com.lang.token.Type.*;

public class JsonParser {
	private final CompilationUnit unit;
	private TokenStream stream;

	public JsonParser(TokenStream stream, CompilationUnit unit) {
		this.stream = stream;
		this.unit = unit != null ? unit : new CompilationUnit();

		if (stream == null) {
			throw new IllegalArgumentException("TokenStream cannot be null");
		}

		this.stream.setHandler((token) -> {
			if (token.isComment()) {
				return false;
			}
			return true;
		});
	}

	public Json parse() {
		return value();
	}

	private Json value() {

		if (stream.check(LBRACE)) {
			return object();
		}

		if (stream.check(LBRACKET)) {
			return array();
		}

		Token token = stream.advance();

		if (token.typeEquals(NULL)) {
			return new JsonNull(token.getPosition());
		}

		if (token.typeEquals(BOOL)) {
			return new JsonBoolean(Boolean.parseBoolean(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(INT)) {
			return new JsonNumber(Long.parseLong(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(FLOAT)) {
			return new JsonNumber(Double.parseDouble(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(STRING)) {
			return new JsonString(token.lexeme.substring(1, token.lexeme.length() - 1), token.getPosition());
		}

		throw unit.error(TAG, UNEXPECTED_TOKEN.format(token.lexeme), token);
	}

	private JsonObject object() {
		Token start = stream.expect(LBRACE);
		Map<String, Json> fields = new LinkedHashMap<>();

		if (stream.match(RBRACE)) {
			return new JsonObject(fields, start.getPosition());
		}

		do {
			Token keyToken = stream.peek();

			if (!stream.match(STRING)) {
				throw unit.error(TAG, EXPECTED_STRING_KEY.getMessage(), keyToken);
			}

			String key = keyToken.lexeme.substring(1, keyToken.lexeme.length() - 1);

			if (!stream.match(COLON)) {
				throw unit.error(TAG, EXPECTED_COLON_AFTER_KEY.getMessage(), stream.previous());
			}

			fields.put(key, value());

		} while (stream.match(COMMA));

		if (!stream.match(RBRACE)) {
			throw unit.error(TAG, EXPECTED_RBRACE.getMessage(), stream.peek());
		}

		return new JsonObject(fields, start.getPosition());
	}

	private JsonArray array() {
		Token start = stream.expect(LBRACKET);
		List<Json> elements = new ArrayList<>();

		if (stream.match(RBRACKET)) {
			return new JsonArray(elements, start.getPosition());
		}

		do {
			elements.add(value());
		} while (stream.match(COMMA));

		if (!stream.match(RBRACKET)) {
			throw unit.error(TAG, EXPECTED_RBRACKET.getMessage(), stream.peek());
		}

		return new JsonArray(elements, start.getPosition());
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}
}