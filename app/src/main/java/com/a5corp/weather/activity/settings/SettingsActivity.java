package com.a5corp.weather.activity.settings;

import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.a5corp.weather.R;
import com.a5corp.weather.activity.WeatherActivity;
import com.a5corp.weather.app.MyContextWrapper;
import com.a5corp.weather.preferences.DBHelper;
import com.a5corp.weather.preferences.Prefs;
import com.a5corp.weather.service.NotificationService;
import com.a5corp.weather.utils.Constants;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static int changed = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = MyContextWrapper.wrap(newBase, new Prefs(newBase).getLanguage());
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    private boolean isStateChanged() {
        return changed == 1;
    }

    public static class MainPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            switch (preference.getKey()) {
                case Constants.PREF_DISPLAY_LANGUAGE:
                    new MaterialDialog.Builder(getActivity())
                            .title(getString(R.string.restart_app))
                            .content(getString(R.string.restart_app_content))
                            .positiveText(getString(android.R.string.ok))
                            .build()
                            .show();
                    break;
            }
            return true;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            // gallery EditText change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_refresh_interval)));

            // notification preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units)));

            findPreference(Constants.PREF_ENABLE_NOTIFS).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    NotificationService.enqueueWork(getActivity() , new Intent(getActivity() , WeatherActivity.class));
                    return true;
                }
            });

            findPreference(Constants.PREF_DISPLAY_LANGUAGE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    new MaterialDialog.Builder(getActivity())
                            .title(getString(R.string.restart_app))
                            .content(getString(R.string.restart_app_content))
                            .show();
                    return true;
                }
            });

            findPreference(Constants.PREF_OWM_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(getActivity())
                            .title(getString(R.string.pref_owm_key_title))
                            .content(getString(R.string.pref_owm_key_summary))
                            .neutralText(getString(R.string.reset))
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    new Prefs(getActivity()).setWeatherKey(Constants.OWM_APP_ID);
                                }
                            })
                            .negativeText(getString(android.R.string.cancel))
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog , @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .input(null, null, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, @NonNull CharSequence input) {
                                    new Prefs(getActivity()).setWeatherKey(input.toString().replaceAll(" " , ""));
                                }
                            }).show();
                    return true;
                }
            });

            findPreference(Constants.PREF_DELETE_CITIES).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(getActivity())
                            .title(getString(R.string.pref_delete_cities_title))
                            .content(getString(R.string.pref_delete_cities_summary))
                            .positiveText(getString(android.R.string.ok))
                            .items(new DBHelper(getActivity()).getCities())
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                    for (CharSequence city : text) {
                                        changed = 1;
                                        new DBHelper(getActivity()).deleteCity(city.toString());
                                    }
                                    return true;
                                }
                            })
                            .show();
                    return true;
                }
            });

            findPreference(Constants.PREF_DISPLAY_LANGUAGE).setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onBackPressed() {
        if (isStateChanged())
            startActivity(new Intent(this , WeatherActivity.class));
        else
            finish();
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };
}
