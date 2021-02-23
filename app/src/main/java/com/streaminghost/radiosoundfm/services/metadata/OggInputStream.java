/**
 * Copyright 2016 Mattias Karlsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streaminghost.radiosoundfm.services.metadata;

import android.util.Log;

import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

final class OggInputStream extends PeekInputStream { // TODO: Use PushbackInputStream instead of Peek?
    private static final String TAG = OggInputStream.class.getName();

    private final PacketInfoHolder holder = new PacketInfoHolder();
    private final IdHeader idHeader = new IdHeader();
    private final CommentHeader commentHeader = new CommentHeader();
    private final PageHeader pageHeader = new PageHeader();

    private final ParsableByteArray packetArray = new ParsableByteArray(new byte['︁'], 0);
    private final ParsableByteArray headerArray = new ParsableByteArray(282);
    private final MetadataListener listener;


    public OggInputStream(InputStream in, MetadataListener listener) {
        super(in);
        this.listener = listener;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        try {
            if(peekPacket(this, this.packetArray, this.headerArray, this.pageHeader, this.holder)) {
                unpackIdHeader(this.packetArray, this.idHeader);
                unpackCommentHeader(this.packetArray, this.commentHeader, this.listener);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.read(target, offset, length);
    }

    private static boolean peekPacket(PeekInputStream stream, ParsableByteArray packetArray, ParsableByteArray headerArray, PageHeader header, PacketInfoHolder holder) throws IOException, InterruptedException {
        int segmentIndex;
        int currentSegmentIndex = -1;
        packetArray.reset();
        for(boolean packetComplete = false; !packetComplete; currentSegmentIndex = segmentIndex == header.pageSegmentCount?-1:segmentIndex) {
            if(currentSegmentIndex < 0) {
                if(!unpackPageHeader(stream, headerArray, header)) {
                    return false;
                }

                segmentIndex = 0;
                if((header.type & 1) == 1 && packetArray.limit() == 0) {
                    calculatePacketSize(header, segmentIndex, holder);
                    segmentIndex += holder.segmentCount;
                }

                currentSegmentIndex = segmentIndex;
            }

            calculatePacketSize(header, currentSegmentIndex, holder);
            segmentIndex = currentSegmentIndex + holder.segmentCount;
            if(holder.size > 0) {
                stream.peekFully(packetArray.data, packetArray.limit(), holder.size);
                packetArray.setLimit(packetArray.limit() + holder.size);
                packetComplete = header.laces[segmentIndex - 1] != 255;
            }
        }

        return true;
    }

    private static void calculatePacketSize(PageHeader header, int startSegmentIndex, PacketInfoHolder holder) {
        holder.segmentCount = 0;
        holder.size = 0;

        int segmentLength;
        while(startSegmentIndex + holder.segmentCount < header.pageSegmentCount) {
             segmentLength = header.laces[startSegmentIndex + holder.segmentCount++];
            holder.size += segmentLength;
            if(segmentLength != 255) {
                break;
            }
        }

    }

    private static boolean unpackPageHeader(PeekInputStream stream, ParsableByteArray headerArray, PageHeader header) throws IOException, InterruptedException {
        headerArray.reset();
        header.reset();
        if(stream.peekFully(headerArray.data, 0, 27, true)) {
            if(headerArray.readUnsignedByte() == 79 && headerArray.readUnsignedByte() == 103 && headerArray.readUnsignedByte() == 103 && headerArray.readUnsignedByte() == 83) {
                header.revision = headerArray.readUnsignedByte();
                if(header.revision != 0) {
                    return false;
                } else {
                    header.type = headerArray.readUnsignedByte();
                    header.granulePosition = headerArray.readLittleEndianLong();
                    header.streamSerialNumber = headerArray.readLittleEndianUnsignedInt();
                    header.pageSequenceNumber = headerArray.readLittleEndianUnsignedInt();
                    header.pageChecksum = headerArray.readLittleEndianUnsignedInt();
                    header.pageSegmentCount = headerArray.readUnsignedByte();
                    headerArray.reset();
                    header.headerSize = 27 + header.pageSegmentCount;
                    stream.peekFully(headerArray.data, 0, header.pageSegmentCount);

                    for(int i = 0; i < header.pageSegmentCount; ++i) {
                        header.laces[i] = headerArray.readUnsignedByte();
                        header.bodySize += header.laces[i];
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static void unpackIdHeader(ParsableByteArray scratch, IdHeader header) {
        scratch.reset();
        if(scratch.readUnsignedByte() == 1) {
            if (scratch.readUnsignedByte() == 118 && scratch.readUnsignedByte() == 111 && scratch.readUnsignedByte() == 114 && scratch.readUnsignedByte() == 98 && scratch.readUnsignedByte() == 105 && scratch.readUnsignedByte() == 115) {
                header.reset();
                header.version = scratch.readLittleEndianUnsignedInt();
                header.audioChannels = scratch.readUnsignedByte();
                header.audioSampleRate = scratch.readLittleEndianUnsignedInt();
                header.bitRateMaximum = scratch.readLittleEndianInt();
                header.bitRateNominal = scratch.readLittleEndianInt();
                header.bitRateMinimum = scratch.readLittleEndianInt();

                int blockSize = scratch.readUnsignedByte();
                header.blockSize0 = (int) Math.pow(2, (blockSize & 15));
                header.blockSize1 = (int) Math.pow(2, (blockSize >> 4));
            }
        }
    }

    private static void unpackCommentHeader(ParsableByteArray scratch, CommentHeader header, MetadataListener listener) {
        scratch.reset();
        if (scratch.readUnsignedByte() == 3) {
            if (scratch.readUnsignedByte() == 118 && scratch.readUnsignedByte() == 111 && scratch.readUnsignedByte() == 114 && scratch.readUnsignedByte() == 98 && scratch.readUnsignedByte() == 105 && scratch.readUnsignedByte() == 115) {
                header.reset();
                int vendorLength = (int) scratch.readLittleEndianUnsignedInt();
                int length = 7 + 4;
                header.vendor = scratch.readString(vendorLength);
                length += header.vendor.length();
                long commentListLen = scratch.readLittleEndianUnsignedInt();
                length += 4;

                int len;
                String comment;
                for (int i = 0; (long) i < commentListLen; ++i) {
                    len = (int) scratch.readLittleEndianUnsignedInt();
                    length += 4;
                    comment = scratch.readString(len);
                    unPackComment(comment, header.comments);
                    length += comment.length();
                }
                header.length = length;
                metadataReceived(header.comments.get("ARTIST"), header.comments.get("TITLE"), listener);
            }
        }
    }

    private static void unPackComment(String comment, HashMap<String, String> commentContainer) {
        if (comment.contains("=")) {
            String[] kv = comment.split("=");
            if (kv.length == 2) {
                commentContainer.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                commentContainer.put(kv[0], "");
            }
        }
    }

    private static void metadataReceived(String artist, String song, MetadataListener listener) {
        Log.i(TAG, "Metadata received: ");
        Log.i(TAG, "Artist: " + artist);
        Log.i(TAG, "Song: " + song);
        if (listener != null) {
            listener.onMetadataReceived(artist, song, "");
        }
    }

    private class PageHeader {
        public int revision;
        public int type;
        public long granulePosition;
        public long streamSerialNumber;
        public long pageSequenceNumber;
        public long pageChecksum;
        public int pageSegmentCount;
        public int headerSize;
        public int bodySize;
        public final int[] laces = new int[255];

        private PageHeader() {
        }

        public void reset() {
            this.revision = 0;
            this.type = 0;
            this.granulePosition = 0L;
            this.streamSerialNumber = 0L;
            this.pageSequenceNumber = 0L;
            this.pageChecksum = 0L;
            this.pageSegmentCount = 0;
            this.headerSize = 0;
            this.bodySize = 0;
        }
    }

    private class IdHeader {
        public long version;
        public int audioChannels;
        public long audioSampleRate;
        public int bitRateMaximum;
        public int bitRateNominal;
        public int bitRateMinimum;
        public int blockSize0;
        public int blockSize1;

        private IdHeader() {}

        public void reset() {
            this.audioChannels = 0;
            this.audioSampleRate = 0;
            this.bitRateMaximum = 0;
            this.bitRateNominal = 0;
            this.bitRateMinimum = 0;
            this.blockSize0 = 0;
            this.blockSize1 = 0;
        }
    }

    private class CommentHeader {
        public String vendor;
        public final HashMap<String, String> comments;
        public int length;

        public CommentHeader() {
            this.comments = new HashMap<>();
        }

        public void reset() {
            this.vendor = "";
            this.comments.clear();
            this.length = 0;
        }
    }

    private class PacketInfoHolder {
        public int size;
        public int segmentCount;

        public PacketInfoHolder() {
        }
    }
}


