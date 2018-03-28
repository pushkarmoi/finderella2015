package club.finderella.finderella.Services;


import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import club.finderella.finderella.CustomClasses.Node;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.MainActivity;
import club.finderella.finderella.R;


public class BluetoothService extends IntentService {

    private BluetoothAdapter mAdapter;
    private boolean contService;

    private static int successive_zeros = 0;
    private static int scan_count = 0;  // for scan group 0 to 5
    private static int dev_num = 0;

    private static int total = 0; // Total no. of devices found yet...
    private static int interval = 30;    // in seconds

    private static boolean power_save = true;
    private static boolean scanning = false;

    private BluetoothDevice mDevice;
    private Node head;
    private Node temp;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;

    private SharedPreferences mPref;
    private android.content.SharedPreferences.Editor mEditor;

    private final TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {

            if (!scanning) {
                scanning = true;
                mAdapter.startDiscovery();
            }
        }
    };

    private static Timer mTimer = new Timer();


    public BluetoothService() {
        super("BluetoothService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAdapter == null)
            contService = false;
        else {
            contService = true;

            head = null;

            db = new MyDBHandler(this, null, null, 1);
            dbObj = db.getWritableDatabase();

            mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
            power_save = mPref.getBoolean("do_power_save", true);

            IntentFilter mFilter;
            mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

            registerReceiver(bluetoothDiscovery, mFilter);
            registerReceiver(serviceControl, new IntentFilter("bluetooth_service_control"));
            registerReceiver(powerSaveControl, new IntentFilter("power_save_control"));
            registerReceiver(bluetoothControl, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));


            Log.i("mTag", "Starting bluetooth service");
        }

    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (contService)
            controller();

        while (contService) {
            synchronized (this) {
                try {
                    wait(10000);     // 10 seconds
                } catch (Exception e) {

                }
            }// end of synchronized
        }// end of while

        mTimer.cancel();
    }

    private void setInterval(int num) {

        if (successive_zeros <= 10) {
            if (num != 0)
                successive_zeros = 0;

            interval = 30;

        } else if (successive_zeros <= 20) {
            if (num != 0) {
                successive_zeros = 0;
                interval = 30;
            } else
                interval = 150;


        } else if (successive_zeros <= 30) {
            if (num != 0) {
                successive_zeros = 11;
                interval = 150;
            } else
                interval = 600;

        } else {
            if (num != 0) {
                successive_zeros = 21;
                interval = 600;
            } else
                interval = 900;
        }
    }   // takes in parameter that shows whether devices were found for a scan group or not

    private void controller() {
        mTimer.cancel();

        mTimer = new Timer();
        if (interval == 30)
            mTimer.scheduleAtFixedRate(mTimerTask, 0, (long) (interval * 1000));
        else
            mTimer.scheduleAtFixedRate(mTimerTask, (long) (interval * 1000), (long) (interval * 1000));

    }   // sets the timer

    private BroadcastReceiver bluetoothDiscovery = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String address = "";
            String query;

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                if (contService) {

                    if (scan_count < 5) {
                        Log.i("mTag", "starting new discovery...");
                        mAdapter.startDiscovery();          // scan_count is incremented in onDiscoveryStarted
                    } else {
                        Log.i("mTag", "Scan group finished");

                        if (total >= 20) {
                            Log.i("mTag", "More than 20 unique devices, launching Cleanser and refreshing tree...");
                            head = null; // after 20 additions
                            total = 0;
                            startService(new Intent(BluetoothService.this, CleanseService.class));
                        }
                        scan_count = 0;

                        if (dev_num == 0)
                            successive_zeros++;


                        if (power_save)
                            setInterval(dev_num);   //sets interval as well as successive zero's
                        else {
                            Log.i("mTag", "NOT in power save mode..");
                            interval = 30;
                            if (successive_zeros >= 11)
                                controller();
                        }

                        dev_num = 0;


                        if ((successive_zeros == 11 || successive_zeros == 21 || successive_zeros == 31) && power_save)   // because interval needs to be changed
                            controller();

                        scanning = false;   // signifies scan group has ended
                    }

                }

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // search for that device in prev and common
                address = formatMac(mDevice.getAddress());

                if (address != null) {
                    temp = new Node(address, address.substring(9));

                    if (!Node.Search(head, temp)) {
                        // is not present
                        // Add to tree
                        if (head == null)
                            head = Node.Insert(head, temp);
                        else
                            Node.Insert(head, temp);

                        dev_num++;
                        total++;
                        query = "INSERT INTO mac_data(address) VALUES(\"" + address + "\")";
                        dbObj.execSQL(query);
                        Log.i("mTag", "new device found, MAC:" + address);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                scan_count++;
            }


        }// end of onReceive
    };

    private BroadcastReceiver serviceControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            contService = false;
            mTimer.cancel();
        }
    };

    private BroadcastReceiver bluetoothControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int result;
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                result = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if ((result == BluetoothAdapter.STATE_TURNING_OFF) || (result == BluetoothAdapter.STATE_OFF)) {

                    if (contService) {
                        contService = false;
                        sendNotifs(2);
                        // MainActivity handles changing finder icon
                    }
                }
            }


        }
    };

    private BroadcastReceiver powerSaveControl = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                power_save = (intent.getExtras()).getBoolean("do_power_save");
                Log.i("mTag", "Intent received in powerSaveControl Receiver (bluetooth service), do_power_save = " + power_save);
            }
        }
    };


    private void sendNotifs(int code) {

        NotificationCompat.Builder mBuilder;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.launch_icon);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        switch (code) {
            case 1:
                // time out // or app was killed
                mBuilder = new NotificationCompat.Builder(this)
                        .setLargeIcon(largeIcon)
                        .setSmallIcon(R.drawable.launch_icon)
                        .setTicker("Finder stopped")
                        .setSound(soundUri)
                        .setContentTitle("Finder stopped")
                        .setContentText("Finderella was terminated by the system.")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true);
                break;
            case 2:
                // system turned off bluetooth
                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.launch_icon)
                        .setLargeIcon(largeIcon)
                        .setTicker("Finder stopped")
                        .setContentTitle("Finder stopped")
                        .setContentText("Bluetooth was turned off")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true);
                break;
            default:
                mBuilder = null;
                break;
        }

        // Creates an explicit intent for an Activity in your app
        if (mBuilder != null) {
            Intent resultIntent = new Intent(this, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, mBuilder.build());
        }

    }   // change icon

    private String formatMac(String x) {
        String toRet = "";
        String error = "";

        int i = 0;
        while (i < x.length()) {
            if (Character.isDigit(x.charAt(i)) || Character.isLetter(x.charAt(i)))
                toRet += x.charAt(i);
            i++;
        }

        if (toRet.length() != 12 && toRet.length() != 16) {
            i = 0;
            while (i < toRet.length()) {
                if (toRet.charAt(i) != '0')
                    error += toRet.charAt(i);
                i++;
            }

            if (error.length() == 12 || error.length() == 16)
                return error;
            else
                return null;
        } // if there are some extra 0's added

        return toRet;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (contService)
            sendNotifs(1);

        unregisterReceiver(bluetoothDiscovery);
        unregisterReceiver(serviceControl);
        unregisterReceiver(powerSaveControl);
        unregisterReceiver(bluetoothControl);

        Log.i("mTag", "Stopping bluetooth service");

    }
}
