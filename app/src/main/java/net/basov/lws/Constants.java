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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final Map<String, List<String>> MIME = new HashMap<String, List<String>>(30, 1.0F) {
        {
            put("html", Arrays.asList("text/html; charset=utf-8", "web"));
            put("css", Arrays.asList("text/css; charset=utf-8", "code"));
            put("js", Arrays.asList("text/javascript; charset=utf-8", "code"));
            put("txt", Arrays.asList("text/plain; charset=utf-8", "file-text"));
            put("md", Arrays.asList("text/markdown; charset=utf-8", "file-text"));
            put("gif", Arrays.asList("image/gif", "image"));
            put("png", Arrays.asList("image/png", "image"));
            put("jpg", Arrays.asList("image/jpeg", "image"));
            put("bmp", Arrays.asList("image/bmp", "image"));
            put("svg", Arrays.asList("image/svg+xml", "image"));
            put("ico", Arrays.asList("image/x-icon", "image"));
            put("zip", Arrays.asList("application/zip", "package"));
            put("gz", Arrays.asList("application/gzip", "package"));
            put("tgz", Arrays.asList("application/gzip", "package"));
            put("pdf", Arrays.asList("application/pdf", "file-text"));
            put("mp4", Arrays.asList("video/mp4", "video"));
            put("avi", Arrays.asList("video/x-msvideo", "video"));
            put("3gp", Arrays.asList("video/3gpp", "video"));
            put("mp3", Arrays.asList("audio/mpeg", "music"));
            put("ogg", Arrays.asList("audio/ogg", "music"));
            put("wav", Arrays.asList("audio/wav", "music"));
            put("flac", Arrays.asList("audio/flac", "music"));
            put("java", Arrays.asList("text/plain", "code"));
            put(".c", Arrays.asList("text/plain", "code"));
            put(".cpp", Arrays.asList("text/plain", "code"));
            put(".sh", Arrays.asList("text/plain", "code"));
            put(".py", Arrays.asList("text/plain", "code"));
        }
    };
}
