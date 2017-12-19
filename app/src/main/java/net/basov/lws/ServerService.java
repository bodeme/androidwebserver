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

import static net.basov.lws.Constants.*;

public class ServerService extends Service {

    private NotificationManager mNM;
    private Notification notification;
    private Server server;
    private boolean isRunning = false;
    private String ipAddress = "";
    private static Handler gHandler;
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);      
    }

    public void startServer(Handler handler, String documentRoot) {
        ServerService.gHandler = handler;
        try {
            // Check WiFi state
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if ((!wifiManager.isWifiEnabled()) || (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)) {
                putToLogScreen("Please connect to a WIFI-network.", true);
                throw new Exception("Please connect to a WIFI-network.");
            }

            final SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            // Start server
            isRunning = true;
            ipAddress = intToIp(wifiInfo.getIpAddress());
            int port = Integer.valueOf(
                    sharedPreferences.getString(
                            getString(R.string.pk_port),
                            "8080"
                    )
            );
            server = new Server(handler, documentRoot, ipAddress, port, getApplicationContext());
            server.start();

            updateNotifiction("Running on " + ipAddress + ":" + port);
            putToLogScreen("Webserver is running on port " + ipAddress + ":" + port);

            // register broadcast receiver to monitor WiFi state
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (info != null && info.getState() == NetworkInfo.State.DISCONNECTED) {
                        putToLogScreen("Webserver stopped because WiFi disconnected.");
                        stopServer();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            registerReceiver(mReceiver, filter);

        } catch (Exception e) {
            isRunning = false;
            Log.e(LOG_TAG, e.getMessage()+ "(from ServerService.startServer())");
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
        isRunning = false;
        mNM.cancel(NOTIFICATION_ID);
        ipAddress = "";
        //TODO: Exception when unrigester receiver which is new...
        if (mReceiver != null) unregisterReceiver(mReceiver);
        if (null != server) {
            server.stopServer();
            server.interrupt();          	
        }
    }

    public void updateNotifiction(String message) {
        if (null == message || message.length()==0) return;
        CharSequence text = message;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, StartActivity.class),
                0
        );
        notification = new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_http_black_24dp)
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

    @Override
    public void onDestroy() {
        stopServer();
        stopSelf();
        //TODO: Exception when unrigester receiver which is new...
        if (mReceiver != null) unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopServer();
        stopSelf();
        if (mReceiver != null) unregisterReceiver(mReceiver);
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
}
