package kr.efe.choongamlunch;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.widget.ListViewCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class MainActivity extends AppCompatActivity {
    private Toast toast;
    private Calendar calendar;
    private int year, month, day;
    private String[] dayOfWeek;
    private boolean isLoading = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private static File cacheDir;
    private static boolean isLunchOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cacheDir = getCacheDir();

        calendar = new GregorianCalendar();

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);

        dayOfWeek = getResources().getStringArray(R.array.dayOfWeek);
        String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        isLunchOnly = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lunch_only", false);

        TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
        textView.setText(String.format("%d. %d. %d. (%s)", year, month, day, date));

        loadData(new Handler(), true);

        swipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh_color);

        String[] drawerItems = getResources().getStringArray(R.array.drawerItems);
        drawerList = (ListView) this.findViewById(R.id.drawer_list);

        drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, drawerItems));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) this.findViewById(R.id.fab);
        fab.setOnClickListener(fabClickListener);
    }

    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            boolean isChanged = MainActivity.this.year != year ||
                    MainActivity.this.month - 1 != monthOfYear || MainActivity.this.day != dayOfMonth;
            calendar.set(year, monthOfYear, dayOfMonth);

            MainActivity.this.year = calendar.get(Calendar.YEAR);
            MainActivity.this.month = calendar.get(Calendar.MONTH) + 1;
            MainActivity.this.day = calendar.get(Calendar.DAY_OF_MONTH);
            String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

            String msg = String.format("%d. %d. %d. (%s)", year, month, day, date);

            TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
            textView.setText(msg);

            if (!isChanged) {
                swipeRefreshLayout.setRefreshing(true);
                refreshListener.onRefresh();
                return;
            }

            loadData(new Handler(), false);

            if (toast != null) toast.cancel();
            toast = Toast.makeText(MainActivity.this, msg + " " + getString(R.string.msg_set_date), Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DatePickerDialog.newInstance(dateSetListener, year, month - 1, day).show(getFragmentManager(), "Datepickerdialog");
        }
    };

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            swipeRefreshLayout.setRefreshing(true);

            final Handler handler = new Handler();
            final Handler handler2 = new Handler();

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    loadData(handler, false);

                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);

                            if (toast != null) toast.cancel();
                            toast = Toast.makeText(MainActivity.this, getString(R.string.msg_refresh), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }, 1000);
                }
            });

            thread.start();
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_yesterday) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH) + 1;
            day = calendar.get(Calendar.DAY_OF_MONTH);
            String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

            String msg = String.format("%d. %d. %d. (%s)", year, month, day, date);

            TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
            textView.setText(msg);

            final Handler handler = new Handler();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    loadData(handler, true);
                }
            });

            thread.start();

            return true;
        } else if (id == R.id.action_tomorrow) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH) + 1;
            day = calendar.get(Calendar.DAY_OF_MONTH);
            String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

            String msg = String.format("%d. %d. %d. (%s)", year, month, day, date);

            TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
            textView.setText(msg);

            final Handler handler = new Handler();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    loadData(handler, true);
                }
            });

            thread.start();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        } else {
            super.onBackPressed();
        }
    }

    private void loadData(Handler handler, final boolean playAnim) {
        if (isLoading) return;

        isLoading = true;

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        final TextView lunchView = (TextView) this.findViewById(R.id.lunch);
        final TextView dinnerView = (TextView) this.findViewById(R.id.dinner);

        if (playAnim) {
            final String loading = getString(R.string.loading);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    lunchView.setText(loading);
                    dinnerView.setText((isLunchOnly) ? "-" : loading);
                }
            });
        }

        final String[] cache = loadCache(year, month, day);
        if (cache == null || cache.length < 2) {
            final Object data = parseHtml(year, month, day);

            if (data instanceof Integer) {
                int val = (int) data;

                if (val == 0) {
                    String text = getString(R.string.error_connection);

                    lunchView.setText(text);
                    dinnerView.setText(text);

                    isLoading = false;
                } else if (val == 1) {
                    final String text = getString(R.string.error_no_data);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            lunchView.setText(text);
                            dinnerView.setText((isLunchOnly) ? "-" : text);

                            isLoading = false;
                        }
                    });
                }
            } else {
                final String[] arr = (String[]) data;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lunchView.setText(arr[0]);
                        dinnerView.setText((isLunchOnly) ? "-" : arr[1]);
                        updateTextSize(lunchView);
                        updateTextSize(dinnerView);

                        if (playAnim) {
                            Animation anim = new AlphaAnimation(0.0F, 1.0F);
                            anim.setDuration(1000);
                            lunchView.setAnimation(anim);
                            dinnerView.setAnimation(anim);
                        }

                        isLoading = false;
                    }
                });

                saveCache(year, month, day, arr);
            }
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    lunchView.setText(cache[0]);
                    dinnerView.setText((isLunchOnly) ? "-" : cache[1]);
                    updateTextSize(lunchView);
                    updateTextSize(dinnerView);

                    if (playAnim) {
                        Animation anim = new AlphaAnimation(0.0F, 1.0F);
                        anim.setDuration(1000);
                        lunchView.setAnimation(anim);
                        dinnerView.setAnimation(anim);
                    }

                    isLoading = false;
                }
            });
        }
    }

    private void updateTextSize(TextView view) {
        int lineCount = 1;

        int i;
        String var = view.getText().toString();
        while ((i = var.indexOf("\n")) != -1 && var.length() > i + 2) {
            var = var.substring(i + 2);
            lineCount ++;
        }

        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, lineCount >= 6 ? 14.0F : 16.0F);
    }

    public static Object parseHtml(int year, int month, int day) {
        String url = "http://www.cham.hs.kr/tablemenu/menu.do?y=" + year + "&m=" + month + "&d=" + day;
        Document doc;

        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            return 0;
        }

        String[] value = doc.body().html().split("<div class=\"style1\" style=\"padding-top:15px;\">");

        for (int i = 0; i < value.length; i ++) {
            String[] split = value[i].split("</div><br> ");
            value[i] = (split.length < 2) ? split[0] : split[1];
        }

        try {
            String lunch = value[1].split(" </td>")[0].replaceAll(",", "\n");
            String dinner = value[2].split(" </td>")[0].replaceAll(",", "\n");

            return new String[]{lunch, dinner};
        } catch (ArrayIndexOutOfBoundsException ex) {
            return 1;
        }
    }

    public static void saveCache(int year, int month, int day, String[] value) {
        File file;
        FileWriter writer;

        try {
            file = new File(cacheDir, String.format("%d_%d_%d", year, month, day) + ".tmp");
            if (!file.createNewFile()) {
                return;
            }

            writer = new FileWriter(file);
            writer.write(value[0] + "|" + value[1]);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] loadCache(int year, int month, int day) {
        File file = new File(cacheDir, String.format("%d_%d_%d", year, month, day) + ".tmp");
        if (!file.exists()) return null;

        StringBuilder builder = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString().split("\\|");
    }

    public static boolean clearCache() {
        for (File file : cacheDir.listFiles()) {
            if (!file.delete()) {
                return false;
            }
        }

        return true;
    }

    public static void setLunchOnly(boolean value) {
        isLunchOnly = value;
    }

    private class DrawerItemClickListener implements ListViewCompat.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }

        private void selectItem(int position) {
            Fragment fragment = new Fragment();

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_layout, fragment)
                    .commit();

            drawerList.setItemChecked(-1, true);

            Intent intent;
            Uri uri;
            switch (position) {
                case 0:
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.send_message));
                    intent.setType("text/plain");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.send_to)));
                    break;
                case 1:
                    intent = new Intent(Intent.ACTION_VIEW);
                    uri = Uri.parse("market://details?id=kr.efe.choongamlunch");
                    intent.setData(uri);
                    startActivity(intent);
                    break;
                case 2:
                    intent = new Intent(Intent.ACTION_VIEW);
                    uri = Uri.parse("http://www.cham.hs.kr/");
                    intent.setData(uri);
                    startActivity(intent);
                    break;
                case 3:
                    intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    }
}
