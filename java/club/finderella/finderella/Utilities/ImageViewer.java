package club.finderella.finderella.Utilities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.R;

public class ImageViewer extends Activity {

    private int code;

    private MyDBHandler db;
    private SQLiteDatabase dbObj;

    private ImageView mImage, mClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_imageviewer);
        mImage = (ImageView) findViewById(R.id.mImage);
        mClose = (ImageView) findViewById(R.id.mClose);

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();

        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            code = bundle.getInt("code");       // 1 for profile pic, 2 for exp image

            new AsyncTask<Void, Void, Void>() {
                File f;
                Bitmap b;

                @Override
                protected Void doInBackground(Void... params) {
                    String query = "SELECT * FROM user_media WHERE 1=1";
                    Cursor c = dbObj.rawQuery(query, null);

                    try {

                        while (c.moveToNext()) {

                            if (code == 1) {
                                if (c.getString(c.getColumnIndex("profile_image")) == null) {
                                    f = null;
                                } else {
                                    f = new File(c.getString(c.getColumnIndex("profile_image")), "profile_image.jpg");
                                }
                            }

                            if (code == 2) {
                                if (c.getString(c.getColumnIndex("exp_image")) == null) {
                                    f = null;
                                } else {
                                    f = new File(c.getString(c.getColumnIndex("exp_image")), "exp_image.jpg");
                                }
                            }

                            if (f != null)
                                b = BitmapFactory.decodeStream(new FileInputStream(f));
                            else
                                b = null;

                            break;
                        }

                    } catch (FileNotFoundException e) {
                        Log.i("images", "No Picture at given path, iMAGEVIEWER:");
                    }

                    c.close();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (b != null)
                        mImage.setImageBitmap(b);
                }
            }.execute();    // loads picture according to code
        } else {
            Log.i("images", "Bad intent sent" + bundle.toString());
            Toast.makeText(this, "There was some error", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
