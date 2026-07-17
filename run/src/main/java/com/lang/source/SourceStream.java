package com.lang.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.lang.codepoint.Codepoint;
import com.lang.codepoint.CodepointStream;

public class SourceStream extends CodepointStream {

    private final ReadableByteChannel channel;
    private final long length;

    private ByteBuffer buffer;
    private static final int BUFFER_SIZE = 8192;

    private long currentByteOffset = 0;
    private boolean eofReached = false;

    public SourceStream(InputStream inputStream) {
        this(inputStream, -1);
    }

    public SourceStream(InputStream inputStream, long length) {
        super(10, 10, 4);
        this.channel = Channels.newChannel(inputStream);
        this.length = length;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.flip();
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
    protected Integer fetchNext() {
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
                buffer.position(pos + 1);
                currentByteOffset += 1;
                continue;
            }

            if (length >= 0 && currentByteOffset + width > length) {
                return Codepoint.EOF;
            }

            if (!ensureBytesAvailable(width)) {
                buffer.position(buffer.limit());
                currentByteOffset += buffer.remaining();
                return Codepoint.EOF;
            }

            int codepoint;
            if (width == 1) {
                codepoint = Codepoint.fromByte(firstByte);
            } else {
                int mask;
                switch (width) {
                    case 2:
                        mask = 0x1F;
                        break;
                    case 3:
                        mask = 0x0F;
                        break;
                    case 4:
                        mask = 0x07;
                        break;
                    default:
                        mask = 0;
                        break;
                }
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
                    buffer.position(pos + 1);
                    currentByteOffset += 1;
                    continue;
                }
            }

            int minValue;
            switch (width) {
                case 2:
                    minValue = 0x80;
                    break;
                case 3:
                    minValue = 0x800;
                    break;
                case 4:
                    minValue = 0x10000;
                    break;
                default:
                    minValue = 0;
                    break;
            }

            if (codepoint < minValue || !Codepoint.isValid(codepoint)) {
                buffer.position(pos + 1);
                currentByteOffset += 1;
                continue;
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
