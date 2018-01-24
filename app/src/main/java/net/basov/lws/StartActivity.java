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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Array;

import android.app.Activity;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static net.basov.lws.Constants.*;

public class StartActivity extends Activity {
    private static int prevMsgCount;
    private static String prevMsg;
    private ToggleButton mToggleButton;
    private static TextView mLog;
    private static ScrollView mScroll;
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

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);

        Button btnSettings = (Button) findViewById(R.id.buttonSettings);
        btnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, PreferencesActivity.class);
                startActivity(i);
            }
        });

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

        documentRoot = getDocumentRoot();

        if(null == documentRoot) {
            log("E: Document-Root could not be found.");
        }

        mToggleButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent intent = new Intent(StartActivity.this, ServerService.class);
                if(mToggleButton.isChecked()) {
                    startServer(mHandler);
                    startService(intent);
                } else {
                    stopServer();
                    stopService(intent);
                }
                refreshMainScreen();
            }
        });

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

    private void refreshMainScreen() {
        final TextView viewDirectoryRoot = (TextView) findViewById(R.id.document_root);
        final TextView viewAddress = (TextView) findViewById(R.id.address);
        final TextView viewPort = (TextView) findViewById(R.id.port);
        final Button btnBrowser = (Button) findViewById(R.id.buttonOpenBrowser);
        final Button btnSendURL = (Button) findViewById(R.id.buttonSendURL);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        documentRoot = getDocumentRoot();
        viewDirectoryRoot.setText(documentRoot);

        final String port = sharedPreferences.getString(
                getString(R.string.pk_port),
                "8080"
        );
        viewPort.setText(port);

        if(mBoundService != null) {         
            mToggleButton.setChecked(mBoundService.isRunning());         
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
                
                btnBrowser.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {            
                            String url =
                                    "http://"
                                    + ipAddress 
                                    + ":"
                                    + port
                                    + "/";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);               
                        }
                });
                btnBrowser.setEnabled(true);

                btnSendURL.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url =
                                "http://"
                                        + ipAddress
                                        + ":"
                                        + port
                                        + "/";
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setData(Uri.parse(url));
                        i.setType("text/html");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Current lWS URL");
                        i.putExtra(Intent.EXTRA_TEXT, url);
                        startActivity(i);
                    }
                });
                btnSendURL.setEnabled(true);

            } else {
                viewAddress.setText("");
                btnBrowser.setEnabled(false);
                btnSendURL.setEnabled(false);
            }
        } else {
            viewAddress.setText("");
            btnBrowser.setEnabled(false);
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
                    log("I: Default DocumentRoot HTML index file creted.");
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
                mLog.append("... previous repeaed " + prevMsgCount +"times.\n");
            prevMsgCount = 0;
            prevMsg = s;
            mLog.append(s + "\n");
        }
        mScroll.fullScroll(ScrollView.FOCUS_DOWN);
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
}
