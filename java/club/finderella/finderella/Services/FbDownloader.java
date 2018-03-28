package club.finderella.finderella.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import club.finderella.finderella.Helpers.MyDBHandler;

public class FbDownloader extends IntentService {

    public FbDownloader() {
        super("FbDownloader");
    }

    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private static String mUrl = null;
    private Bitmap b;


    @Override
    public void onCreate() {
        super.onCreate();
        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
    }

    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mUrl = intent.getExtras().getString("fbProfilePicUrl");
            Log.i("images", "mUrl in fb service=" + mUrl);
        } else {
            Log.i("images", "no intent");
        }

        Log.i("images", "on start command, fbDownloader!");
        return START_REDELIVER_INTENT;
    }
    */

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mUrl = intent.getExtras().getString("fbProfilePicUrl");
            Log.i("images", "mUrl in fb service=" + mUrl);
        } else {
            Log.i("images", "no intent");
        }

        Log.i("images", "on handel intent, fbDownloader!");



        if (mUrl != null) {
            Log.i("images", "starting download..");
            URL img_value;
            try {
                img_value = new URL(mUrl);
                b = BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
                Log.i("images", "downloaded....");
            } catch (Exception e) {
                Log.i("images", "exception " + e);
            }


            Intent mIntent = new Intent("fbDownload");
            if (b != null) {
                saveFbProfileImageToInternalStorage(b);
                mIntent.putExtra("success", true);
                Log.i("images", "success, fbdownloader");
            } else {
                mIntent.putExtra("success", false);
                Log.i("images", "bitmap is null");
            }
            sendBroadcast(mIntent);
        }
    }

    private String saveFbProfileImageToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "fb_temp.jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();
        } catch (Exception e) {
        }

        Log.i("images", "FB Image saved at:" + directory.getAbsolutePath());
        return directory.getAbsolutePath();
    }   // only for FB profileImage
}
