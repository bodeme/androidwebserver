/*
 * Copyright (C) 2017 Mikhail Basov
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
package net.basov.web;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

class ServerHandler extends Thread {
  private BufferedReader in;
  private PrintWriter out;
  private Socket toClient;
  private String documentRoot;
  private Context context;
  
  public ServerHandler(String d, Context c, Socket s) {
    toClient = s;
    documentRoot = d;
    context = c;
  }

  public void run() {
	String dokument = "";

    try {
      in = new BufferedReader(new InputStreamReader(toClient.getInputStream()));

      // Receive data
      while (true) {
        String s = in.readLine().trim();

        if (s.equals("")) {
          break;
        }
        
        if (s.substring(0, 3).equals("GET")) {
        	int leerstelle = s.indexOf(" HTTP/");
        	dokument = s.substring(5,leerstelle);
        	dokument = dokument.replaceAll("[/]+","/");
        }
      }
    } catch (Exception e) {
    	Server.remove(toClient);
    	try {
    		toClient.close();
		}
    	catch (Exception ex){}
    }
    
   	showHtml(dokument);
  }
  
  private void send(String text) {
      String header = context.getString(R.string.header,
              context.getString(R.string.rc200),
              text.length(),
              "text/html"
      );
	  try {
	      out = new PrintWriter(toClient.getOutputStream(), true);
	      out.print(header);
		  out.print(text);
		  out.flush();
		  Server.remove(toClient);
		  toClient.close();
	  } catch (Exception e) {

	  }
  }
  
  private void showHtml(String dokument) {
    Integer rc = 200;

    // Standard-Doc
	if (dokument.equals("")) {
		dokument = "index.html";
	}
	
	// Don't allow directory traversal
	if (dokument.indexOf("..") != -1) {
	    rc = 403;
	}
	
	// Search for files in docroot
	dokument = documentRoot + dokument;
	Log.d("Webserver", "Got " + dokument);
	dokument = dokument.replaceAll("[/]+","/");
	
	if(dokument.charAt(dokument.length()-1) == '/') {
		rc = 404;
	}
	
	try {
    	File f = new File(dokument);
        if (!f.exists()) {
            rc = 404;
        }
    }
    catch (Exception e) {}

    Log.d("lWS", "Serving " + dokument);

    try {
      String rcStr;
      File f = new File(dokument);
      ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
      BufferedOutputStream outStream = new BufferedOutputStream(toClient.getOutputStream());

      if (rc == 200) {
          Log.d("lWS", "Send " + dokument + ", rc:" + rc);

          BufferedInputStream in = new BufferedInputStream(new FileInputStream(dokument));
          rcStr = context.getString(R.string.rc200);

    	  byte[] buf = new byte[4096];
    	  int count = 0;
    	  while ((count = in.read(buf)) != -1){
    		  tempOut.write(buf, 0, count);
    	  }

    	  tempOut.flush();

          String header = context.getString(R.string.header,
                  rcStr,
                  tempOut.size(),
                  getMIMETypeForDocument(dokument)
          );
    	  outStream.write(header.getBytes());
    	  outStream.write(tempOut.toByteArray());
    	  outStream.flush();
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
              default:
                  errAsset = "500.html";
                  rcStr = context.getString(R.string.rc500);
                  break;
          }
          BufferedInputStream in = new BufferedInputStream(am.open(errAsset));

          byte[] buf = new byte[4096];
          int count = 0;
          while ((count = in.read(buf)) != -1){
              tempOut.write(buf, 0, count);
          }
          tempOut.flush();

          String header = context.getString(R.string.header,
                  rcStr,
                  tempOut.size(),
                  "text/html"
          );

          outStream.write(header.getBytes());
          outStream.write(tempOut.toByteArray());
          outStream.flush();
      }

      Server.remove(toClient);
      toClient.close();
    } catch (Exception e) {
    	
    }
  }

  private String getMIMETypeForDocument(String document) {
      final HashMap<String,String> MIME = new HashMap<String, String>(){
          {
              put("html","text/html");
              put("css", "text/css");
              put("js", "text/javascript");
              put("gif", "image/gif");
              put("png", "image/png");
              put("jpg","image/jpeg");
              put("bmp","image/bmp");
          }
      };
      return MIME.get(document.substring(document.lastIndexOf(".")+1));
  }
}
