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

import java.util.HashMap;
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
    public static final MimeType MIME_OCTAL = new MimeType("application/octet-stream", "file");
    public static final Map<String, MimeType> MIME = new HashMap<String, MimeType>(30, 1.0F) {
        {
            put("html", new MimeType("text/html; charset=utf-8", "web"));
            put("css", new MimeType("text/css; charset=utf-8", "code"));
            put("js", new MimeType("text/javascript; charset=utf-8", "code"));
            put("txt", new MimeType("text/plain; charset=utf-8", "file-text"));
            put("md", new MimeType("text/markdown; charset=utf-8", "file-text"));
            put("gif", new MimeType("image/gif", "image"));
            put("png", new MimeType("image/png", "image"));
            put("jpg", new MimeType("image/jpeg", "image"));
            put("bmp", new MimeType("image/bmp", "image"));
            put("svg", new MimeType("image/svg+xml", "image"));
            put("ico", new MimeType("image/x-icon", "image"));
            put("zip", new MimeType("application/zip", "package"));
            put("gz", new MimeType("application/gzip", "package"));
            put("tgz", new MimeType("application/gzip", "package"));
            put("pdf", new MimeType("application/pdf", "file-text"));
            put("mp4", new MimeType("video/mp4", "video"));
            put("avi", new MimeType("video/x-msvideo", "video"));
            put("3gp", new MimeType("video/3gpp", "video"));
            put("mp3", new MimeType("audio/mpeg", "music"));
            put("ogg", new MimeType("audio/ogg", "music"));
            put("wav", new MimeType("audio/wav", "music"));
            put("flac", new MimeType("audio/flac", "music"));
            put("java", new MimeType("text/plain", "code"));
            put(".c", new MimeType("text/plain", "code"));
            put(".cpp", new MimeType("text/plain", "code"));
            put(".sh", new MimeType("text/plain", "code"));
            put(".py", new MimeType("text/plain", "code"));
        }
    };
    
    static class MimeType {
        final String contentType;
        final String kind;

        public MimeType(String contentType, String kind) {
            this.contentType = contentType;
            this.kind = kind;
        }
    } 
}
