/**
 * Copyright 2016 Mattias Karlsson
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streaminghost.radiosoundfm.services.metadata;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class PeekInputStream extends InputStream {
    private static final byte[] SCRATCH_SPACE = new byte[4096];
    private final InputStream stream;
    private final long streamLength;
    private long position;
    private byte[] peekBuffer = new byte[8192];
    private int peekBufferPosition;
    private int peekBufferLength;

    PeekInputStream(InputStream stream) {
        this(stream, 0, -1L);
    }

    private PeekInputStream(InputStream stream, long position, long length) {
        this.stream = stream;
        this.position = position;
        this.streamLength = length;
    }

    @Override
    public int read() throws IOException {
        int bytesRead = this.readFromPeekBuffer();
        if (bytesRead == 0) {
            try {
                bytesRead = this.readFromStream();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.commitBytesRead(bytesRead);
        return bytesRead;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        int bytesRead = this.readFromPeekBuffer(target, offset, length);
        if (bytesRead == 0) {
            try {
                bytesRead = this.readFromStream(target, offset, length, 0, true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.commitBytesRead(bytesRead);
        return bytesRead;
    }

    private boolean readFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException, InterruptedException {
        int bytesRead;
        for (bytesRead = this.readFromPeekBuffer(target, offset, length); bytesRead < length && bytesRead != -1; bytesRead = this.readFromStream(target, offset, length, bytesRead, allowEndOfInput)) {
        }

        this.commitBytesRead(bytesRead);
        return bytesRead != -1;
    }

    public void readFully(byte[] target, int offset, int length) throws IOException, InterruptedException {
        this.readFully(target, offset, length, false);
    }

    public int skip(int length) throws IOException, InterruptedException {
        int bytesSkipped = this.skipFromPeekBuffer(length);
        if (bytesSkipped == 0) {
            bytesSkipped = this.readFromStream(SCRATCH_SPACE, 0, Math.min(length, SCRATCH_SPACE.length), 0, true);
        }

        this.commitBytesRead(bytesSkipped);
        return bytesSkipped;
    }

    private boolean skipFully(int length, boolean allowEndOfInput) throws IOException, InterruptedException {
        int bytesSkipped;
        for (bytesSkipped = this.skipFromPeekBuffer(length); bytesSkipped < length && bytesSkipped != -1; bytesSkipped = this.readFromStream(SCRATCH_SPACE, -bytesSkipped, Math.min(length, bytesSkipped + SCRATCH_SPACE.length), bytesSkipped, allowEndOfInput)) {
        }

        this.commitBytesRead(bytesSkipped);
        return bytesSkipped != -1;
    }

    public void skipFully(int length) throws IOException, InterruptedException {
        this.skipFully(length, false);
    }

    boolean peekFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException, InterruptedException {
        if (!this.advancePeekPosition(length, allowEndOfInput)) {
            return false;
        } else {
            System.arraycopy(this.peekBuffer, this.peekBufferPosition - length, target, offset, length);
            return true;
        }
    }

    void peekFully(byte[] target, int offset, int length) throws IOException, InterruptedException {
        this.peekFully(target, offset, length, false);
    }

    private boolean advancePeekPosition(int length, boolean allowEndOfInput) throws IOException, InterruptedException {
        this.ensureSpaceForPeek(length);
        int bytesPeeked = Math.min(this.peekBufferLength - this.peekBufferPosition, length);
        this.peekBufferLength += length - bytesPeeked;

        do {
            if (bytesPeeked >= length) {
                this.peekBufferPosition += length;
                return true;
            }

            bytesPeeked = this.readFromStream(this.peekBuffer, this.peekBufferPosition, length, bytesPeeked, allowEndOfInput);
        } while (bytesPeeked != -1);

        return false;
    }

    public void advancePeekPosition(int length) throws IOException, InterruptedException {
        this.advancePeekPosition(length, false);
    }

    public void resetPeekPosition() {
        this.peekBufferPosition = 0;
    }

    public long getPeekPosition() {
        return this.position + (long) this.peekBufferPosition;
    }

    public long getPosition() {
        return this.position;
    }

    public long getLength() {
        return this.streamLength;
    }

    private void ensureSpaceForPeek(int length) {
        int requiredLength = this.peekBufferPosition + length;
        if (requiredLength > this.peekBuffer.length) {
            this.peekBuffer = Arrays.copyOf(this.peekBuffer, Math.max(this.peekBuffer.length * 2, requiredLength));
        }

    }

    private int skipFromPeekBuffer(int length) {
        int bytesSkipped = Math.min(this.peekBufferLength, length);
        this.updatePeekBuffer(bytesSkipped);
        return bytesSkipped;
    }

    private int readFromPeekBuffer() {
        if (this.peekBufferLength == 0) {
            return 0;
        } else {
            int bytesRead = this.peekBuffer[0];
            this.updatePeekBuffer(1);
            return bytesRead;
        }
    }

    private int readFromPeekBuffer(byte[] target, int offset, int length) {
        if (this.peekBufferLength == 0) {
            return 0;
        } else {
            int peekBytes = Math.min(this.peekBufferLength, length);
            System.arraycopy(this.peekBuffer, 0, target, offset, peekBytes);
            this.updatePeekBuffer(peekBytes);
            return peekBytes;
        }
    }

    private void updatePeekBuffer(int bytesConsumed) {
        this.peekBufferLength -= bytesConsumed;
        this.peekBufferPosition = 0;
        System.arraycopy(this.peekBuffer, bytesConsumed, this.peekBuffer, 0, this.peekBufferLength);
    }

    private int readFromStream() throws InterruptedException, IOException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else {
            return this.stream.read();
        }
    }

    private int readFromStream(byte[] target, int offset, int length, int bytesAlreadyRead, boolean allowEndOfInput) throws InterruptedException, IOException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else {
            int bytesRead = this.stream.read(target, offset + bytesAlreadyRead, length - bytesAlreadyRead);
            if (bytesRead == -1) {
                if (bytesAlreadyRead == 0 && allowEndOfInput) {
                    return -1;
                } else {
                    throw new EOFException();
                }
            } else {
                return bytesAlreadyRead + bytesRead;
            }
        }
    }

    private void commitBytesRead(int bytesRead) {
        if (bytesRead != -1) {
            this.position += (long) bytesRead;
        }

    }
}

