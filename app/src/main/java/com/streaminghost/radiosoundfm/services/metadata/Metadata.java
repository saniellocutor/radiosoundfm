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

public class Metadata {
    private final String artist;
    private final String song;
    private final String show;
    private final String channels;
    private final String bitrate;
    private final String station;
    private final String genre;
    private final String url;

    public Metadata(String artist, String song, String show, String channels, String bitrate, String station, String genre, String url) {
        this.artist = artist;
        this.song = song;
        this.show = show;
        this.channels = channels;
        this.bitrate = bitrate;
        this.station = station;
        this.genre = genre;
        this.url = url;
    }

    public String getArtist() {
        return artist;
    }

    public String getSong() {
        return song;
    }

    public String getShow() {
        return show;
    }

    public String getChannels() {
        return channels;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getStation() {
        return station;
    }

    public String getGenre() {
        return genre;
    }

    public String getUrl() {
        return url;
    }
}