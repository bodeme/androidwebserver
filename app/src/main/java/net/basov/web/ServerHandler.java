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

import android.content.Context;
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
	  String header = getHeaderBase();
	  header = header.replace("%code%", "200 ok");	    	  
	  header = header.replace("%length%", "" + text.length());	  
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
    // Standard-Doc
	if (dokument.equals("")) {
		dokument = "index.html";
	}
	
	// Don't allow directory traversal
	if (dokument.indexOf("..") != -1) {
		dokument = "403.html";
	}
	
	// Search for files in docroot
	dokument = documentRoot + dokument;
	Log.d("Webserver", "Got " + dokument);
	dokument = dokument.replaceAll("[/]+","/");
	
	if(dokument.charAt(dokument.length()-1) == '/') {
		dokument = documentRoot + "404.html";
	}
	
	String header = getHeaderBase();
	header = header.replace("%code%", "403 Forbidden");

	try {
    	File f = new File(dokument);
        if (!f.exists()) {
        	header = getHeaderBase();
        	header = header.replace("%code%", "404 File not found");
        	dokument = "404.html";
        }
    }
    catch (Exception e) {}

    if (!dokument.equals(documentRoot + "403.html")) {
    	header = getHeaderBase().replace("%code%", "200 OK");
    }
	
    Log.d("Webserver", "Serving " + dokument);
    
    try {
      File f = new File(dokument);
      if (f.exists()) {
    	  BufferedInputStream in = new BufferedInputStream(new FileInputStream(dokument));
    	  BufferedOutputStream out = new BufferedOutputStream(toClient.getOutputStream());
    	  ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
    	  
    	  byte[] buf = new byte[4096];
    	  int count = 0;
    	  while ((count = in.read(buf)) != -1){
    		  tempOut.write(buf, 0, count);
    	  }

    	  tempOut.flush();
    	  header = header.replace("%length%", ""+tempOut.size());

    	  out.write(header.getBytes());
    	  out.write(tempOut.toByteArray());
    	  out.flush();
      } else {
          // Send HTML-File (Ascii, not as a stream)
    	  header = getHeaderBase();
    	  header = header.replace("%code%", "404 File not found");	    	  
    	  header = header.replace("%length%", ""+"404 - File not Found".length());	    	  
          out = new PrintWriter(toClient.getOutputStream(), true);
          out.print(header);
    	  out.print("404 - File not Found");
    	  out.flush();
      }

      Server.remove(toClient);
      toClient.close();
    } catch (Exception e) {
    	
    }
  }
  
  private String getHeaderBase() {
	  return  "HTTP/1.1 %code%\n"+
		"Server: AndroidWebserver/1.0\n"+
		"Content-Length: %length%\n"+
		"Connection: close\n"+
		"Content-Type: text/html; charset=utf-8\n\n";
  }
}
