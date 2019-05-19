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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static net.basov.lws.Constants.*;

public class StartActivity extends Activity {
    private static int prevMsgCount;
    private static String prevMsg;
    private ToggleButton btnStartStop;
    private static TextView viewLog;
    private static ScrollView viewScroll;
    private String documentRoot;
  
    private ServerService mBoundService;
    private ServiceConnection mConnection;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {     
        StartActivity.prevMsg = "";
        StartActivity.prevMsgCount = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.hello);
        
        btnStartStop = (ToggleButton) findViewById(R.id.buttonStartStop);
        viewLog = (TextView) findViewById(R.id.log);
        viewScroll = (ScrollView) findViewById(R.id.ScrollView);

        findViewById(R.id.buttonSettings)
                .setOnClickListener(makePrefListener(-1));
        
        try {
            android.content.pm.PackageInfo pInfo =
                    this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            String appName =
                    this.getString(R.string.hello)
                    + " v"
                    + pInfo.versionName;
            log(
                    appName
                    + "\n"
                    + new String(new char[appName.length()]).replace('\0', '*')
            );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        documentRoot = getDocumentRoot();

        if(null == documentRoot) {
            log("E: Document-Root could not be found.");
        }

        /**
         * Hide QR Code plugin call button if run on SdkVersion older then 16
         * because plugin doesn't operate on older versions
         * If version is higher set total button group weight to 3 else set to 2
         */
        LinearLayout btnGroup = findViewById(R.id.buttonsBlock);
        View btnQR = findViewById(R.id.buttonQRCodeURL);
        if (Build.VERSION.SDK_INT < 16) {
            btnQR.setVisibility(View.GONE);
            btnGroup.setWeightSum(2f);
        } else {
            btnGroup.setWeightSum(3f);
        }

        btnStartStop.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent intent = new Intent(StartActivity.this, ServerService.class);
                if(btnStartStop.isChecked()) {
                    startServer(mHandler);
                    startService(intent);
                } else {
                    stopServer();
                    stopService(intent);
                }
                refreshMainScreen();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);               
            NotificationChannel channel = new NotificationChannel(
                    this.getString(R.string.notif_ch_id),
                    this.getString(R.string.notif_ch_hr), 
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setSound(null, null);
            mNM.createNotificationChannel(channel);
        }

        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((ServerService.LocalBinder)service).getService();
                Toast.makeText(
                        StartActivity.this,
                        "Service connected",
                        Toast.LENGTH_SHORT
                ).show();
                refreshMainScreen();
            }

            public void onServiceDisconnected(ComponentName className) {
                mBoundService = null;
                Toast.makeText(
                        StartActivity.this,
                        "Service disconnected",
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        doBindService();

        refreshMainScreen();
    }

    private void doUnbindService() {
        if (mBoundService != null) {
            getApplicationContext().unbindService(mConnection);
        }

    }

    private void startServer(Handler handler) {
        if (mBoundService == null) {
            Toast.makeText(
                    StartActivity.this,
                    "Service not connected",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                String defaultDocumentRoot = StartActivity.getFilesDir(this).getPath() + "/html/";
                if (!documentRoot.equals(defaultDocumentRoot)) {
                    if (
                            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                GRANT_WRITE_EXTERNAL_STORAGE
                        );
                    }
                }
            }
            mBoundService.startServer(handler);
        }
    }

    private void stopServer() { 
        if (mBoundService == null) {
            Toast.makeText(
                    StartActivity.this,
                    "Service not connected",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            mBoundService.stopServer();
        }
    }

    private void doBindService() {
        getApplicationContext()
                .bindService(
                        new Intent(
                                StartActivity.this,
                                ServerService.class),
                        mConnection,
                        Context.BIND_AUTO_CREATE
                );
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
        refreshMainScreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // Called then status bar pulled up
        if (hasFocus) refreshMainScreen();
        super.onWindowFocusChanged(hasFocus);
    }

    private void refreshMainScreen() {
        final TextView viewDirectoryRoot = (TextView) findViewById(R.id.document_root);
        final TextView viewAddress = (TextView) findViewById(R.id.address);
        final TextView viewPort = (TextView) findViewById(R.id.port);
        final Button btnBrowser = (Button) findViewById(R.id.buttonOpenBrowser);
        final Button btnSendURL = (Button) findViewById(R.id.buttonSendURL);
        final Button btnQRCodeURL = (Button) findViewById(R.id.buttonQRCodeURL);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        documentRoot = getDocumentRoot();
        viewDirectoryRoot.setText(documentRoot);
        viewDirectoryRoot.setOnClickListener(makePrefListener(1));

        final String port = sharedPreferences.getString(
                getString(R.string.pk_port),
                "8080"
        );
        viewPort.setText(port);
        
        viewPort.setOnClickListener(makePrefListener(2));

        if(mBoundService != null) {         
            btnStartStop.setChecked(mBoundService.isRunning());
            if (mBoundService.isRunning()) {
                if (sharedPreferences.getBoolean(getString(R.string.pk_pref_changed), false)) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    stopServer();
                    startServer(mHandler);
                    Toast.makeText(StartActivity.this,"Service restarted because configuration changed", Toast.LENGTH_SHORT).show();
                    editor.putBoolean(getString(R.string.pk_pref_changed), false);
                    editor.commit();
                }
                final String ipAddress = mBoundService.getIpAddress();
                viewAddress.setText(ipAddress);
                viewAddress.setTextColor(0xFFFFFF00);

                final String url =
                        "http://"
                        + ipAddress
                        + ":"
                        + port
                        + "/";
                btnBrowser.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {            
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);               
                        }
                });
                btnBrowser.setEnabled(true);

                btnQRCodeURL.setOnClickListener( new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PackageManager pm = getApplicationContext().getPackageManager();
                        try {
                            pm.getPackageInfo(getString(R.string.qrPluginPackage), 0);
                            Intent i = new Intent(getString(R.string.qrIntentAction));
                            i.setData(Uri.parse("createqr:"));
                            i.putExtra("ENCODE_DATA", url);
                            i.putExtra("ENCODE_LABEL", "Open lWS page<br/>(" + url + ")");
                            i.putExtra("ENCODE_CORRECTION", "L");
                            i.putExtra("ENCODE_MODULE_SIZE", 6);
                            i.putExtra("ENCODE_MASK", -1);
                            i.putExtra("ENCODE_MIN_VERSION", 1);
                            
//                            i.putExtra("ENCODE_DATA", url);
//                            i.putExtra("ENCODE_SIZE", "256");
//                            i.putExtra("ENCODE_DARK", "#000");
//                            i.putExtra("ENCODE_LIGHT", "#e0ffff");
//                            i.putExtra("ENCODE_CORRECTION", "L");

                            startActivity(i);
                        } catch (PackageManager.NameNotFoundException e_lws_qr) {
                            try {
                                pm.getPackageInfo("com.google.zxing.client.android", 0);
                                Intent i = new Intent("com.google.zxing.client.android.ENCODE");
                                i.putExtra("ENCODE_TYPE", "TEXT_TYPE");
                                i.putExtra("ENCODE_DATA", url);
                                i.putExtra("ENCODE_FORMAT", "QRCODE");
                                startActivity(i);
                            } catch (PackageManager.NameNotFoundException e_zxing) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse("market://details?id=" + getString(R.string.qrPluginPackage)));
                                startActivity(i);
                            }
                        }
                    }
                });
                if (!ipAddress.equals("127.0.0.1"))
                    btnQRCodeURL.setEnabled(true);

                OnClickListener sendListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setData(Uri.parse(url));
                        i.setType("text/html");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Current lWS URL");
                        i.putExtra(Intent.EXTRA_TEXT, url);
                        startActivity(i);
                    }
                };
                if (!ipAddress.equals("127.0.0.1")) {
                    btnSendURL.setOnClickListener(sendListener);
                    btnSendURL.setEnabled(true);
                    viewAddress.setOnClickListener(sendListener);
                }

            } else {
                viewAddress.setText("not running");
                viewAddress.setTextColor(0xFFFF0000);
                viewAddress.setOnClickListener(null);
                viewAddress.setClickable(false);            
                btnBrowser.setEnabled(false);
                btnSendURL.setEnabled(false);
                btnQRCodeURL.setEnabled(false);
            }
        } else {
            viewAddress.setText("not running");
            viewAddress.setTextColor(0xFFFF0000);
            viewAddress.setOnClickListener(null);
            viewAddress.setClickable(false);
            btnBrowser.setEnabled(false);
            btnSendURL.setEnabled(false);
            btnQRCodeURL.setEnabled(false);
        }
    }

    public static File getFilesDir(Context c) {
        File filesDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (Build.VERSION.SDK_INT <= 18)
                filesDir = new File(Environment.getExternalStorageDirectory()
                        + "/Android/data/"
                        + c.getPackageName()
                        +"/files"
                );
            else
                filesDir = c.getExternalFilesDir(null);
        } else {
            filesDir = c.getFilesDir();
        }
        return filesDir;
    }

    private String getDocumentRoot(){
        String defaultDocumentRoot = getFilesDir(this).getPath() + "/html/";
        final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        String documentRoot = sharedPreferences.getString(
            getString(R.string.pk_document_root),
            ""
        );

        if (documentRoot.length() == 0 ) {
            // if preferences contain empty string or absent reset it to default
            documentRoot = defaultDocumentRoot;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pk_document_root), documentRoot);
            editor.commit();
        }

        if (documentRoot.equals(defaultDocumentRoot)) createDefaultIndex();

        return documentRoot;
    }

    private void createDefaultIndex() {
        try {
            String defaultDocumentRoot = getFilesDir(this).getPath() + "/html/";
            File defaultDocumentRootDirectory = new File(defaultDocumentRoot);
            if (!defaultDocumentRootDirectory.exists()) {
                if(defaultDocumentRootDirectory.mkdirs()) {
                    BufferedWriter bout = new BufferedWriter(new FileWriter(defaultDocumentRoot + "index.html"));
                    bout.write(getString(R.string.def_doc_root_index, defaultDocumentRoot));
                    bout.flush();
                    bout.close();
                    log("I: Default DocumentRoot HTML index file created.");
                } else {
                    throw new Exception("Can't create document root.");
                }
            }
        } catch (Exception e) {
            log("E: Error creating HTML index file.");
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    /**
    * Application main screen related functions and handler
    */
    
    private static void log(String s) {
        if (prevMsg.equals(s)) {
            prevMsgCount++;
        }else {
            if (prevMsgCount != 0)
                viewLog.append("... previous string repeated " + prevMsgCount +" times.\n");
            prevMsgCount = 0;
            prevMsg = s;
            viewLog.append(s + "\n");
        }
        viewScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }
    
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            if (b.containsKey("toast")){
                Toast.makeText(StartActivity.this, b.getString("msg"), Toast.LENGTH_SHORT).show();
            }
            log(b.getString("msg"));
        }
    };
    
    public static void putToLogScreen(String message, Handler msgHandler) {
        putToLogScreen(message, msgHandler, false);
    }

    public static void putToLogScreen(String message, Handler msgHandler, Boolean isToast) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", message);
        if (isToast)
            b.putBoolean("toast",true);
        msg.setData(b);
        msgHandler.sendMessage(msg);
    }

    private OnClickListener makePrefListener(final int index) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, PreferencesActivity.class);
                if (index != -1 )
                    i.putExtra("item", index);
                startActivity(i);
            }
        };
    }

}
