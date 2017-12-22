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

package net.basov.lws;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import static net.basov.lws.Constants.*;

public class ServerService extends Service {

    private NotificationManager mNM;
    private Server server;
    private boolean isRunning = false;
    private String ipAddress = "";
    private static Handler gHandler;
    private static BroadcastReceiver mReceiver = null;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);      
    }

    public void startServer(Handler handler, String documentRoot) {
        ServerService.gHandler = handler;
        try {
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            // Check Tethering AP state
            Boolean isWifiAPenabled = isSharingWiFi(wifiManager);                          
            // Check WiFi state
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (((!wifiManager.isWifiEnabled()) || (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)) && !isWifiAPenabled) {
                putToLogScreen("Please connect to a WIFI-network or start Tethering.", true);
                mNM.cancel(NOTIFICATION_ID);
                throw new Exception("Please connect to a WiFi-network or start Tethering.");
            }

            final SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            // Start server
            isRunning = true;
            if (isWifiAPenabled)
                // Dirty hack.
                ipAddress="192.168.43.1";
            else
                ipAddress = intToIp(wifiInfo.getIpAddress());
            int port = Integer.valueOf(
                    sharedPreferences.getString(
                            getString(R.string.pk_port),
                            "8080"
                    )
            );
            server = new Server(handler, documentRoot, ipAddress, port, getApplicationContext());
            server.start();

            updateNotification("Running on " + ipAddress + ":" + port);
            putToLogScreen("Web server is running on port " + ipAddress + ":" + port);

            // register broadcast receiver to monitor WiFi state
            if (mReceiver == null) {
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        if (info != null && info.getState() == NetworkInfo.State.DISCONNECTED) {
                            putToLogScreen("Web server stopped because WiFi disconnected.");
                            stopServer();
                        }
                    }
                };
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                registerReceiver(mReceiver, filter);
            }

        } catch (Exception e) {
            isRunning = false;
            Log.e(LOG_TAG, e.getMessage()+ "(from ServerService.startServer())");
            if (!isRunning) mNM.cancel(NOTIFICATION_ID);
        }
    }

    private static String intToIp(int i) {
        return ((i       ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ( i >> 24   & 0xFF);
    }

    public void stopServer() {
        isRunning = false;
        ipAddress = "";
        mNM.cancel(NOTIFICATION_ID);
        ipAddress = "";
        try {
            //TODO: Exception when unregister receiver which is new...
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            putToLogScreen("Receiver unregister error again :( (stopServer())");
            Log.e(LOG_TAG, e.getMessage() + "on ServerService.stopServer()");
        }
        if (null != server) {
            server.stopServer();
            server.interrupt();          	
        }
    }

    public void updateNotification(String message) {
        if (null == message || message.length()==0) return;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, StartActivity.class),
                0
        );
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_http_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.lws_ic))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .build();
        mNM.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!isRunning) mNM.cancel(NOTIFICATION_ID);
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

    @Override
    public void onDestroy() {
        stopServer();
        stopSelf();      
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopServer();
        stopSelf();      
        super.onTaskRemoved(rootIntent);
    }

    public String getIpAddress() { return ipAddress; }

    private void putToLogScreen(String message) {
        putToLogScreen(message, false);
    }

    private void putToLogScreen(String message, Boolean isToast) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", message);
        if (isToast)
            b.putBoolean("toast",true);
        msg.setData(b);
        gHandler.sendMessage(msg);
    }
    
    // Code from https://stackoverflow.com/a/20432036
    // Check Tethering AP enabled
    private static boolean isSharingWiFi(final WifiManager manager) {
        try {
            final Method method = manager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs
            return (Boolean) method.invoke(manager);
        }
        catch (final Throwable ignored) { }
        return false;
    }
}
