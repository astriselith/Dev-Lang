package com.lang.unit;

import com.lang.util.Position;
import com.lang.util.Positioned;

public class WarningException extends Exception {
	private final String tag;
	private final String message;
	private final Position position;

	public WarningException(String tag, String message, Position position) {
		super(formatMessage(tag, message, position));
		this.tag = tag;
		this.message = message;
		this.position = position;
	}

	public WarningException(String tag, String message, Positioned positioned) {
		this(tag, message, positioned != null ? positioned.getPosition() : null);
	}

	public WarningException(String tag, String message) {
		super(formatMessage(tag, message, null));
		this.tag = tag;
		this.message = message;
		this.position = null;
	}

	public WarningException(String tag, String message, Position position, Throwable cause) {
		super(formatMessage(tag, message, position), cause);
		this.tag = tag;
		this.message = message;
		this.position = position;
	}

	public WarningException(String tag, String message, Positioned positioned, Throwable cause) {
		this(tag, message, positioned != null ? positioned.getPosition() : null, cause);
	}

	public WarningException(String tag, String message, Throwable cause) {
		super(formatMessage(tag, message, null), cause);
		this.tag = tag;
		this.message = message;
		this.position = null;
	}

	private static String formatMessage(String tag, String message, Position position) {
		if (position == null) {
			return String.format("[%s] %s", tag, message);
		}
		return String.format("[%s] %s [line %d, lineStart %d]", tag, message, position.getLine(), position.getColumn());
	}

	public String getTag() {
		return tag;
	}

	public String getMessage() {
		return message;
	}

	public Position getPosition() {
		return position;
	}

	@Override
	public String toString() {
		return getMessage();
	}
}