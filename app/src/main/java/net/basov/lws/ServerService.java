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

import android.app.Notification;
import android.app.NotificationChannel;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static net.basov.lws.Constants.*;

public class ServerService extends Service {
    private NotificationManager mNM;
    private Server server;
    private boolean isRunning = false;
    private String ipAddress = "";
    private static Handler gHandler;
    private static BroadcastReceiver mReceiver = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        if (intent != null
            && intent.getAction() != null
            && intent.getAction().equals(ACTION_STOP)
            ) stopServer();
        
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);      
    }

    public void startServer(Handler handler) {
        ServerService.gHandler = handler;
        try {
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            // Check Tethering AP state
            Boolean isWifiAPEnabled = isSharingWiFi(wifiManager);
            // Check WiFi state
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
 
            final SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            // Start server
            isRunning = true;
            if (isWifiAPEnabled) {
                ipAddress = getAPIpAddress();         
            } else {
                ipAddress = intToIp(wifiInfo.getIpAddress());
            }
            int port = Integer.valueOf(
                    sharedPreferences.getString(
                            getString(R.string.pk_port),
                            "8080"
                    )
            );

            if (
                (
                (!wifiManager.isWifiEnabled())
                || (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)
                || (wifiInfo.getIpAddress() == 0)
                )
                && !isWifiAPEnabled
                ) {
                ipAddress="127.0.0.1";
                StartActivity.putToLogScreen(
                        "Connected to loopback interface (127.0.0.1)\nTo change it connect to a WiFi-network or start Tethering, then restart server.",
                        gHandler,
                        true
                );
            }
            
            server = new Server(
                    gHandler,
                    sharedPreferences.getString(getString(R.string.pk_document_root), ""),
                    ipAddress,
                    port,
                    getApplicationContext()
            );
            server.start();

            startForegroundService("Running on " + ipAddress + ":" + port);

            StartActivity.putToLogScreen(
                    "I: Web server address http://"
                    + ipAddress
                    + ":"
                    + port,
                    gHandler
            );

            // register broadcast receiver to monitor WiFi state
            if (mReceiver == null) {
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        if (getIpAddress().equals("127.0.0.1")) return;
                        
                        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        if (info != null && info.getState() == NetworkInfo.State.DISCONNECTED) {
                            StartActivity.putToLogScreen(
                                    "I: Web server stopped because WiFi disconnected.",
                                    gHandler
                            );
                            stopServer();
                        }

                        Integer tetheringState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);                     
                        if (tetheringState > 10) tetheringState -= 10; // Old android fix
                        if (tetheringState == 10) {
                            StartActivity.putToLogScreen(
                                    "I: Web server stopped because AP switched off.",
                                    gHandler
                            );
                            stopServer();
                        }
                    }
                };
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                registerReceiver(mReceiver, filter);
            }

        } catch (Exception e) {
            isRunning = false;
            mNM.cancel(NOTIFICATION_ID);
            Log.e(LOG_TAG, e.getMessage()+ "(from ServerService.startServer())");
            StartActivity.putToLogScreen("E: " + e.getMessage(), gHandler);
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
        try {
            //TODO: Exception when unregister receiver which is new...
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            StartActivity.putToLogScreen(
                    "E: Receiver unregister error again :( (stopServer())",
                    gHandler
            );
            Log.e(LOG_TAG, e.getMessage() + "on ServerService.stopServer()");
        }
        if (null != server) {
            server.stopServer();
            server.interrupt();          	
        }
        stopForeground(true);
        stopSelf();
    }

    private void startForegroundService(String message) {
        if (null == message || message.length()==0) return;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                Constants.MAIN_SCREEN_REQUEST,
                new Intent(this, StartActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        Intent stopIntent = new Intent(this,ServerService.class);     
        stopIntent.setAction(Constants.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                Constants.STOP_SERVICE_REQUEST,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.lws_ic)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.lws_ic))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .addAction(0, "Stop service", stopPendingIntent)
                .setOngoing(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {              
                notificationBuilder.setChannelId(this.getString(R.string.notif_ch_id));
            }        
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
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
    
    // Code from https://stackoverflow.com/questions/17302220/android-get-ip-address-of-a-hotspot-providing-device
    // Get IP address in WiFi hotspot (tethering) mode
    private String getAPIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                    .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                    .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()
                           && networkInterface.getName().toLowerCase().contains("wlan")
                       ) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return ip;
    }
}
