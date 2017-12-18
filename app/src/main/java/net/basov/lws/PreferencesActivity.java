/*
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
package net.basov.lws;

/**
 * Created by mvb on 6/22/17.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import static net.basov.lws.Constants.*;

public class PreferencesActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences defSharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        Preference prefDocumetRoot = findPreference(getString(R.string.pk_document_root));
        prefDocumetRoot.setSummary(defSharedPref.getString(getString(R.string.pk_document_root), ""));
        if(defSharedPref.getBoolean(getString(R.string.pk_use_directory_pick), true)) {
            prefDocumetRoot.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((EditTextPreference) preference).getDialog().dismiss();
                    Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
                    intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Select document root");
                    startActivityForResult(intent, DIRECTORY_REQUEST);
                    return true;
                }
            });
        }

        Preference prefPort = findPreference(getString(R.string.pk_port));
        prefPort.setSummary(defSharedPref.getString(getString(R.string.pk_port), "8080"));

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
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pk_document_root), newValue);
            editor.commit();
            recreate();
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
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(getString(R.string.pk_pref_changed), true).apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);

        String pref_document_root = getString(R.string.pk_document_root);
        if (pref_document_root.equals(key)) {
            pref.setSummary(sharedPreferences.getString(pref_document_root,""));
        }

        String pref_port = getString(R.string.pk_port);
        if (pref_port.equals(key)) {
            pref.setSummary(sharedPreferences.getString(pref_port,"8080"));
        }

        String pref_use_directory_pick = getString(R.string.pk_use_directory_pick);
        if (pref_use_directory_pick.equals(key)) {
            recreate();
        }

    }
}
