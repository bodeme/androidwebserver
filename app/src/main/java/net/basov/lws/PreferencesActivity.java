/*
 * Copyright (C) 2017-2019 Mikhail Basov
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

/**
 * Created by mvb on 6/22/17.
 */

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static net.basov.lws.Constants.*;

public class PreferencesActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences defSharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        Preference prefDocumentRoot = findPreference(getString(R.string.pk_document_root));
        prefDocumentRoot.setSummary(defSharedPref.getString(getString(R.string.pk_document_root), ""));
        enableDirPicker(
                prefDocumentRoot,
                defSharedPref.getBoolean(getString(R.string.pk_use_directory_pick), false)
        );

        Preference prefPort = findPreference(getString(R.string.pk_port));
        prefPort.setSummary(defSharedPref.getString(getString(R.string.pk_port), "8080"));
        
        Intent incomingIntent = getIntent();
        Bundle incomingExtras = incomingIntent.getExtras();
        if (incomingExtras != null) {
            int incomingIndex = incomingExtras.getInt("item");
            if (incomingIndex >= 0 && incomingIndex <= 2 ) {
                PreferenceScreen screen = getPreferenceScreen();
                screen.onItemClick(null, null, incomingIndex , 0);
            }
        }      
        
    }

    private void enableDirPicker(Preference p, Boolean enable) {
        if (enable) {
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((EditTextPreference) preference).getDialog().dismiss();
                    Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
                    intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Select document root");
                    try {
                        startActivityForResult(intent, DIRECTORY_REQUEST);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(PreferencesActivity.this,
                                "OI File Manager not installed. Install or disable using.",
                                Toast.LENGTH_LONG
                        ).show();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("market://details?id=org.openintents.filemanager"));
                        startActivity(i);
                        Log.w("lWS", "OI File Manager not found", e);
                    }
                    return true;
                }
            });
        } else {
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    return false;
                }
            });
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIRECTORY_REQUEST && data != null) {
            String newValue = null;
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.toString();
                if (path.toLowerCase().startsWith("file://")) {
                    newValue = path.replace("file://","") + "/";
                }
            }
            try {
                newValue = URLDecoder.decode(newValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("lWS", "Invalid document root picked up (URLDecoder)", e);
            }
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pk_document_root), newValue);
            editor.putBoolean(getString(R.string.pk_pref_changed), true);
            editor.commit();
            findPreference(getString(R.string.pk_document_root)).setSummary(newValue);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();     
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);
        SharedPreferences.Editor prefEdit = sharedPreferences.edit();

        String pref_document_root = getString(R.string.pk_document_root);
        if (pref_document_root.equals(key)) {
            String defaultDocumentRoot = StartActivity.getFilesDir(this).getPath() + "/html/";
            String documentRoot = sharedPreferences.getString(pref_document_root, defaultDocumentRoot);
            int docRootLength = documentRoot.length();
            if (! new File(documentRoot).canRead() || docRootLength == 0){
                documentRoot = defaultDocumentRoot;
                docRootLength = documentRoot.length();
                Toast.makeText(PreferencesActivity.this,
                        "Document root doesn't exists. Set to default.",
                        Toast.LENGTH_LONG
                ).show();
                Log.w("lWS", "Document root doesn't exists. Set to default.");
                prefEdit.putString(getString(R.string.pk_document_root), defaultDocumentRoot).commit();
            } else if (documentRoot.charAt(docRootLength - 1) != '/') {
                // existing directory readable with and without trailing slash
                // but slash need for correct filename forming
                documentRoot = documentRoot + "/";
                prefEdit.putString(getString(R.string.pk_document_root), documentRoot).commit();
            }
            prefEdit.putBoolean(getString(R.string.pk_pref_changed), true).commit();
            pref.setSummary(documentRoot);
        }

        String pref_port = getString(R.string.pk_port);
        if (pref_port.equals(key)) {
            Integer port;
            String portAsString = sharedPreferences.getString(pref_port,"8080");
            try {
                port = Integer.valueOf(portAsString);
            } catch (NumberFormatException e) {
                port = 8080;
                Log.w(Constants.LOG_TAG, "Port preferences may be empty");
            }
            if (port < 1024 || port > 65535 || portAsString.length() == 0) {
                port = 8080;
                portAsString = Integer.toString(port);
                Toast.makeText(PreferencesActivity.this,
                        "Port less then 1024 or grate then 65535. Set to default.",
                        Toast.LENGTH_LONG
                ).show();
                Log.w("lWS", "Port less then 1024 or grate then 65535. Set to default.");
                prefEdit.putString(getString(R.string.pk_port), portAsString).commit();
            }
            prefEdit.putBoolean(getString(R.string.pk_pref_changed), true).commit();
            pref.setSummary(portAsString);
        }

        String pref_use_directory_pick = getString(R.string.pk_use_directory_pick);
        if (pref_use_directory_pick.equals(key)) {
            // don't set preferences changed flag if only use directory pick changed
            Preference prefDocumentRoot = findPreference(getString(R.string.pk_document_root));
            enableDirPicker(
                    prefDocumentRoot,
                    sharedPreferences.getBoolean(pref_use_directory_pick, false)
            );
        }
               
    }
}
