package club.finderella.finderella.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.IOException;

import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;


public class MediaSync extends IntentService {

    private static MyDBHandler db;
    private static SQLiteDatabase dbObj;

    private static int user_id;
    private static String password;
    private int type;

    private File mFile;

    private HttpClient mClient;
    private HttpPost mPost;
    private MultipartEntityBuilder mBuilder;
    private HttpEntity mEntity;
    private RequestQueue queue;

    public MediaSync() {
        super("MediaSync");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
        queue = MySingleton.getInstance(this).getRequestQueue();


        String query = "SELECT user_id,password FROM user_data WHERE 1=1";
        Cursor c = dbObj.rawQuery(query, null);
        while (c.moveToNext()) {
            user_id = c.getInt(c.getColumnIndex("user_id"));
            password = c.getString(c.getColumnIndex("password"));

            break;
        }
        c.close();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // network connected

            String query = "SELECT * FROM server_sync WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);
            Cursor c1;

            c.moveToNext();

            if (c.getInt(c.getColumnIndex("profile_pic_sync")) == 1) {
                type = 0;

                c1 = dbObj.rawQuery("SELECT profile_image FROM user_media", null);
                while (c1.moveToNext()) {

                    if (c1.getString(c1.getColumnIndex("profile_image")) == null) {
                        // remove
                        mFile = null;
                        media_remove(0);
                    } else {
                        try {
                            mFile = new File(c1.getString(c1.getColumnIndex("profile_image")), "profile_image.jpg");

                            if (media_add()) {
                                dbObj.execSQL("UPDATE server_sync SET profile_pic_sync =0 WHERE 1=1");
                            } else {
                                dbObj.execSQL("UPDATE server_sync SET profile_pic_sync =0 WHERE 1=1");
                            }

                        } catch (SQLException s) {
                            Log.i("images", "SQL Exception in media sync service (profile_pic), " + s.toString());
                        }
                    }

                    break;
                }
                c1.close();
            }


            if (c.getInt(c.getColumnIndex("exp_sync")) == 1) {
                type = 1;

                c1 = dbObj.rawQuery("SELECT exp_image FROM user_media", null);
                while (c1.moveToNext()) {
                    if (c1.getString(c1.getColumnIndex("exp_image")) == null) {
                        // remove
                        mFile = null;
                        media_remove(1);

                    } else {
                        try {
                            mFile = new File(c1.getString(c1.getColumnIndex("exp_image")), "exp_image.jpg");
                            // no need to compress as already compressed ;

                            if (media_add()) {
                                dbObj.execSQL("UPDATE server_sync SET exp_sync =0 WHERE 1=1");
                            } else {
                                dbObj.execSQL("UPDATE server_sync SET exp_sync =1 WHERE 1=1");
                            }

                        } catch (SQLException ff) {
                            Log.i("images", "SQL Exception in media sync service (exp), " + ff.toString());
                        }
                    }

                    break;
                }

                c1.close();
            }

            c.close();
        }
    }

    private boolean media_add() {

        try {
            mClient = new DefaultHttpClient();
            mPost = new HttpPost(MyUrlData.media_sync_add);
            mBuilder = MultipartEntityBuilder.create();
            mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            mBuilder.addTextBody("user_id", String.valueOf(user_id));
            mBuilder.addTextBody("password", password);
            mBuilder.addTextBody("type", String.valueOf(type));



            mBuilder.addBinaryBody("upload_file", mFile);
            mEntity = mBuilder.build();
            mPost.setEntity(mEntity);


            HttpResponse response = mClient.execute(mPost);
            HttpEntity httpEntity = response.getEntity();
            EntityUtils.consumeQuietly(httpEntity);


            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                Log.i("images", "Image upload successful " + statusCode);
                return true;
            } else {
                Log.i("images", "Bad server response code, media add, Status code: " + statusCode);
                return false;

            }

        } catch (IOException e) {
            Log.i("images", "IOException in media_sync, Media add" + e.toString());
            return false;
        }


    }

    private void media_remove(final int type) {

        try {
            final JSONObject job = new JSONObject();
            job.put("user_id", user_id);
            job.put("password", password);
            job.put("type", type);

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.media_sync_remove, job, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    Log.i("images", "Success media sync remove " + response.toString());

                    if (type == 0)
                        dbObj.execSQL("UPDATE server_sync SET profile_pic_sync=0 WHERE 1=1");
                    else
                        dbObj.execSQL("UPDATE server_sync SET exp_sync=0 WHERE 1=1");

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (type == 0)
                        dbObj.execSQL("UPDATE server_sync SET profile_pic_sync=1 WHERE 1=1");
                    else
                        dbObj.execSQL("UPDATE server_sync SET exp_sync=1 WHERE 1=1");

                }
            }) {
                @Override
                public byte[] getBody() {
                    return job.toString().getBytes();
                }


            };

            mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);


        } catch (JSONException e) {
            Log.i("images", "Exception in media sync service, media remove" + e.toString());
        }


    }


}
