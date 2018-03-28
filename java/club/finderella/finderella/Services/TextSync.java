package club.finderella.finderella.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

import org.json.JSONArray;
import org.json.JSONObject;


import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;

public class TextSync extends IntentService {           /// NEEDS EDIT

    private static MyDBHandler db;
    private static SQLiteDatabase dbObj;
    private JSONObject jOb;
    private JSONArray jAr;
    private RequestQueue queue;

    public TextSync() {
        super("TextSync");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
        jOb = new JSONObject();
        jAr = new JSONArray();

        queue = MySingleton.getInstance(this).getRequestQueue();
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // internet connected
            String query = "SELECT * FROM meta_data WHERE 1=1";
            Cursor c = dbObj.rawQuery(query, null);

            try {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndex("first_name")) + " " + c.getString(c.getColumnIndex("last_name"));

                    jAr.put(name);

                    jAr.put(c.getString(c.getColumnIndex("age")));

                    if (c.getString(c.getColumnIndex("gender")) != null)
                        jAr.put(c.getString(c.getColumnIndex("gender")));
                    else
                        jAr.put("0");

                    if (c.getString(c.getColumnIndex("location")) != null)
                        jAr.put(c.getString(c.getColumnIndex("location")));
                    else
                        jAr.put("0");

                    if (c.getString(c.getColumnIndex("status")) != null)
                        jAr.put(c.getString(c.getColumnIndex("status")));
                    else
                        jAr.put("Hey there! Nice to meet you");

                    break;
                }

                jOb.put("main_data", jAr);

                query = "SELECT user_id,password FROM user_data WHERE 1=1";
                c = dbObj.rawQuery(query, null);
                while (c.moveToNext()) {
                    jOb.put("user_id", c.getInt(c.getColumnIndex("user_id")));
                    jOb.put("password", c.getString(c.getColumnIndex("password")));
                }
                c.close();

                // jOb is ready to be sent
                Log.i("mTag", "JSON being sent TextSync -- JSON:" + jOb.toString());

                JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.text_sync, jOb, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i("mTag", "Inside volley json success TextSync");
                            Log.i("mTag", "Response in TextSync:" + response.toString());
                            dbObj.execSQL("UPDATE server_sync SET text_sync =0 WHERE 1=1");
                        } catch (Exception e) {
                            Log.i("mTag", "Inside volley json exception TextSync EXCEPTION :" + e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        dbObj.execSQL("UPDATE server_sync SET text_sync =1 WHERE 1=1");
                        Log.i("mTag", "HAS ERROR textSync volley" + error.toString());
                    }
                }) {
                    @Override
                    public byte[] getBody() {
                        return jOb.toString().getBytes();
                    }
                };
                mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                queue.add(mReq);
            } catch (Exception e) {
            }


        } else {
            dbObj.execSQL("UPDATE server_sync SET text_sync =1 WHERE 1=1");
        }
    }
}
