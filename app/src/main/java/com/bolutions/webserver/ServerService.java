/*
 * Copyright (C) 2009-2014 Markus Bode
 * Copyright (C) 2017 Mikhail Basov
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


import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class ServerService extends Service {

    private int NOTIFICATION_ID = 4711;
    private NotificationManager mNM;
    private String message;
    private Notification notification;
    private Server server;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }
    
    private void showNotification() {
    	updateNotifiction("");
        startForeground(NOTIFICATION_ID, notification);
    }
    
    public void startServer(Handler handler, String documentRoot, int port) {
    	try {
    		isRunning = true;
    		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    		
    		String ipAddress = intToIp(wifiInfo.getIpAddress());

    		if( wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
    			new AlertDialog.Builder(this).setTitle("Error").setMessage("Please connect to a WIFI-network for starting the webserver.").setPositiveButton("OK", null).show();
    			throw new Exception("Please connect to a WIFI-network.");
    		}
            
		    server = new Server(handler, documentRoot, ipAddress, port, getApplicationContext());
		    server.start();
		    
	        Intent i = new Intent(this, StartActivity.class);
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

	        updateNotifiction("Webserver is running on port " + ipAddress + ":" + port);
	        
	    	Message msg = new Message();
	    	Bundle b = new Bundle();
	    	b.putString("msg", "Webserver is running on port " + ipAddress + ":" + port);
	    	msg.setData(b);
	    	handler.sendMessage(msg);
	    	
    	} catch (Exception e) {
    		isRunning = false;
    		Log.e("Webserver", e.getMessage());
	        updateNotifiction("Error: " + e.getMessage());
    	}
    }

    public static String intToIp(int i) {
        return ((i       ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ( i >> 24   & 0xFF);
    }
    
    public void stopServer() {
    	if(null != server) {
			server.stopServer();
			server.interrupt();
			isRunning = false;
    	}
    }
    
    public void updateNotifiction(String message) {
        CharSequence text = message;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
        notification = new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent)
            .build();
        mNM.notify(NOTIFICATION_ID, notification);

    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
    	ServerService getService() {
            return ServerService.this;
        }
    }
    
    public boolean isRunning() {
    	return isRunning;
    }
}
