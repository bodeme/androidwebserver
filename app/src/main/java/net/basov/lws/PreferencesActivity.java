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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences defSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Preference prefDocumetRoot = findPreference(getString(R.string.pk_document_root));
        prefDocumetRoot.setSummary(defSharedPref.getString(getString(R.string.pk_document_root), ""));

        Preference prefPort = findPreference(getString(R.string.pk_port));
        prefPort.setSummary(defSharedPref.getString(getString(R.string.pk_port), "8080"));

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
            pref.setSummary(sharedPreferences.getString(pref_port,"8081"));
        }
    }
}


