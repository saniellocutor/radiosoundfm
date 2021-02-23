package com.streaminghost.radiosoundfm.services.parser;

import android.annotation.SuppressLint;

import com.streaminghost.radiosoundfm.utilities.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

public class UrlParser {

    @SuppressLint("DefaultLocale") // Does not matter, no text but URL
    public static String getUrl(String url) {
        String mUrl = url.toUpperCase();
        if (mUrl.endsWith(".FLAC")) {
            return url;
        } else if (mUrl.endsWith(".MP3")) {
            return url;
        } else if (mUrl.endsWith(".WAV")) {
            return url;
        } else if (mUrl.endsWith(".M4A")) {
            return url;
        } else if (mUrl.endsWith(".PLS")) {
            return url;

            /**
             * Since our new player can handle PLS directly, no need to parse.
             PLSParser mPls = new PLSParser();
             LinkedList<String> urls = mPls.getRawUrl(url);
             if(urls.size()>0)
             {
             return urls.get(0);
             }**/
        } else if (mUrl.endsWith(".M3U")) {
            M3UParser mM3u = new M3UParser();
            LinkedList<String> urls = mM3u.getRawUrl(url);
            if ((urls.size() > 0)) {
                return urls.get(0);
            }
        } else if (mUrl.endsWith(".ASX"))//|| url.toUpperCase().endsWith("WMA_UK_CONCRETE"))
        {
            ASXParser mAsx = new ASXParser();
            LinkedList<String> urls = mAsx.getRawUrl(url);
            if ((urls.size() > 0)) {
                return urls.get(0);
            }
        } else {
            URLConnection conn = getConnection(url);
            if (conn != null) {
                String mContentDisposition = conn.getHeaderField("Content-Disposition");

                Log.v("INFO", "Requesting: " + url + " Headers: " + conn.getHeaderFields());

                String mContentType = conn.getContentType();
                if (mContentType != null) {
                    mContentType = mContentType.toUpperCase();
                }
                if (mContentDisposition != null && mContentDisposition.toUpperCase().endsWith("M3U")) {
                    M3UParser m3u = new M3UParser();
                    LinkedList<String> urls = m3u.getRawUrl(conn);
                    if (urls.size() > 0) {
                        return urls.getFirst();
                    }
                } else if (mContentType != null && mContentType.contains("AUDIO/X-SCPLS")) {
                    return url;
                    /** Since our new player can handle PLS directly, no need to parse.

                     PLSParser pls = new PLSParser();
                     LinkedList<String> urls = pls.getRawUrl(conn);
                     if(urls.size()> 0)
                     {
                     return urls.getFirst();
                     }*/
                } else if (mContentType != null && mContentType.contains("VIDEO/X-MS-ASF")) {
                    ASXParser asx = new ASXParser();
                    LinkedList<String> urls = asx.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                    PLSParser pls = new PLSParser();
                    urls = pls.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                } else if (mContentType != null && mContentType.contains("AUDIO/MPEG")) {
                    return url;
                } else if (mContentType != null && mContentType.contains("AUDIO/X-MPEGURL")) {
                    M3UParser m3u = new M3UParser();
                    LinkedList<String> urls = m3u.getRawUrl(url);
                    if ((urls.size() > 0)) {
                        return urls.get(0);
                    }
                } else {
                    //not found
                }
            }
        }
        return url;
    }

    private static URLConnection getConnection(String url) {
        URLConnection mUrl;
        try {
            mUrl = new URL(url).openConnection();
            return mUrl;
        } catch (MalformedURLException e) {

        } catch (IOException e) {

        }
        return null;
    }

}
