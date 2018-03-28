package club.finderella.finderella.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;


public class CleanseService extends IntentService {


    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private static String password;
    private static int user_id;
    private static long start_day;

    private RequestQueue queue;
    private static boolean done = false;

    public CleanseService() {
        super("CleanseService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
        queue = MySingleton.getInstance(this).getRequestQueue();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("mTag", "Inside cleanse ops, called by BluetoothService");

        String query;
        Cursor c;

        query = "SELECT * FROM user_data";
        c = dbObj.rawQuery(query, null);

        while (c.moveToNext()) {
            user_id = c.getInt(c.getColumnIndex("user_id"));
            password = c.getString(c.getColumnIndex("password"));
            start_day = c.getLong(c.getColumnIndex("start"));
            break;
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // internet connected
            cleanseOperations();
            while (!done) {
                synchronized (this) {
                    try {
                        wait(2000);
                    } catch (InterruptedException ii) {
                    }
                }
            }// end of while

        }


    }

    public void cleanseOperations() {
        try {
            done = false;
            String query;
            Cursor c;

            final JSONObject jUp = new JSONObject();
            JSONObject jTemp;
            JSONArray jArray = new JSONArray();

            query = "SELECT * FROM  mac_data";
            c = dbObj.rawQuery(query, null);


            if (c.getCount() != 0) {
                while (c.moveToNext()) {
                    jTemp = new JSONObject();
                    jTemp.put("id", c.getInt(c.getColumnIndex("_id")));
                    jTemp.put("mac", c.getString(c.getColumnIndex("address")));

                    jArray.put(jTemp);
                }

                jUp.put("main_data", jArray);
            }
            c.close();

            jUp.put("user_id", user_id);
            jUp.put("password", password);

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_account_id, jUp, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                fillCache(response);
                                otherQueries();
                                uploadAccounts();

                            } catch (SQLException s) {
                                done = true;
                                Log.i("tag", "SQL Exception thrown inside otherQueries");
                            } catch (JSONException e) {
                                Log.i("tag", "exception in filling cache" + e.toString());
                                done = true;
                            }
                        }
                    }).start();


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("tag", "HAS ERROR " + error.toString());
                    done = true;
                }
            }) {
                @Override
                public byte[] getBody() {
                    return jUp.toString().getBytes();
                }
            };


            mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);

        } catch (JSONException e) {
            Log.i("tag", "JSON EXCEPTION thrown, in CleanseOpera(), called by bluetooth service" + e.toString());
        }
    }

    private void uploadAccounts() throws JSONException {
        final JSONObject jUp = new JSONObject();
        JSONArray jArray = new JSONArray();
        String query;
        Cursor c;


        query = "SELECT account_id FROM main_data WHERE uploaded =0";
        c = dbObj.rawQuery(query, null);

        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                jArray.put(c.getInt(c.getColumnIndex("account_id")));
            }


            jUp.put("user_id", user_id);
            jUp.put("password", password);
            jUp.put("main_data", jArray);

            Log.i("tag", "Uploading accounts: JSON->" + jUp.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.upload_account, jUp, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        dbObj.execSQL("UPDATE main_data SET uploaded=1 WHERE uploaded=0");
                        done = true;
                    } catch (Exception e) {
                        Log.i("tag", "volley exception in received response (upload accounts)" + e.toString());
                        done = true;
                        throw e;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("tag", "volley response HAS ERROR " + error.toString());
                    done = true;
                }
            }) {
                @Override
                public byte[] getBody() {
                    return jUp.toString().getBytes();
                }
            };

            mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);
        }

        c.close();


    }

    private void fillCache(JSONObject jDw) throws JSONException {      // returns success status

        Log.i("tag", "filling cache with respomse data...");

        JSONArray jArray;
        if (jDw.getInt("error") == 0) {
            // no error
            jArray = jDw.getJSONArray("main_data"); // RESULTS // might be empty

            // Add into cache data
            ArrayList<JSONArray> out = new ArrayList<JSONArray>();

            for (int i = 0; i < jArray.length(); i++) {
                out.add(jArray.getJSONArray(i));
            }

            if (out.size() != 0) {      // size of the array list
                try {
                    dbObj.beginTransaction();
                    SQLiteStatement mStat = dbObj.compileStatement("INSERT INTO cache_data(mac_data_id, account_id, server_addition) VALUES(?,?,?)");

                    for (int i = 0; i < out.size(); i++) {
                        mStat.bindLong(1, out.get(i).getLong(0));
                        mStat.bindLong(2, out.get(i).getLong(1));
                        mStat.bindLong(3, out.get(i).getLong(2));
                        mStat.execute();
                    }

                    dbObj.setTransactionSuccessful();

                } catch (SQLException k) {
                    throw k;
                } finally {
                    dbObj.endTransaction();
                }
            } else {
                Log.i("tag", "inside on repsonse cleanser, no data added AFTER CLEANSE OPERATIONs");
            }


        }


    }

    private void otherQueries() throws SQLException {

        Log.i("tag", "Executing other queries");

        String query;
        query = "DELETE FROM mac_data WHERE _id IN (" +
                "SELECT mac_data_id FROM cache_data)";

        dbObj.execSQL(query);

        query = "DELETE FROM cache_data WHERE account_id = 0";
        dbObj.execSQL(query);

        // delete same rows in cache_data // cache data contains extra server added data, that has greater mac_data_id (appended to the end)
        query = "DELETE FROM cache_data " +
                "WHERE mac_data_id NOT IN (" +
                "SELECT MAX(mac_data_id) FROM cache_data GROUP BY account_id)";
        dbObj.execSQL(query);

        // Calculate current day
        long current_day = (System.currentTimeMillis()) / 1000; // in seconds
        current_day = current_day / (24 * 60 * 60); // in days
        current_day = current_day - start_day;  // relative days

        // delete all records before 2 days (in worst case)
        query = "DELETE FROM main_data WHERE days <" + (current_day - 1);
        dbObj.execSQL(query);

        query = "DELETE FROM cache_data WHERE account_id IN(" +
                "SELECT account_id FROM main_data)";
        dbObj.execSQL(query);

        query = "INSERT INTO main_data (account_id, uploaded, days, shown_id) SELECT account_id, server_addition," + current_day + ", 0" + " FROM cache_data";
        dbObj.execSQL(query);

        query = "DELETE FROM cache_data";
        dbObj.execSQL(query);
    }


}
