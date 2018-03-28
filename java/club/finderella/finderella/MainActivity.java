package club.finderella.finderella;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.bottomsheet.BottomSheet;

import club.finderella.finderella.Coach.Coach;
import club.finderella.finderella.Helpers.FragmentLifecycle;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Myadapters.MyPagerAdapter;
import club.finderella.finderella.POJO.AppVersion;
import club.finderella.finderella.POJO.FragmentRefrences;
import club.finderella.finderella.Services.BluetoothService;
import club.finderella.finderella.Services.GCMRegistrar;
import club.finderella.finderella.Services.MediaSync;
import club.finderella.finderella.Services.TextSync;
import club.finderella.finderella.Utilities.BlockList;
import club.finderella.finderella.Utilities.Help;
import de.hdodenhof.circleimageview.CircleImageView;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;


public class MainActivity extends AppCompatActivity   {
    private ViewPager mPager;
    private MyPagerAdapter mPageAdapter;
    private Toolbar mToolBar;

    private MaterialTabHost mTabs;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MaterialTabListener mMaterialTabListener;
    private NavigationView navigation_view;
    private View nav_header;

    private View mSnackBar;

    private static boolean bluetooth_service_state;

    private static CircleImageView finderCircle;
    private static TextView finderText;
    private static boolean do_power_save = false;

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    private final int BLUETOOTH_ACTIVITY_CODE = 15;




    public interface MonitorTelView {
        public boolean getTelViewVisibility();
    }

    private static MyIntroFrag myIntroFragRef = null;
    private static FragmentRefrences mReferences = new FragmentRefrences();


    public static MonitorTelView mMonitorTelViewInstance = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CharSequence[] titles;

        super.onCreate(savedInstanceState);
        Log.i("workflow", "*********** ON CREATE EXECUTED FOR MAIN ACTIVITY ***************");
        setContentView(R.layout.layout_main);

        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();


        new AsyncTask<Void, Void, Void>() {
            boolean gcm_sync = false;
            boolean text_sync = false;
            boolean media_sync = false;

            @Override
            protected Void doInBackground(Void... params) {
                if ((mPref.getInt("gcm_sync_required", 1) == 1) || (mPref.getInt("app_version", AppVersion.APP_VERSION) != AppVersion.APP_VERSION)) {
                    gcm_sync = true;
                } else
                    gcm_sync = false;

                MyDBHandler db = new MyDBHandler(MainActivity.this, null, null, 1);
                SQLiteDatabase dbObj = db.getWritableDatabase();
                Cursor c = dbObj.rawQuery("SELECT * FROM server_sync", null);

                while (c.moveToNext()) {
                    if (c.getInt(c.getColumnIndex("text_sync")) == 1) {
                        text_sync = true;
                    } else
                        text_sync = false;


                    if ((c.getInt(c.getColumnIndex("profile_pic_sync")) == 1) || (c.getInt(c.getColumnIndex("exp_sync")) == 1)) {
                        media_sync = true;
                    } else
                        media_sync = false;


                    break;
                }
                c.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (gcm_sync) {
                    startService(new Intent(MainActivity.this, GCMRegistrar.class));
                    Log.i("mTag", "doing gcm_sync");
                }
                if (text_sync) {
                    startService(new Intent(MainActivity.this, TextSync.class));
                    Log.i("mTag", "doing text_sync");
                }
                if (media_sync) {
                    startService(new Intent(MainActivity.this, MediaSync.class));
                    Log.i("mTag", "doing media sync");
                }

            }

        }.execute();    // for gcm, text, media syncing

        titles = new CharSequence[]{"Introductions", "Bookmarks", "Pokes", "My Introduction"};

        //Initialization
        mPager = (ViewPager) findViewById(R.id.mPager);
        mPageAdapter = new MyPagerAdapter(getSupportFragmentManager(), 4, titles, mPager.getId());
        mPager.setOffscreenPageLimit(3);


