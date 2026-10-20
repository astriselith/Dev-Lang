package com.lang.source;

import static com.lang.source.SourceErrorCode.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.lang.codepoint.Codepoint;
import com.lang.codepoint.CodepointStream;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

public class Source extends CodepointStream {

    private static final int BUFFER_SIZE = 8192;

    private final ReadableByteChannel channel;
    private final long length;
    private final ByteBuffer buffer;
    private final CompilationUnit unit;

    private long currentByteOffset = 0;
    private boolean eofReached = false;

    private Source(InputStream inputStream, long length, CompilationUnit unit) {
        super(10);
        this.channel = Channels.newChannel(inputStream);
        this.length = length;
        this.unit = unit;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.flip();
    }

    public static Source fromString(String code, CompilationUnit unit) {
        byte[] bytes = code.getBytes(StandardCharsets.UTF_8);
        return new Source(new ByteArrayInputStream(bytes), bytes.length, unit);
    }

    public static Source fromBytes(byte[] bytes, CompilationUnit unit) {
        return new Source(new ByteArrayInputStream(bytes), bytes.length, unit);
    }

    public static Source fromStream(InputStream inputStream, long length, CompilationUnit unit) {
        return new Source(inputStream, length, unit);
    }

    public static Source fromFile(Path path, CompilationUnit unit) throws IOException {
        return new Source(Files.newInputStream(path, StandardOpenOption.READ), Files.size(path), unit);
    }

    private void error(SourceErrorCode code, long byteOffset, Object... args) {
        if (unit == null) return;
        unit.addDiagnostic(TAG, Diagnostic.error(null,
                code.format(args) + " [byte " + byteOffset + "]"));
    }

    private void warning(SourceErrorCode code, long byteOffset, Object... args) {
        if (unit == null) return;
        unit.addDiagnostic(TAG, Diagnostic.warning(null,
                code.format(args) + " [byte " + byteOffset + "]"));
    }

    private void fillBuffer() {
        try {
            buffer.compact();
            int maxRead = buffer.remaining();
            if (length >= 0) {
                long remaining = length - currentByteOffset;
                if (remaining <= 0) {
                    buffer.flip();
                    eofReached = true;
                    return;
                }
                if (remaining < maxRead) {
                    maxRead = (int) remaining;
                }
            }
            buffer.limit(buffer.position() + maxRead);
            int read = channel.read(buffer);
            buffer.flip();
            if (read == -1) {
                eofReached = true;
            }
        } catch (IOException e) {
            buffer.limit(0);
            eofReached = true;
        }
    }

    private boolean ensureBytesAvailable(int needed) {
        if (buffer.remaining() >= needed) {
            return true;
        }
        if (eofReached) {
            return false;
        }
        if (length >= 0 && currentByteOffset + needed > length) {
            return false;
        }
        fillBuffer();
        return buffer.remaining() >= needed;
    }

    @Override
    protected int fetchNext() {
        while (true) {
            if (length >= 0 && currentByteOffset >= length) {
                return Codepoint.EOF;
            }

            if (!ensureBytesAvailable(1)) {
                return Codepoint.EOF;
            }

            int pos = buffer.position();
            byte firstByte = buffer.get(pos);
            int width = Codepoint.width(firstByte);

            if (width == 0) {
                error(INVALID_LEADING_BYTE, currentByteOffset);
                buffer.position(pos + 1);
                currentByteOffset += 1;
                continue;
            }

            if (length >= 0 && currentByteOffset + width > length) {
                error(TRUNCATED_SEQUENCE, currentByteOffset);
                buffer.position(buffer.limit());
                currentByteOffset += buffer.remaining();
                return Codepoint.EOF;
            }

            if (!ensureBytesAvailable(width)) {
                error(TRUNCATED_SEQUENCE, currentByteOffset);
                buffer.position(buffer.limit());
                currentByteOffset += buffer.remaining();
                return Codepoint.EOF;
            }

            int codepoint;
            if (width == 1) {
                codepoint = Codepoint.fromByte(firstByte);
            } else {
                int mask = Codepoint.mask(width);
                codepoint = (firstByte & mask) << (6 * (width - 1));
                boolean invalid = false;

                for (int i = 1; i < width; i++) {
                    byte b = buffer.get(pos + i);
                    if (!Codepoint.isContinuation(b)) {
                        invalid = true;
                        break;
                    }
                    codepoint |= (b & 0x3F) << (6 * (width - 1 - i));
                }

                if (invalid) {
                    error(INVALID_CONTINUATION, currentByteOffset);
                    buffer.position(pos + 1);
                    currentByteOffset += 1;
                    continue;
                }
            }

            int minValue = Codepoint.minValue(width);

            if (codepoint < minValue) {
                error(OVERLONG_ENCODING, currentByteOffset);
                buffer.position(pos + width);
                currentByteOffset += width;
                continue;
            }

            if (Codepoint.isSurrogate(codepoint)) {
                error(SURROGATE_CODEPOINT, currentByteOffset, codepoint);
                buffer.position(pos + width);
                currentByteOffset += width;
                continue;
            }

            if (!Codepoint.isValid(codepoint)) {
                error(OUT_OF_RANGE_CODEPOINT, currentByteOffset, codepoint);
                buffer.position(pos + width);
                currentByteOffset += width;
                continue;
            }

            if (codepoint == 0xFEFF && currentByteOffset == 0) {
                warning(BOM_PRESENT, currentByteOffset);
                buffer.position(pos + width);
                currentByteOffset += width;
                continue;
            }

            if (codepoint == Codepoint.EMPTY) {
                warning(NULL_BYTE, currentByteOffset);
            } else if (codepoint < 0x20 && !Codepoint.isWhitespace(codepoint)) {
                warning(CONTROL_CHARACTER, currentByteOffset, codepoint);
            } else if (codepoint == 0xFFFD) {
                warning(REPLACEMENT_CHARACTER, currentByteOffset);
            }

            buffer.position(pos + width);
            currentByteOffset += width;
            return codepoint;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}