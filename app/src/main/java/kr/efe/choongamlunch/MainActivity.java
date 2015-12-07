package kr.efe.choongamlunch;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
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

import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity {
    private CacheManager cacheManager;

    private Toast toast;
    private Calendar calendar;
    private int year, month, day;
    private String[] dayOfWeek;
    private boolean isLoading = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private static boolean isLunchOnly = false;

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cacheManager = new CacheManager(getCacheDir());

        calendar = new GregorianCalendar();

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);

        dayOfWeek = getResources().getStringArray(R.array.dayOfWeek);
        String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        isLunchOnly = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lunch_only", false);

        swipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);
        swipeRefreshLayout.setColorSchemeResources(R.color.refresh_color);

        TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
        textView.setText(String.format("%d. %d. %d. (%s)", year, month, day, date));

        updateData(false, false);

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

            if (!isChanged) {
                swipeRefreshLayout.setRefreshing(true);
                refreshListener.onRefresh();
                return;
            }

            changeDateText(true);
            updateData(false, false);
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

            updateData(true, true);
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

            changeDateText(false);
            updateData(false, false);

            return true;
        } else if (id == R.id.action_tomorrow) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH) + 1;
            day = calendar.get(Calendar.DAY_OF_MONTH);

            changeDateText(false);
            updateData(false, false);

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

    public void changeDateText(boolean showToast) {
        String date = dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        String msg = String.format("%d. %d. %d. (%s)", year, month, day, date);

        TextView textView = (TextView) MainActivity.this.findViewById(R.id.calendar_text);
        textView.setText(msg);

        if (showToast) {
            if (toast != null)
                toast.cancel();
            toast = Toast.makeText(MainActivity.this, msg + " " + getString(R.string.msg_set_date), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void updateData(final boolean reload, final boolean hasDelay) {
        final Handler handler0 = new Handler();
        final Handler handler1 = new Handler();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                changeDietText(reload, hasDelay, handler0);

                handler1.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);

                            if (toast != null)
                                toast.cancel();
                            toast = Toast.makeText(MainActivity.this, getString(R.string.msg_refresh), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }, 1000);
            }
        });

        thread.start();
    }

    private void changeDietText(boolean reload, boolean hasDelay, Handler handler) {
        if (isLoading) return;

        final TextView lunchView = (TextView) this.findViewById(R.id.lunch);
        final TextView dinnerView = (TextView) this.findViewById(R.id.dinner);

        isLoading = true;

        MealData data = getCacheManager().loadCache(year, month, day);
        final String lunchText, dinnerText;

        if (data == null || reload) {
            // 저장된 캐시가 없을 경우 학교 홈페이지에서 HTML을 파싱한다.

            // 데이터 로드 중임을 텍스트로 표시한다.
            final String loadingText = getString(R.string.loading);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    lunchView.setText(loadingText);
                    dinnerView.setText((isLunchOnly) ? "-" : loadingText);
                }
            });

            data = getCacheManager().parseHtml(year, month, day);

            if (data.getState() == MealData.DataState.NOT_CONNECTED) {
                String text = getString(R.string.error_connection);

                lunchText = text;
                dinnerText = text;
            } else if (data.getState() == MealData.DataState.BLANK) {
                String text = getString(R.string.error_no_data);

                lunchText = text;
                dinnerText = text;
            } else {
                lunchText = data.getLunchText();
                dinnerText = data.getDinnerText();

                getCacheManager().saveCache(year, month, day, data);
            }
        } else {
            // 저장된 캐시가 있을 경우 반영한다.
            lunchText = data.getLunchText();
            dinnerText = data.getDinnerText();
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lunchView.setText(lunchText);
                dinnerView.setText((isLunchOnly) ? "-" : dinnerText);

                updateTextSize(lunchView);
                updateTextSize(dinnerView);

                Animation anim = new AlphaAnimation(0.0F, 1.0F);
                anim.setDuration(1000);

                lunchView.setAnimation(anim);
                dinnerView.setAnimation(anim);

                isLoading = false;
            }
        }, hasDelay ? 1000 : 0);
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