        mToolBar = (Toolbar) findViewById(R.id.mToolBar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("Introductions");

        mPager.setAdapter(mPageAdapter);
        mTabs = (MaterialTabHost) findViewById(R.id.mTabs);


        navigation_view = (NavigationView) findViewById(R.id.navigation_view);
        nav_header = navigation_view.inflateHeaderView(R.layout.navbar_header);     // inflates nav_header and sets it as header for side bar

        bluetooth_service_state = isMyServiceRunning();

        finderCircle = (CircleImageView) (nav_header.findViewById(R.id.finderCircle));
        finderText = (TextView) (nav_header.findViewById(R.id.finderText));

        do_power_save = mPref.getBoolean("do_power_save", true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.mDrawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolBar, R.string.DrawerOpened, R.string.DrawerClosed) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };


        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mSnackBar = findViewById(R.id.mSnackbar);
        (mSnackBar.findViewById(R.id.noInternetClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSnackBar.setVisibility(View.GONE);
            }
        });


        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            mSnackBar.setVisibility(View.GONE);
        } else {
            // not connected
            mSnackBar.setVisibility(View.VISIBLE);
        }


        registerReceiver(mInternetChecker, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


        registerReceiver(mBluetoothChecker, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));


        registerReceiver(mFinderStateChecker, new IntentFilter("finder_state"));

        mMaterialTabListener = new MaterialTabListener() {
            @Override
            public void onTabSelected(MaterialTab materialTab) {
                mPager.setCurrentItem(materialTab.getPosition());
            }

            @Override
            public void onTabReselected(MaterialTab materialTab) {
                // Scroll to top

            }

            @Override
            public void onTabUnselected(MaterialTab materialTab) {
            }
        };

        // insert all tabs from pagerAdapter data
        for (int i = 0; i < mPageAdapter.getCount(); i++) {
            // getIcon has been defined
            mTabs.addTab(mTabs.newTab().setIcon(getIcon(i)).setTabListener(mMaterialTabListener));
        }


        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            int currentPosition = 0;
            FragmentLifecycle fragmentToShow;
            FragmentLifecycle fragmentToHide;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mTabs.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageSelected(int newposition) {
                getSupportActionBar().setTitle(mPageAdapter.getPageTitle(newposition));

                fragmentToShow = (FragmentLifecycle) mPageAdapter.getFragmentForPosition(newposition);

                if (fragmentToShow == null) {
                    mPageAdapter.getItem(newposition);
                    fragmentToShow = (FragmentLifecycle) mPageAdapter.getFragmentForPosition(newposition);
                }


                fragmentToShow.onResumeFragment();

                //fragmentToHide = (FragmentLifecycle) mPageAdapter.getFragmentForPosition(currentPosition);
                //fragmentToHide.onPauseFragment();

                currentPosition = newposition;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        // Navigation View and OnClick Listener
        navigation_view = (NavigationView) findViewById(R.id.navigation_view);
        navigation_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                //Checking if the item is in checked state or not, if not make it in checked state\
                if (menuItem.isChecked())
                    menuItem.setChecked(false);
                else
                    menuItem.setChecked(true);

                switch (menuItem.getItemId()) {
                    case R.id.tutorial:
                        mDrawerLayout.closeDrawers();
                        Intent mIntent = new Intent(MainActivity.this, Coach.class);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        //mIntent.putExtra("go_to", 2);
                        startActivity(mIntent);
                        return true;
                    case R.id.help:
                        mDrawerLayout.closeDrawers();
                        Intent m = new Intent(MainActivity.this, Help.class);
                        m.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(m);
                        return true;
                    case R.id.blocklist:
                        mDrawerLayout.closeDrawers();
                        Intent m1 = new Intent(MainActivity.this, BlockList.class);
                        m1.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(m1);
                        return true;
                    default:
                        return true;
                }
            }
        });


        // NAV HEADER LISTENER
        nav_header.findViewById(R.id.finderCircle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth_service_state) {
                    // stop service
                    // first ask for confirmation with dialog sheet
                    new BottomSheet.Builder(MainActivity.this, R.style.MyBottomSheetTheme).title("Turn Finder Off? No data will be collected").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case R.id.yes_delete:
                                    sendBroadcast(new Intent("bluetooth_service_control"));
                                    bluetooth_service_state = false;
                                    showFinderOff();
                                    dialog.dismiss();
                                    break;
                                case R.id.no_delete:
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    }).show();

                } else {
                    // START SERVICE
                    bluetooth_service_state = true;
                    Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    mIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                    startActivityForResult(mIntent, BLUETOOTH_ACTIVITY_CODE);
                }

            }
        });
        nav_header.findViewById(R.id.finderText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth_service_state) {
                    // toggle
                    if (do_power_save) {
                        finderText.setText("Power Save Off");
                        do_power_save = false;
                        mEditor.putBoolean("do_power_save", false);
                        mEditor.commit();
                        Intent bIntent = new Intent("power_save_control");
                        bIntent.putExtra("do_power_save", false);
                        sendBroadcast(bIntent);

                    } else {
                        finderText.setText("Power Save On");
                        do_power_save = true;
                        mEditor.putBoolean("do_power_save", true);
                        mEditor.commit();
                        Intent bIntent = new Intent("power_save_control");
                        bIntent.putExtra("do_power_save", true);
                        sendBroadcast(bIntent);
                    }


                }
            }
        });


        if (bluetooth_service_state) {
            showFinderOn();
        } else {
            showFinderOff();
            mDrawerLayout.openDrawer(GravityCompat.START);
        }

        mReferences.setup(mPageAdapter);


    }//  end of onCreate



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mInternetChecker);
        unregisterReceiver(mBluetoothChecker);
        unregisterReceiver(mFinderStateChecker);
    }

    private Drawable getIcon(int x) {
        switch (x) {
            case 0:
                return getResources().getDrawable(R.drawable.introductions_icon);

            case 1:
                return getResources().getDrawable(R.drawable.bookmarks_icon);
            case 2:
                return getResources().getDrawable(R.drawable.pokes_icon);
            case 3:
                return getResources().getDrawable(R.drawable.myintro_icon);
            default:
                return null;
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("club.finderella.finderella.Services.BluetoothService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BLUETOOTH_ACTIVITY_CODE) {
            if (resultCode != RESULT_CANCELED) {
                showFinderOn();
                startService(new Intent(this, BluetoothService.class));
            } else {
                Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show();
            }

        } else {

            if (mReferences.myIntroFrag == null)
                mReferences.setup(mPageAdapter);

            mReferences.myIntroFrag.onActivityResult(requestCode, resultCode, data);
        }


    }

    private final BroadcastReceiver mInternetChecker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    mSnackBar.setVisibility(View.GONE);
                } else {
                    // not connected
                    mSnackBar.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private final BroadcastReceiver mBluetoothChecker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int result;
            if (intent.getAction() == BluetoothAdapter.ACTION_STATE_CHANGED) {
                result = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if ((result == BluetoothAdapter.STATE_TURNING_OFF) || (result == BluetoothAdapter.STATE_OFF)) {
                    showFinderOff();
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    bluetooth_service_state = false;
                }
            }
        }
    };

    private final BroadcastReceiver mFinderStateChecker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showFinderOff();
            bluetooth_service_state = false;
        }
    };

    private void showFinderOff() {
        finderCircle.setBorderColor(Color.BLACK);
        finderCircle.setImageDrawable(getResources().getDrawable(R.drawable.off_icon));
        finderText.setText("Tap to turn on Finder");
    }

    private void showFinderOn() {
        finderCircle.setBorderColor(Color.BLACK);
        finderCircle.setImageDrawable(getResources().getDrawable(R.drawable.on_icon));
        // Show power on/off options
        if (do_power_save) {
            finderText.setText("Power Save On");
        } else {
            finderText.setText("Power Save Off");
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.share_icon:
                new BottomSheet.Builder(this, R.style.MyBottomSheetTheme).title("Invite friends").sheet(R.menu.share).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.whatsapp_share:
                                // OPEN API
                                dialog.dismiss();

                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, "Meet everyone around you, just with Bluetooth! Download Finderella for android from www.finderella.club");
                                sendIntent.setType("text/plain");
                                sendIntent.setPackage("com.whatsapp");
                                startActivity(sendIntent);

                                break;
                        }
                    }
                }).show();

                break;


        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        // returns true if telview is visible
        if (mMonitorTelViewInstance == null) {

            if (mReferences.myIntroFrag == null)
                mReferences.setup(mPageAdapter);

            mMonitorTelViewInstance = (MonitorTelView) mReferences.myIntroFrag;
        }

        if ((!mMonitorTelViewInstance.getTelViewVisibility()))
            super.onBackPressed();
    }


}
