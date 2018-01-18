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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
    private ToggleButton mToggleButton;
    private static TextView mLog;
    private static ScrollView mScroll;
    private String documentRoot;

    private ServerService mBoundService;

    private ServiceConnection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.hello);

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);

        Button button = (Button) findViewById(R.id.buttonSettings);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, PreferencesActivity.class);
                StartActivity.this.startActivityForResult(i, PREFERENCES_REQUEST);
            }
        });

        documentRoot = getDocumentRoot();

        if(null != documentRoot) {
            log("");
        } else {
            log("Error: Document-Root could not be found.");
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

    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_MAIN)) {
                Bundle main_extras = intent.getExtras();
                if (main_extras == null) Log.d(Constants.LOG_TAG, "main_extras == null");
                if (main_extras.get(Constants.ACTION_CMD_KEY) == null) Log.d(Constants.LOG_TAG, "main_extras.getString == null");
                if (
                        main_extras != null
                        && main_extras.getString(Constants.ACTION_CMD_KEY) != null
                        && main_extras.getString(Constants.ACTION_CMD_KEY).equals(Constants.CMD_STOP)
                    ) {

                    stopServer();
                    stopService(intent);
                    finish();
                    //refreshMainScreen();
                }
            }
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFERENCES_REQUEST) {
            SharedPreferences defSharedPref =
                    PreferenceManager.getDefaultSharedPreferences(this);
            if (defSharedPref.getBoolean(getString(R.string.pk_pref_changed), false)) {
                SharedPreferences.Editor editor = defSharedPref.edit();
                if(mBoundService != null &&  mBoundService.isRunning()) {
                    stopServer();
                    startServer(mHandler);
                }
                refreshMainScreen();
                editor.putBoolean(getString(R.string.pk_pref_changed), false);
                editor.commit();
            }
        }
    }

    private void refreshMainScreen() {
        final TextView viewDirectoryRoot = (TextView) findViewById(R.id.document_root);
        final TextView viewAddress = (TextView) findViewById(R.id.address);
        final TextView viewPort = (TextView) findViewById(R.id.port);
        final Button btnBrowser = (Button) findViewById(R.id.buttonOpenBrowser);
        
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
            } else {
                viewAddress.setText("");
                btnBrowser.setEnabled(false);
            }
        } else {
            viewAddress.setText("");
            btnBrowser.setEnabled(false);
        }
    }

    private static File getFilesDir(Context c) {
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
        final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        String dr = sharedPreferences.getString(
            getString(R.string.pk_document_root),
            ""
        );
        if (dr.length() == 0) {
            // if preferences contain empty string or absent reset it to default
            dr = getFilesDir(this).getPath() + "/html/";
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pk_document_root), dr);
            editor.commit();
            try {
                File documentRootDirectory = new File(dr);
                if (!documentRootDirectory.exists()) {
                    if(documentRootDirectory.mkdir()) {
                        Log.d(LOG_TAG, "Created " + dr);
                        BufferedWriter bout = new BufferedWriter(new FileWriter(dr + "index.html"));
                        bout.write("<html><head><title>lightweight WebServer</title>");
                        bout.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
                        bout.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"> ");
                        bout.write("</head>");
                        bout.write("<body>Welcome to lWS.");
                        bout.write("<br/><br/>Default document root is" + dr);
                        bout.write("<br/>Source code here<a href=\"https://github.com/mvbasov/lWS\">GitHub</a>");
                        bout.write("</body></html>");
                        bout.flush();
                        bout.close();
                        Log.d(LOG_TAG, "Created html files");
                        log("Default DocumentRoot HTML index file creted.");
                    } else {
                        throw new Exception("Can't create document root.");
                    }
                }
            } catch (Exception e) {
                Log.v(LOG_TAG,e.getMessage());
            }
        }
        return dr;
    }
    
    /**
    * Application main screen related functions and handler
    */
    
    private static void log(String s) {
        mLog.append(s + "\n");
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
