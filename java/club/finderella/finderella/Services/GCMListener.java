package club.finderella.finderella.Services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import club.finderella.finderella.POJO.PokeWrapper;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.MainActivity;
import club.finderella.finderella.R;
import de.greenrobot.event.EventBus;

public class GCMListener extends GcmListenerService {

    private static MyDBHandler db;
    private static SQLiteDatabase dbObj;
    private Bundle pokeData;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i("mTag", "Notif received : " + data.toString());

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();

        if (from.startsWith("/topics/")) {
        } else {
            // normal downstream message.
            this.pokeData = data;    // {"name";"",  "account_id":""}
            // add poke to data base
            // send notification to user
            sendNotification(pokeData.getString("name"));
            ContentValues values = new ContentValues();   // inserting into pokes table
            values.put("account_id", pokeData.getInt("account_id"));
            dbObj.insert("pokes", null, values);

            // send event bus message to pokesFrag
            EventBus.getDefault().post(new PokeWrapper(pokeData.getInt("account_id")));
        }
    }

    private void sendNotification(final String name) {
        new AsyncTask<Void, Void, Void>() {
            NotificationCompat.Builder mBuilder;
            NotificationManager mNotificationManager;

            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.launch_icon);
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            @Override
            protected Void doInBackground(Void... params) {
                Cursor c = dbObj.rawQuery("SELECT * FROM pokes WHERE read=0", null);
                if (c.getCount() == 0) {
                    mBuilder = new NotificationCompat.Builder(getApplicationContext())
                            .setLargeIcon(largeIcon)
                            .setSmallIcon(R.drawable.launch_icon)
                            .setSound(soundUri)
                            .setTicker("New Pokes")
                            .setContentTitle("Pokes")
                            .setContentText(name + " poked you recently")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);
                } else {
                    mBuilder = new NotificationCompat.Builder(getApplicationContext())
                            .setLargeIcon(largeIcon)
                            .setSmallIcon(R.drawable.launch_icon)
                            .setSound(soundUri)
                            .setTicker("New Pokes")
                            .setContentTitle("Pokes")
                            .setContentText(name + " and " + c.getCount() + " others poked you recently")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);
                }


                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                mBuilder.setContentIntent(resultPendingIntent);
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mNotificationManager.notify(2, mBuilder.build());
            }

        }.execute();

    }


}
