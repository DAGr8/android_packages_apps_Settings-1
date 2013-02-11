/*
 * Copyright (C) 2013 The LiquidSmooth Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.liquid;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.view.IWindowManager;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class InterfaceSettings extends SettingsPreferenceFragment
	implements OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";
    private static final String ADVANCED_SETTINGS = "interface_advanced";
    private static final String KEY_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_NOTIF_STYLE = "notification_style";
    private static final String KEY_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String KEY_FORCE_DUAL_PANE = "force_dual_pane";

    private Preference mLcdDensity;
    private PreferenceCategory mAdvanced;
    private Preference mCustomLabel;
    private Preference mNotifStyle;
    private Preference mRamBar;
    private CheckBoxPreference mDualPane;

    int newDensityValue;
    DensityChanger densityFragment;
    String mCustomLabelText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();
        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }
        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);
        mAdvanced = (PreferenceCategory) prefs.findPreference(ADVANCED_SETTINGS);

        mCustomLabel = findPreference(KEY_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        // Only show the hardware keys config on a device that does not have a navbar
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                mAdvanced.removePreference(findPreference(KEY_HARDWARE_KEYS));
            }
        } catch (RemoteException e) {
            // Do nothing
        }

        mNotifStyle = findPreference(KEY_NOTIF_STYLE);
        mRamBar = findPreference(KEY_RECENTS_RAM_BAR);
        updateRamBar();

        mDualPane = (CheckBoxPreference) findPreference(KEY_FORCE_DUAL_PANE);
        boolean preferDualPane = getResources().getBoolean(
                com.android.internal.R.bool.preferences_prefer_dual_pane);
        boolean dualPaneMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.FORCE_DUAL_PANE, (preferDualPane ? 1 : 0)) == 1;
        mDualPane.setChecked(dualPaneMode);
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0) {
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        } else {
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRamBar();
    }

    @Override
    public void onPause() {
        super.onResume();
        updateRamBar();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mDualPane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FORCE_DUAL_PANE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    mContext.sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}