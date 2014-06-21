/*
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
package com.bolutions.webserver;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
    
    Pattern taskerPattern = Pattern.compile("tasker/(.+)");
    Matcher taskerMatcher = taskerPattern.matcher(dokument);

    if(taskerMatcher.matches()) {
    	try {
			dokument = java.net.URLDecoder.decode(taskerMatcher.group(1), "UTF-8");
	    	sendTasker(dokument);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	    	showHtml("403.html");
		}
    } else if(dokument.equals("tasker/")) {
    	listTaskerTasks();
    } else {
    	showHtml(dokument);
    }
  }
  
  private void sendTasker(String taskName) {
	  if(TaskerIntent.testStatus(context).equals(TaskerIntent.Status.OK)) {
		  TaskerIntent i = new TaskerIntent(taskName);
		  context.sendBroadcast(i);

	      send("Sent intent \"" + taskName + "\" to tasker.");
	  } else {
	      send("Could not sent intent \"" + taskName + "\" to tasker (" + 
	    		  TaskerIntent.testStatus(context) + ").");
	  }
  }
  
  private void listTaskerTasks() {
	  Cursor c = context.getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), null, null, null, null);

	  String text = "Found tasks:<ul>";
	  if(c != null) {
		  Log.d("Webserver", "Cursor is not null");
		  int nameCol = c.getColumnIndex("name");
		  int projNameCol = c.getColumnIndex("project_name");

		  while(c.moveToNext()) {
			  text = text + "<li>" + c.getString(projNameCol) + ": " + c.getString(nameCol) + "</li>";
		  }
		  c.close();
	  } else {
		  Log.d("Webserver", "Cursor is null");
	  }
	  text = text + "</ul>";
	  send(text);
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
		"Content-Type: text/html; charset=iso-8859-1\n\n";
  }
}
