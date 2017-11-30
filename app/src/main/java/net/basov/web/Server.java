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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Server extends Thread {
	private ServerSocket listener = null;
	private boolean running = true;
	private String documentRoot;
	private static Handler mHandler;
	private Context context;
	
	public static LinkedList<Socket> clientList = new LinkedList<Socket>();
	
    public Server(Handler handler, String documentRoot, String ip, int port, Context context) throws IOException {
		super();
		this.documentRoot = documentRoot;
		this.context = context;
		Server.mHandler = handler;
		InetAddress ipadr = InetAddress.getByName(ip);
		listener = new ServerSocket(port,0,ipadr);
	}
    
	@Override
	public void run() {
		while( running ) {
			try {
				send("Waiting for connections");
				Socket client = listener.accept();
			    
				send("New connection from " + client.getInetAddress().toString());
				new ServerHandler(documentRoot, context, client).start();
				clientList.add(client);
			} catch (IOException e) {
				send(e.getMessage());
				Log.e("Webserver", e.getMessage());
			}
		}
	}

	public void stopServer() {
		running = false;
		try {
			listener.close();
		} catch (IOException e) {
			send(e.getMessage());
			Log.e("Webserver", e.getMessage());
		}
	}
	
	public synchronized static void remove(Socket s) {
	    send("Closing connection: " + s.getInetAddress().toString());
        clientList.remove(s);      
    }
	
    private static void send(String s) {
    	if(s != null) {
	    	Message msg = new Message();
	    	Bundle b = new Bundle();
	    	b.putString("msg", s);
	    	msg.setData(b);
	    	mHandler.sendMessage(msg);
    	}
    }
    
}
