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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class StartActivity extends Activity {
    private ToggleButton mToggleButton;
    private EditText port;
    private static TextView mLog;
    private static ScrollView mScroll;
    private String documentRoot;

    private String lastMessage = "";

    private ServerService mBoundService;

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            log(b.getString("msg"));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        port = (EditText) findViewById(R.id.port);
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);

        documentRoot = getDocRoot();

        if(null != documentRoot) {
            try {
                if (!(new File(documentRoot)).exists()) {
                    (new File(documentRoot)).mkdir();
                    Log.d("Webserver", "Created " + documentRoot);
                     BufferedWriter bout = new BufferedWriter(new FileWriter(documentRoot + "index.html"));
                     bout.write("<html><head><title>lightweight WebServer</title>");
                     bout.write("</head>");
                     bout.write("<body>Welcome to lWS.");
                     bout.write("<br/><br/>Document root " + documentRoot );
                     bout.write("<br/>Source code here<a href=\"https://github.com/mvbasov/lWS\">Github</a>");
                     bout.write("</body></html>");
                     bout.flush();
                     bout.close();
                    Log.d("lWS", "Created html files");
                }
            } catch (Exception e) {
                Log.v("ERROR",e.getMessage());
            }

            log("");
            log("Document-Root: " + documentRoot);
        } else {
            log("Error: Document-Root could not be found.");
        }

        mToggleButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if(mToggleButton.isChecked()) {
                    startServer(mHandler, documentRoot, new Integer(port.getText().toString()));
                } else {
                    stopServer();
                }
            }
        });

        doBindService();
    }

    public static void log( String s ) {
        mLog.append(s + "\n");
        mScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void startServer(Handler handler, String documentRoot, int port) {
        if (mBoundService == null) {
            Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
        } else {
            mBoundService.startServer(handler, documentRoot, port);
        }
    }

    private void stopServer() { 
        if (mBoundService == null) {
            Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
        } else {
            mBoundService.stopServer();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServerService.LocalBinder)service).getService();
            Toast.makeText(StartActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
            mBoundService.updateNotifiction(lastMessage);

            mToggleButton.setChecked(mBoundService.isRunning());
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(StartActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    private void doUnbindService() {
        if (mBoundService != null) {
            unbindService(mConnection);
        }
    }

    private void doBindService() {
        bindService(new Intent(StartActivity.this, ServerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private String getDocRoot() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/androidwebserver/";
    }
}
