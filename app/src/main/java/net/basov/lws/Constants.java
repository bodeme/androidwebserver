/*
 * Copyright (C) 2017-2019 Mikhail Basov
 *
 * Licensed under the GNU General Public License v3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package net.basov.lws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by mvb on 12/18/17.
 */

final class Constants {
    public static final String LOG_TAG = "lWS";
    public static final String ACTION_STOP = "net.basov.lws.stop_service";
    public static final int NOTIFICATION_ID = 690927;
    public static final int DIRECTORY_REQUEST = 170;
    public static final int MAIN_SCREEN_REQUEST = 171;
    public static final int STOP_SERVICE_REQUEST = 172;
    public static final int GRANT_WRITE_EXTERNAL_STORAGE = 173;
    public static final HashMap<String, ArrayList<String>> MIME = new HashMap<String, ArrayList<String>>() {
        {
            put("html", new ArrayList<String>(Arrays.asList("text/html; charset=utf-8", "web")));
            put("css", new ArrayList<String>(Arrays.asList("text/css; charset=utf-8", "code")));
            put("js", new ArrayList<String>(Arrays.asList("text/javascript; charset=utf-8", "code")));
            put("txt", new ArrayList<String>(Arrays.asList("text/plain; charset=utf-8", "file-text")));
            put("md", new ArrayList<String>(Arrays.asList("text/markdown; charset=utf-8", "file-text")));
            put("gif", new ArrayList<String>(Arrays.asList("image/gif", "image")));
            put("png", new ArrayList<String>(Arrays.asList("image/png", "image")));
            put("jpg", new ArrayList<String>(Arrays.asList("image/jpeg", "image")));
            put("bmp", new ArrayList<String>(Arrays.asList("image/bmp", "image")));
            put("svg", new ArrayList<String>(Arrays.asList("image/svg+xml", "image")));
            put("ico", new ArrayList<String>(Arrays.asList("image/x-icon", "image")));
            put("zip", new ArrayList<String>(Arrays.asList("application/zip", "package")));
            put("gz", new ArrayList<String>(Arrays.asList("application/gzip", "package")));
            put("tgz", new ArrayList<String>(Arrays.asList("application/gzip", "package")));
            put("pdf", new ArrayList<String>(Arrays.asList("application/pdf", "file-text")));
            put("mp4", new ArrayList<String>(Arrays.asList("video/mp4", "video")));
            put("avi", new ArrayList<String>(Arrays.asList("video/x-msvideo", "video")));
            put("3gp", new ArrayList<String>(Arrays.asList("video/3gpp", "video")));
            put("mp3", new ArrayList<String>(Arrays.asList("audio/mpeg", "music")));
            put("ogg", new ArrayList<String>(Arrays.asList("audio/ogg", "music")));
            put("wav", new ArrayList<String>(Arrays.asList("audio/wav", "music")));
            put("flac", new ArrayList<String>(Arrays.asList("audio/flac", "music")));
            put("java", new ArrayList<String>(Arrays.asList("text/plain", "code")));
            put(".c", new ArrayList<String>(Arrays.asList("text/plain", "code")));
            put(".cpp", new ArrayList<String>(Arrays.asList("text/plain", "code")));
            put(".sh", new ArrayList<String>(Arrays.asList("text/plain", "code")));
            put(".py", new ArrayList<String>(Arrays.asList("text/plain", "code")));
        }
    };
}
