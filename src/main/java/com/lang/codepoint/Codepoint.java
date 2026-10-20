package com.lang.codepoint;

public final class Codepoint {

    private Codepoint() {
    }

    public static final int EOF = -1;

    public static final int EMPTY = 0x0000;

    private static final int[][] LETTER_RANGES = {
        // Básico Latino
        {0x0041, 0x005A}, // A-Z
        {0x0061, 0x007A}, // a-z
        
        // Latim Suplementar
        {0x00C0, 0x00D6}, // À-Ö
        {0x00D8, 0x00F6}, // Ø-ö
        {0x00F8, 0x00FF}, // ø-ÿ
        
        // Latim Estendido-A
        {0x0100, 0x017F},
        
        // Latim Estendido-B
        {0x0180, 0x024F},
        
        // IPA Extensions
        {0x0250, 0x02AF},
        
        // Spacing Modifier Letters
        {0x02B0, 0x02FF},
        
        // Combining Diacritical Marks
        {0x0300, 0x036F},
        
        // Grego
        {0x0370, 0x03FF},
        
        // Cirílico
        {0x0400, 0x04FF},
        {0x0500, 0x052F},
        
        // Armênio
        {0x0530, 0x058F},
        
        // Hebraico
        {0x0590, 0x05FF},
        
        // Árabe
        {0x0600, 0x06FF},
        {0x0750, 0x077F},
        {0x08A0, 0x08FF},
        
        // Siríaco
        {0x0700, 0x074F},
        
        // Tâna
        {0x0780, 0x07BF},
        
        // N'Ko
        {0x07C0, 0x07FF},
        
        // Samaritano
        {0x0800, 0x083F},
        
        // Mandaean
        {0x0840, 0x085F},
        
        // Devanagari
        {0x0900, 0x097F},
        
        // Bengali
        {0x0980, 0x09FF},
        
        // Gurmukhi
        {0x0A00, 0x0A7F},
        
        // Gujarati
        {0x0A80, 0x0AFF},
        
        // Oriya
        {0x0B00, 0x0B7F},
        
        // Tamil
        {0x0B80, 0x0BFF},
        
        // Telugu
        {0x0C00, 0x0C7F},
        
        // Kannada
        {0x0C80, 0x0CFF},
        
        // Malayalam
        {0x0D00, 0x0D7F},
        
        // Sinhala
        {0x0D80, 0x0DFF},
        
        // Tailandês
        {0x0E00, 0x0E7F},
        
        // Lao
        {0x0E80, 0x0EFF},
        
        // Tibetano
        {0x0F00, 0x0FFF},
        
        // Myanmar
        {0x1000, 0x109F},
        
        // Georgiano
        {0x10A0, 0x10FF},
        
        // Hangul Jamo
        {0x1100, 0x11FF},
        
        // Etiópico
        {0x1200, 0x137F},
        {0x1380, 0x139F},
        
        // Cherokee
        {0x13A0, 0x13FF},
        
        // Silábico Aboriginal Canadense
        {0x1400, 0x167F},
        
        // Ogham
        {0x1680, 0x169F},
        
        // Rúnico
        {0x16A0, 0x16FF},
        
        // Tagalog
        {0x1700, 0x171F},
        
        // Hanunoo
        {0x1720, 0x173F},
        
        // Buhid
        {0x1740, 0x175F},
        
        // Tagbanwa
        {0x1760, 0x177F},
        
        // Khmer
        {0x1780, 0x17FF},
        
        // Mongol
        {0x1800, 0x18AF},
        
        // Limbu
        {0x1900, 0x194F},
        
        // Tai Le
        {0x1950, 0x197F},
        
        // New Tai Lue
        {0x1980, 0x19DF},
        
        // Khmer Symbols
        {0x19E0, 0x19FF},
        
        // Buginese
        {0x1A00, 0x1A1F},
        
        // Tai Tham
        {0x1A20, 0x1AAF},
        
        // Balinês
        {0x1B00, 0x1B7F},
        
        // Sundanês
        {0x1B80, 0x1BBF},
        
        // Batak
        {0x1BC0, 0x1BFF},
        
        // Lepcha
        {0x1C00, 0x1C4F},
        
        // Ol Chiki
        {0x1C50, 0x1C7F},
        
        // Cirílico Estendido
        {0x1C80, 0x1C8F},
        
        // Saurashtra
        {0x1A80, 0x1AEF},
        
        // Kayah Li
        {0x1A90, 0x1A9F},
        
        // Rejang
        {0x1AA0, 0x1AAF},
        
        // Cham
        {0x1AA0, 0x1AFF},
        
        // Vai
        {0x1A20, 0x1AAF},
        
        // Latim Estendido-C
        {0x2C60, 0x2C7F},
        
        // Copta
        {0x2C80, 0x2CFF},
        
        // Georgiano Suplementar
        {0x2D00, 0x2D2F},
        
        // Tifinagh
        {0x2D30, 0x2D7F},
        
        // Etiópico Estendido
        {0x2D80, 0x2DDF},
        
        // Cirílico Estendido-A
        {0x2DE0, 0x2DFF},
        
        // Cherokee Suplementar
        {0xAB70, 0xABBF},
        
        // Latim Estendido-D
        {0xA720, 0xA7FF},
        
        // Syloti Nagri
        {0xA800, 0xA82F},
        
        // Phags-pa
        {0xA840, 0xA87F},
        
        // Saurashtra
        {0xA880, 0xA8DF},
        
        // Devanagari Estendido
        {0xA8E0, 0xA8FF},
        
        // Kayah Li
        {0xA900, 0xA92F},
        
        // Rejang
        {0xA930, 0xA95F},
        
        // Hangul Jamo Estendido-A
        {0xA960, 0xA97F},
        
        // Javanês
        {0xA980, 0xA9DF},
        
        // Myanmar Estendido-B
        {0xA9E0, 0xA9FF},
        
        // Cham
        {0xAA00, 0xAA5F},
        
        // Myanmar Estendido-A
        {0xAA60, 0xAA7F},
        
        // Tai Viet
        {0xAA80, 0xAADF},
        
        // Meetei Mayek Extensions
        {0xAAE0, 0xAAFF},
        
        // Etiópico Estendido-A
        {0xAB00, 0xAB2F},
        
        // Latim Estendido-E
        {0xAB30, 0xAB6F},
        
        // Cherokee
        {0xAB70, 0xABBF},
        
        // Meetei Mayek
        {0xABC0, 0xABFF},
        
        // Hangul Syllables
        {0xAC00, 0xD7AF},
        
        // Hangul Jamo Estendido-B
        {0xD7B0, 0xD7FF},
        
        // Ideográficos CJK
        {0x4E00, 0x9FFF},
        
        // CJK Extension A
        {0x3400, 0x4DBF},
        
        // CJK Extension B
        {0x20000, 0x2A6DF},
        
        // CJK Extension C
        {0x2A700, 0x2B73F},
        
        // CJK Extension D
        {0x2B740, 0x2B81F},
        
        // CJK Extension E
        {0x2B820, 0x2CEAF},
        
        // CJK Extension F
        {0x2CEB0, 0x2EBEF},
        
        // CJK Extension G
        {0x30000, 0x3134F},
        
        // CJK Extension H
        {0x31350, 0x323AF},
        
        // CJK Compatibility Ideographs
        {0xF900, 0xFAFF},
        
        // Alfabeto Fonético Internacional
        {0x1D00, 0x1D7F},
        
        // Fonético Estendido
        {0x1D80, 0x1DBF},
        
        // Latim Estendido Adicional
        {0x1E00, 0x1EFF},
        
        // Grego Estendido
        {0x1F00, 0x1FFF},
        
        // Letras Simbólicas
        {0x2100, 0x214F},
        
        // Sobrescritos e Subscritos
        {0x2070, 0x209F},
        
        // Letras Matemáticas
        {0x1D400, 0x1D7FF},
        
        // Símbolos e Pictogramas
        {0x1F130, 0x1F149}, // Letras quadradas
        {0x1F150, 0x1F169}, // Letras circulares
        {0x1F170, 0x1F189}  // Letras negativas
    };

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

    public static int mask(int width) {
        switch (width) {
            case 2:
                return 0x1F;
            case 3:
                return 0x0F;
            case 4:
                return 0x07;
            default:
                return 0;
        }
    }

    public static int minValue(int width) {
        switch (width) {
            case 2:
                return 0x80;
            case 3:
                return 0x800;
            case 4:
                return 0x10000;
            default:
                return 0;
        }
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
        for (int[] range : LETTER_RANGES) {
            if (codepoint >= range[0] && codepoint <= range[1]) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAlphanumeric(int codepoint) {
        return isDigit(codepoint) || isLetter(codepoint);
    }

    public static boolean isIdentifierStart(int codepoint) {
        return isLetter(codepoint) || codepoint == '_';
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