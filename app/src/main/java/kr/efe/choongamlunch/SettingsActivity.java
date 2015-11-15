package kr.efe.choongamlunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SettingsActivity extends AppCompatActivity {
    private static Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingPreferenceFragment()).commit();

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void loadAll() {
        final Handler handler = new Handler();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }

                Calendar calendar = new GregorianCalendar();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;

                for (int day = 1; day <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {
                    Object data = MainActivity.parseHtml(year, month, day);

                    if (data instanceof Integer)
                        continue;

                    MainActivity.saveCache(year, month, day, (String[]) data);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        toast = Toast.makeText(SettingsActivity.this, getString(R.string.load_all_finish), Toast.LENGTH_SHORT);
                        toast.show();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(SettingsActivity.this)
                                .setSmallIcon(R.drawable.ic_calendar)
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.load_all_finish));

                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(0, builder.build());
                    }
                });
            }
        });

        thread.start();
    }

    public static class SettingPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

            getPreferenceManager().findPreference("lunch_only").setOnPreferenceChangeListener(onPreferenceChangeListener);
            getPreferenceManager().findPreference("load_all").setOnPreferenceClickListener(onPreferenceClickListener);
            getPreferenceManager().findPreference("cache").setOnPreferenceClickListener(onPreferenceClickListener);

            String versionName = getVersionName(getActivity());
            getPreferenceManager().findPreference("app_version").setSummary(versionName);
        }

        private Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                switch (preference.getKey()) {
                    case "lunch_only":
                        MainActivity.setLunchOnly((boolean) newValue);
                        break;
                }

                return true;
            }
        };

        private Preference.OnPreferenceClickListener onPreferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                switch (preference.getKey()) {
                    case "load_all":
                        new LoadAllDialogFragment().show(getFragmentManager(), "load_all");

                        break;
                    case "cache":
                        MainActivity.clearCache();

                        if (toast != null) toast.cancel();
                        toast = Toast.makeText(preference.getContext(), getString(R.string.delete_cache), Toast.LENGTH_SHORT);
                        toast.show();

                        break;
                }

                return true;
            }
        };

        public String getVersionName(Context context) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                return pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
    }

    public static class LoadAllDialogFragment extends DialogFragment {
        private SettingsActivity settingsActivity;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            settingsActivity = (SettingsActivity) activity;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.load_all_dialog))
                    .setPositiveButton(getString(R.string.load_all_dialog_positive), confirmListener)
                    .setNegativeButton(getString(R.string.load_all_dialog_negative), cancelListener)
                    .create();
        }

        private Dialog.OnClickListener confirmListener = new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (toast != null) toast.cancel();
                toast = Toast.makeText(settingsActivity, getString(R.string.load_all_start), Toast.LENGTH_SHORT);
                toast.show();

                settingsActivity.loadAll();
            }
        };

        private Dialog.OnClickListener cancelListener = new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        };
    }
}
