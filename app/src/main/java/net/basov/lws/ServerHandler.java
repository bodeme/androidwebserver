/*
 * Copyright (C) 2017-2018 Mikhail Basov
 * Copyright (C) 2009-2014 Markus Bode
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static net.basov.lws.Constants.*;

class ServerHandler extends Thread {
    private final Socket toClient;
    private final String documentRoot;
    private final Context context;
    private static Handler msgHandler;
    private DateFormat DF;
    private DateFormat FLDF;
    private Boolean requestHEAD = false;

    public ServerHandler(String d, Context c, Socket s, Handler h) {
        toClient = s;
        documentRoot = d;
        context = c;
        msgHandler = h;
        DF = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        DF.setTimeZone(TimeZone.getTimeZone("GMT"));
        FLDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    }

    public void run() {
        String document = "";
        String[] rangesArray = {};
        requestHEAD = false;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(toClient.getInputStream()));

            // Receive data

            while (true) {
                String s = in.readLine().trim();
                    if (s.equals("")) {
                    break;
                }

                if (s.startsWith("HEAD"))
                    requestHEAD = true;
                if (s.substring(0, 3).equals("GET") || s.substring(0, 4).equals("HEAD")) {
                    int leerstelle = s.indexOf(" HTTP/");
                    document = s.substring(5,leerstelle);
                    document = document.replaceAll("[/]+","/");
                    document = URLDecoder.decode(document, "UTF-8");
                }
                if (s.substring(0,6).equals("Range:")) {
                    rangesArray = s
                            .split("=", 2)[1]
                            .split(",");
                }
            }
        } catch (Exception e) {
            Server.remove(toClient);
            try {
                toClient.close();
            }
            catch (Exception ex){}
        }
        showHtml(document, rangesArray);
    }

    private void send(String text) {
        String header = context.getString(R.string.header,
                context.getString(R.string.rc200),
                text.getBytes().length,
                DF.format(new Date()) + " GMT", // workaround to avoid +00:00
                "text/html"
        );
        try {
            PrintWriter out = new PrintWriter(toClient.getOutputStream(), true);
            out.print(header);
            out.print(text);
            out.flush();
            Server.remove(toClient);
            toClient.close();
        } catch (Exception e) {

        }
    }

    private void showHtml(String document, String[] ranges) {
        Integer rc = 200;
        Long fileSize = 0L;
        String fileModified = "";
        String clientIP = "";
        if(toClient != null
                && toClient.getRemoteSocketAddress() != null
                && toClient.getRemoteSocketAddress().toString() != null
                && toClient.getRemoteSocketAddress().toString().length() > 2
                ) {
            clientIP = toClient.getRemoteSocketAddress().toString().substring(1);
            Integer clientIPColon = clientIP.indexOf(':');
            if (clientIPColon > 0)
                clientIP = clientIP.substring(0, clientIPColon);
        }

        // Standard-Doc
        if (document.equals("")) {
            document = "/";
        }

        // Don't allow directory traversal
        if (document.contains("..")) {
            rc = 403;
        }

        // Search for files in document root
        document = documentRoot + document;
        document = document.replaceAll("[/]+","/");

        try {
            if (!new File(document).exists()) {
                if (document.replace(documentRoot, "").equals("favicon.ico")) {
                    // set fake rc for default favicon.ico
                    rc = -2;
                } else {
                    rc = 404;
                }
            } else if(document.charAt(document.length()-1) == '/') {
                // This is directory
                if (new File(document+"index.html").exists()) {
                    document = document + "index.html";
                } else {
                    send(directoryHTMLindex(document));
                    StartActivity.putToLogScreen(
                            "rc: "
                                    + rc
                                    + ", "
                                    + clientIP
                                    + ", /"
                                    + document.replace(documentRoot, "")
                                    + " (dir. index)",
                            msgHandler
                    );
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            String rcStr;
            String header;
            String contType;
            BufferedOutputStream outStream = new BufferedOutputStream(toClient.getOutputStream());
            BufferedInputStream in;

            if (rc == 200) {
                in = new BufferedInputStream(new FileInputStream(document));
                rcStr = context.getString(R.string.rc200);
                contType = getMIMETypeForDocument(document).get(0);
            } else if (rc == -2) {
                // favicon.ico doesn't exist. Send application icon instead.
                @SuppressLint("ResourceType")
                final AssetFileDescriptor raw = context
                        .getResources()
                        .openRawResourceFd(R.mipmap.lws_ic);
                in = new BufferedInputStream(raw.createInputStream());
                fileSize = (long) in.available();
                // mipmap resource modification time difficult to obtain
                // and has no meaning. Set current date instead.
                fileModified = DF.format(new Date());
                rcStr = context.getString(R.string.rc200);
                contType = getMIMETypeForDocument(document).get(0);
                rc = 200;
            } else {
                String errAsset = "";
                AssetManager am = context.getAssets();
                switch (rc) {
                    case 404:
                        rcStr = context.getString(R.string.rc404);
                        errAsset = "404.html";
                        break;
                    case 403:
                        rcStr = context.getString(R.string.rc403);
                        errAsset = "403.html";
                        break;
                    case 416:
                        errAsset = "416.html";
                        rcStr = context.getString(R.string.rc416);
                        break;
                    default:
                        errAsset = "500.html";
                        rcStr = context.getString(R.string.rc500);
                        break;
                }

                contType = "text/html";
                in = new BufferedInputStream(am.open(errAsset));
                fileSize = (long) in.available();
                fileModified = DF.format(new File("file:///android_asset/"+errAsset).lastModified()) + " GMT"; // workaround to avoid +00:00

            }
            // If fileSize not 0 some error detected and fileSize already set
            // to assets file length
            File documentFile = new File(document);
            if (fileSize == 0L) fileSize = documentFile.length();
            if (fileModified.length() == 0) fileModified = DF.format(documentFile.lastModified())  + " GMT"; // workaround to avoid +00:00
            if(ranges.length == 0 || rc != 200) {
                header = context.getString(R.string.header,
                        rcStr,
                        fileSize,
                        fileModified,
                        contType
                );

                header = normalizeLineEnd(header);
                outStream.write(header.getBytes());
                if (!requestHEAD) {
                    byte[] fileBuffer = new byte[8192];
                    int bytesCount = 0;
                    while ((bytesCount = in.read(fileBuffer)) != -1) {
                        outStream.write(fileBuffer, 0, bytesCount);
                    }
                }
                String headMark = requestHEAD ? "(HEAD)":"";
                StartActivity.putToLogScreen(
                       "rc: "
                                + rc
                                + ", "
                                + clientIP
                                + ", /"
                                + document.replace(documentRoot, "")
                                + headMark,
                        msgHandler
                );
            } else {
                // TODO: range error processing
                // TODO: number conversion error processing
                rc = 206;
                Long partialHeaderLength = 0L;
                PartialRange[] boundaries = new PartialRange[ranges.length];

                for (int i = 0; i < ranges.length; i++) {
                    String strRangeBegin = ranges[i].split("-",2)[0];
                    String strRangeEnd = ranges[i].split("-",2)[1];
                    boundaries[i] = new PartialRange();
                    try {
                        if (strRangeBegin.length() != 0 && strRangeEnd.length() != 0) {
                            boundaries[i].begin = Long.valueOf(strRangeBegin);
                            boundaries[i].end = Long.valueOf(strRangeEnd);
                        } else if (strRangeBegin.length() != 0 && strRangeEnd.length() == 0) {
                            boundaries[i].begin = Long.valueOf(strRangeBegin);
                            boundaries[i].end = fileSize - 1;
                        } else if (strRangeBegin.length() == 0 && strRangeEnd.length() != 0) {
                            boundaries[i].begin = fileSize - Long.valueOf(strRangeEnd);
                            boundaries[i].end = fileSize - 1;
                        }
                    } catch (NumberFormatException e ) {
                        e.printStackTrace();
                        handleError416(outStream);
                        return;
                    }
                    boundaries[i].size = boundaries[i].end - boundaries[i].begin + 1;
                    if (boundaries[i].size <= 0
                            || boundaries[i].end > fileSize
                            || boundaries[i].begin > fileSize) {
                        handleError416(outStream);
                        return;
                    }
                    boundaries[i].header = "";
                    if (i != 0) boundaries[i].header += "\n";
                    boundaries[i].header += context.getString(R.string.range_header,
                            context.getString(R.string.boundary_string),
                            contType,
                            boundaries[i].begin, // begin
                            boundaries[i].end, // end
                            fileSize  // length
                    );
                    boundaries[i].header = normalizeLineEnd(boundaries[i].header);

                    partialHeaderLength += boundaries[i].size + boundaries[i].header.length();
                }
                if (ranges.length > 1) partialHeaderLength += context.getString(R.string.boundary_string).length() + 2 + 4; // I don't know why + 4

                String headMark = requestHEAD ? "(HEAD)":"";
                StartActivity.putToLogScreen(
                                "rc: "
                                + rc
                                + ", "
                                + clientIP
                                + ", /"
                                + document.replace(documentRoot, "")
                                + ", Range: "
                                + Arrays.toString(ranges)
                                + headMark,
                        msgHandler
                );

                header = context.getString(R.string.header_partial,
                        context.getString(R.string.rc206),
                        ranges.length > 1 ? "" : "\nContent-Range: bytes " + boundaries[0].begin+"-"+boundaries[0].end+"/" + fileSize,
                        ranges.length > 1 ? partialHeaderLength : boundaries[0].size,
                        ranges.length > 1 ? "multipart/byteranges; boundary=" + context.getString(R.string.boundary_string) : contType
                );
                header = normalizeLineEnd(header);
                outStream.write(header.getBytes());

                if (!requestHEAD) {
                    for (PartialRange b : boundaries) {
                        if (boundaries.length > 1) {
                            outStream.write(b.header.getBytes());
                        }
                        byte[] fileBuffer = new byte[8192];
                        int bytesCount = 0;
                        Long currentPosition = b.begin;
                        in = new BufferedInputStream(new FileInputStream(document));
                        in.skip(currentPosition);
                        while ((bytesCount = in.read(fileBuffer)) != -1) {
                            if (currentPosition + bytesCount <= b.end)
                                currentPosition += bytesCount;
                            else {
                                outStream.write(fileBuffer, 0, (int) (b.end - currentPosition + 1));
                                break;
                            }
                            outStream.write(fileBuffer, 0, bytesCount);
                        }
                    }
                    if (boundaries.length > 1)
                        outStream.write(("\r\n--" + context.getString(R.string.boundary_string) + "\r\n").getBytes());
                }

            }
            outStream.flush();

            Server.remove(toClient);
            toClient.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.LOG_TAG, "showHtml() very complex and need to be written simpler ... ");
        }
    }

    private String directoryHTMLindex(String dir) {     
        StringBuilder html = new StringBuilder(context.getString(
                R.string.dir_list_top_html,
                dir.replace(documentRoot, ""),
                dir.replace(documentRoot, ""),
                dir.equals(documentRoot) ? "" : context.getString(R.string.dir_list_parent_dir)
        ));
        
        ArrayList <FileInfo> dirs = new ArrayList<FileInfo>();
        ArrayList <FileInfo> files = new ArrayList<FileInfo>();

        for (File i : new File(dir).listFiles()) {
            if (i.isDirectory()) {
                dirs.add(new FileInfo());
                dirs.get(dirs.size() - 1).name = i.getName();
                dirs.get(dirs.size() - 1).date = FLDF.format(i.lastModified());
            } else if (i.isFile()) {
                files.add(new FileInfo());
                files.get(files.size() - 1).name = i.getName();
                files.get(files.size() - 1).size = i.length();
                files.get(files.size() - 1).date = FLDF.format(i.lastModified());
            }
        }
        
        Comparator<FileInfo> fileNameCmp =  new Comparator<FileInfo>(){
            @Override
            public int compare(FileInfo f1, FileInfo f2) {
                return f1.name.compareToIgnoreCase(f2.name);
            }
        };
        Collections.sort(dirs, fileNameCmp);
        Collections.sort(files, fileNameCmp);
        
        for (FileInfo d : dirs) {
            html.append(context.getString(
                    R.string.dir_list_item,
                    "folder",
                    "folder",
                    fileName2URL(d.name) + "/",
                    d.name,
                    d.date,
                    0,
                    "-"
            ));
        }
        for (FileInfo f : files) {
            html.append(context.getString(
                    R.string.dir_list_item,
                    "file",
                    getMIMETypeForDocument(f.name).get(1),
                    fileName2URL(f.name),
                    f.name,
                    f.date,
                    f.size,
                    bytesToKMGT(f.size)
            ));
        }
        
        html.append(context.getString(R.string.dir_list_bottom_html));
        
        return html.toString();
    }

    private ArrayList<String> getMIMETypeForDocument(String document) {
        final HashMap<String,ArrayList<String>> MIME = new HashMap<String, ArrayList<String>>(){
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
        String fileExt = document.substring(
                document.lastIndexOf(".")+1
        ).toLowerCase();
        if (MIME.containsKey(fileExt))
            return MIME.get(fileExt);
        else
            return new ArrayList<String>(Arrays.asList("application/octet-stream", "file"));
    }
    
    private String fileName2URL(String fn) {
        String ref = "";
        try {
            ref = URLEncoder.encode(fn, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {}
        return ref;
    }

    private void handleError416(BufferedOutputStream outStream) {
        try {
            AssetManager am = context.getAssets();
            BufferedInputStream in = new BufferedInputStream(am.open("416.html"));

            String header = context.getString(R.string.header,
                    context.getString(R.string.rc500),
                    (long) in.available(),
                    DF.format(new Date()) + " GMT", // workaround to avoid +00:00
                    "text/html"
            );
            outStream.write(header.getBytes());

            byte[] fileBuffer = new byte[8192];
            int bytesCount = 0;
            while ((bytesCount = in.read(fileBuffer)) != -1) {
                outStream.write(fileBuffer, 0, bytesCount);
            }
            outStream.flush();
            Server.remove(toClient);
            toClient.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private String bytesToKMGT(Long size) {
        String ret;
        if (size <= 1024)
            ret = String.format("%d b", size);
        else if (size > 1024 && size <= 1024*1024)
            ret = String.format("%.2f Kb", (float) size/1024.0);
        else if (size > 1024.0*1024.0 && size <= 1024.0*1024.0*1024.0)
            ret = String.format("%.2f Mb", (float) size/(1024*1024));
        else if (size > 1024.0*1024.0*1024.0 && size <= 1024.0*1024.0*1024.0*1024.0)
            ret = String.format("%.2f Gb", (float) size/(1024.0*1024.0*1024.0));
        else // Yes. I am optimist :)
            ret = String.format("%.2f Tb", (float) size/(1024.0*1024.0*1024.0*1024.0));
        return ret;
    }

    private String normalizeLineEnd (String src) {
        return src.replaceAll("\\n|\\r|\\n\\r", "\r\n");
    }

    class PartialRange {
        Long begin;
        Long end;
        Long size;
        String header;
    }

    class FileInfo {
        String name;
        Long size;
        String date;
    }
}
