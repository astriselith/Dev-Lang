package com.lang.codepoint;

public final class Codepoint {

	private Codepoint() {
	}

	public static final int EOF = -1;

	public static int width(byte b) {
		int v = b & 0xFF;
		if ((v & 0x80) == 0)
			return 1;
		if ((v & 0xE0) == 0xC0)
			return 2;
		if ((v & 0xF0) == 0xE0)
			return 3;
		if ((v & 0xF8) == 0xF0)
			return 4;
		return 0;
	}

	public static boolean isContinuation(byte b) {
		return (b & 0xC0) == 0x80;
	}

	public static boolean isSurrogate(int codepoint) {
		return codepoint >= 0xD800 && codepoint <= 0xDFFF;
	}

	public static boolean isValid(int codepoint) {
		return codepoint >= 0 && codepoint <= 0x10FFFF && !isSurrogate(codepoint);
	}

	public static boolean isWhitespace(int codepoint) {
		return codepoint == ' ' || codepoint == '\t' || codepoint == '\n' ||
				codepoint == '\r' || codepoint == '\f' || codepoint == '\u000B';
	}

	public static boolean isLineBreak(int codepoint) {
		return codepoint == '\n' || codepoint == '\r';
	}

	public static boolean isDigit(int codepoint) {
		return codepoint >= '0' && codepoint <= '9';
	}

	public static boolean isLetter(int codepoint) {
		return (codepoint >= 'A' && codepoint <= 'Z') ||
				(codepoint >= 'a' && codepoint <= 'z');
	}

	public static boolean isAlphanumeric(int codepoint) {
		return isDigit(codepoint) || isLetter(codepoint);
	}

	public static boolean isIdentifier(int codepoint) {
		return isAlphanumeric(codepoint) || codepoint == '_';
	}

	public static int fromByte(byte b) {
		return b & 0xFF;
	}

	public static boolean equalsIgnoreCase(int cp1, int cp2) {
		if (cp1 == cp2)
			return true;
		if (cp1 >= 'A' && cp1 <= 'Z')
			return cp1 + 32 == cp2;
		if (cp1 >= 'a' && cp1 <= 'z')
			return cp1 - 32 == cp2;
		return false;
	}

	public static int toChars(int codepoint, char[] dst, int index) {
		if (!isValid(codepoint)) {
			return 0;
		}
		if (codepoint <= 0xFFFF) {
			dst[index] = (char) codepoint;
			return 1;
		}

		codepoint -= 0x10000;
		dst[index] = (char) (0xD800 | (codepoint >> 10));
		dst[index + 1] = (char) (0xDC00 | (codepoint & 0x3FF));
		return 2;
	}

	public static String toString(int codepoint) {
		if (codepoint == EOF) {
			return "EOF";
		}

		if (!isValid(codepoint)) {
			return String.format("U+%04X (invalid)", codepoint);
		}

		if (codepoint < 0x20 || codepoint == 0x7F) {
			switch (codepoint) {
				case 0x00:
					return "U+0000 (NUL)";
				case 0x01:
					return "U+0001 (SOH)";
				case 0x02:
					return "U+0002 (STX)";
				case 0x03:
					return "U+0003 (ETX)";
				case 0x04:
					return "U+0004 (EOT)";
				case 0x05:
					return "U+0005 (ENQ)";
				case 0x06:
					return "U+0006 (ACK)";
				case 0x07:
					return "U+0007 (BEL)";
				case 0x08:
					return "U+0008 (BS)";
				case 0x09:
					return "U+0009 (TAB)";
				case 0x0A:
					return "U+000A (LF)";
				case 0x0B:
					return "U+000B (VT)";
				case 0x0C:
					return "U+000C (FF)";
				case 0x0D:
					return "U+000D (CR)";
				case 0x0E:
					return "U+000E (SO)";
				case 0x0F:
					return "U+000F (SI)";
				case 0x10:
					return "U+0010 (DLE)";
				case 0x11:
					return "U+0011 (DC1)";
				case 0x12:
					return "U+0012 (DC2)";
				case 0x13:
					return "U+0013 (DC3)";
				case 0x14:
					return "U+0014 (DC4)";
				case 0x15:
					return "U+0015 (NAK)";
				case 0x16:
					return "U+0016 (SYN)";
				case 0x17:
					return "U+0017 (ETB)";
				case 0x18:
					return "U+0018 (CAN)";
				case 0x19:
					return "U+0019 (EM)";
				case 0x1A:
					return "U+001A (SUB)";
				case 0x1B:
					return "U+001B (ESC)";
				case 0x1C:
					return "U+001C (FS)";
				case 0x1D:
					return "U+001D (GS)";
				case 0x1E:
					return "U+001E (RS)";
				case 0x1F:
					return "U+001F (US)";
				case 0x7F:
					return "U+007F (DEL)";
				default:
					return String.format("U+%04X", codepoint);
			}
		}

		char[] chars = new char[2];
		int len = toChars(codepoint, chars, 0);
		return new String(chars, 0, len);
	}
}