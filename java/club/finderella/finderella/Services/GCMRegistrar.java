package club.finderella.finderella.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONObject;

import java.io.IOException;

import club.finderella.finderella.POJO.AppVersion;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;


public class GCMRegistrar extends IntentService {

    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private int user_id;
    private String password;
    private JSONObject job;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    int app_version = AppVersion.APP_VERSION;
    String sender_id = "779597386352";

    private RequestQueue queue;
    private static final String[] TOPICS = {"global"};


    public GCMRegistrar() {
        super("GCMRegistrar");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();

        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();

        queue = MySingleton.getInstance(this).getRequestQueue();


        Cursor c = dbObj.rawQuery("SELECT * FROM user_data", null);
        while (c.moveToNext()) {
            user_id = c.getInt(c.getColumnIndex("user_id"));
            password = c.getString(c.getColumnIndex("password"));

            break;
        }

        job = new JSONObject();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(sender_id, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);  // the token
            job.put("GCM_RegistrationID", token);
            job.put("app_version", app_version);

            if (internetConnected() && sendToServer()) {
                // update mSharedPref
                mEditor.putInt("gcm_sync_required", 0);  // 1 means required
                mEditor.putInt("app_version", app_version);
                mEditor.commit();
            } else { // error
                // update mSharedPref
                mEditor.putInt("gcm_sync_required", 1);  // 1 means required
                mEditor.commit();
            }

            subscribeTopics(token);


            Log.i("mTag", "GCM Registration ID: " + token);

        } catch (Exception e) {
            Log.i("mTag", "Exception thrown in obtaining GCM: " + e.toString());
        }


    }

    private boolean sendToServer() {

        try {
            job.put("user_id", user_id);
            job.put("password", password);

            Log.i("mTag", "JSON sent ( gcm registration):->" + job.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.gcm_registration, job, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    Log.i("mTag", "GCM Registration Response success: " + response.toString());
                    mEditor.putInt("gcm_sync_required", 0);
                    mEditor.putInt("app_version", AppVersion.APP_VERSION);
                    mEditor.commit();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mTag", "HAS ERROR " + error.toString());
                    mEditor.putInt("gcm_sync_required", 1);
                    mEditor.commit();

                }
            }) {
                @Override
                public byte[] getBody() {
                    return job.toString().getBytes();
                }
            };


            mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);


            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

    private boolean internetConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else
            return false;
    }


}
