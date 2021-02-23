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

import androidx.annotation.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IcyInputStream extends FilterInputStream {
    private static final String TAG = IcyInputStream.class.getName();
    private final String characterEncoding;
    private final MetadataListener metadataListener;
    private final int interval;
    private int remaining;


    /**
     * Creates a new input stream.
     * @param in the underlying input stream
     * @param interval the interval of metadata frame is repeating (in bytes)
     */
    private IcyInputStream(InputStream in, int interval, MetadataListener metadataListener) {
        this(in, interval, null, metadataListener);
    }

    /**
     * Creates a new input stream.
     * @param in the underlying input stream
     * @param interval the interval of metadata frame is repeating (in bytes)
     * @param characterEncoding the encoding used for metadata strings - may be null = default is UTF-8
     */
    public IcyInputStream(InputStream in, int interval, String characterEncoding, MetadataListener metadataListener) {
        super(in);
        this.interval = interval;
        this.characterEncoding = characterEncoding != null ? characterEncoding : "UTF-8";
        this.metadataListener = metadataListener;
        this.remaining = interval;
    }

    @Override
    public int read() throws IOException {
        int ret = super.read();

        if (--remaining == 0) {
            getMetadata();
        }

        return ret;
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int len ) throws IOException {
        int ret = super.in.read( buffer, offset, remaining < len ? remaining : len );

        if (remaining == ret) {
            getMetadata();
        } else {
            remaining -= ret;
        }

        return ret;
    }

    /**
     * Tries to read all bytes into the target buffer.
     * @param size the requested size
     * @return the number of really bytes read; if less than requested, then eof detected
     */
    private int readFully(byte[] buffer, int offset, int size) throws IOException {
        int n;
        int oo = offset;

        while (size > 0 && (n = in.read( buffer, offset, size )) != -1) {
            offset += n;
            size -= n;
        }

        return offset - oo;
    }

    private void getMetadata() throws IOException {
        remaining = interval;

        int size = super.in.read();

        // either no metadata or eof:
        if (size < 1) return;

        size *= 16;

        byte[] buffer = new byte[ size ];

        size = readFully(buffer, 0, size );

        // find the string end:
        for (int i=0; i < size; i++) {
            if (buffer[i] == 0) {
                size = i;
                break;
            }
        }

        String s;

        try {
            s = new String(buffer, 0, size, characterEncoding );
        }
        catch (Exception e) {
            Log.e(TAG, "Cannot convert bytes to String" );
            return;
        }

        Log.d(TAG, "Metadata string: " + s );

        parseMetadata(s);
    }


    /**
     * Parses the metadata
     * @param data the metadata string like: StreamTitle='...';StreamUrl='...';
     */
    private void parseMetadata(String data) {
        Matcher match = Pattern.compile("StreamTitle='([^;]*)'").matcher(data.trim());
        if (match.find())
        {
            // Presume artist/title is separated by " - ".
            String[] metadata = match.group(1).split(" - ");
            switch (metadata.length) {
                case 3:
                    metadataReceived(metadata[1], metadata[2], metadata[0]);
                    break;
                case 2:
                    metadataReceived(metadata[0], metadata[1], null);
                    break;
                case 1:
                    metadataReceived(null, null, metadata[0]);
            }
        }
    }

    private void metadataReceived(String artist, String song, String show) {
        Log.i(TAG, "Metadata received: ");
        Log.i(TAG, "Show: " + show);
        Log.i(TAG, "Artist: " + artist);
        Log.i(TAG, "Song: " + song);

        if (this.metadataListener != null) {
            this.metadataListener.onMetadataReceived(artist, song, show);
        }
    }
}

