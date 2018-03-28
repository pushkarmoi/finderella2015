package club.finderella.finderella;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import com.facebook.LoggingBehavior;
import com.facebook.Profile;
import com.facebook.login.LoginManager;

import club.finderella.finderella.Coach.Coach;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;

import com.facebook.login.LoginResult;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class Welcome extends AppCompatActivity {


    private CallbackManager fb_CallbackManager;
    private LoginManager mLoginManagerInstance;
    private static Profile mProfile = null;

    private RequestQueue queue;

    private static JSONObject jUp, jDw;
    private static JSONObject fbObj;


    private static BluetoothAdapter mBluetoothAdapter;
    private static String mac_address;

    private static ProgressDialog progressDialog;

    private SQLiteDatabase dbObj;
    private MyDBHandler db;

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    private TextView mTitle, mSubtext;

    private static String fbProfilePicUrl = null;


    private static boolean parseResponseError = false;
    private static boolean nativeOpsComplete = false;

    private static int tries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        fb_CallbackManager = CallbackManager.Factory.create();
        mLoginManagerInstance = LoginManager.getInstance();
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        setContentView(R.layout.layout_welcome);

        queue = MySingleton.getInstance(this).getRequestQueue();

        db = new MyDBHandler(Welcome.this, null, null, 1);
        dbObj = db.getWritableDatabase();

        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();

        jUp = new JSONObject();
        jDw = new JSONObject();
        fbObj = new JSONObject();


        mTitle = (TextView) findViewById(R.id.mTitle);
        mSubtext = (TextView) findViewById(R.id.mSubtext);
        mTitle.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/finderella_font.ttf"));
        mSubtext.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/sub_font.otf"));

        findViewById(R.id.loginView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fbLogin();
            }
        });


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null) {
            if (formatMac(mBluetoothAdapter.getAddress()) != null)
                mac_address = formatMac(mBluetoothAdapter.getAddress());
            else
                mac_address = "000000000000";
        } else
            mac_address = "000000000000";

        Log.i("mTag", "mac:" + mac_address);


        mLoginManagerInstance.registerCallback(fb_CallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        if (Profile.getCurrentProfile() != null && AccessToken.getCurrentAccessToken() != null) {
                            mProfile = Profile.getCurrentProfile();
                        } else {
                            Profile.fetchProfileForCurrentAccessToken();
                            mProfile = Profile.getCurrentProfile();
                        }

                        new Operations().execute();
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(Welcome.this, "Try again later", Toast.LENGTH_LONG).show();
                    }
                });


    }

    public void fbLogin() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.i("mTag", "Internet connected!");
            mLoginManagerInstance.logInWithReadPermissions(this, Arrays.asList("public_profile"));
        } else {
            Log.i("mTag", "No internet");
            Toast.makeText(this, "Internet required to sign up", Toast.LENGTH_LONG).show();
        }
    }


    private class Operations extends AsyncTask<Void, Integer, Void> {


        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(Welcome.this);
            progressDialog.setTitle("Setting up your account");
            progressDialog.setMessage("Just a moment...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
            progressDialog.setProgress(0);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {

            tries++;

            if (tries != 1)
                synchronized (this) {
                    try {
                        Log.i("mTag", "WAIT WAIT WAIT WAIT WAIT WAIT");
                        wait(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            // start off server ops
            uploadAndGet();

            // start off graph operation1 & operation 2
            final Bundle gr1 = new Bundle();
            gr1.putString("fields", "gender,link,first_name,last_name");


            new GraphRequest(AccessToken.getCurrentAccessToken(), "/me", gr1, HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            fbObj = response.getJSONObject();
                            publishProgress(30);

                            if (fbObj != null) {
                                Log.i("mTag", "fbObj:-" + fbObj.toString());
                                sqlOperations(fbObj);
                            } else
                                Log.i("mTag", "fbObj is null");
                            fbProfilePicUrl = photoOperations();
                        }
                    }
            ).executeAndWait();

            publishProgress(75);
            Log.i("mTag", "after both graph requests");

            while (!nativeOpsComplete) {
                synchronized (this) {
                    try {
                        Log.i("mTag", "WAIT WAIT WAIT WAIT WAIT WAIT");
                        wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }


            publishProgress(100);
            Log.i("mTag", "doInBack of Operations is complete");

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i("mTag", "entering on postExec of Operations");

            progressDialog.dismiss();

            Intent mIntent = new Intent(Welcome.this, Coach.class);

            if (fbProfilePicUrl != null)
                mIntent.putExtra("fbProfilePicUrl", fbProfilePicUrl);



            if (!parseResponseError) {
                // all good
                Log.i("mTag", "Inside onPostExec of Operations Success!!");
                mEditor.putInt("code", 2);
                mEditor.commit();
                startActivity(mIntent);
                finish();
            } else {
                // Reset all variables
                jUp = new JSONObject();
                jDw = new JSONObject();
                fbObj = new JSONObject();
                fbProfilePicUrl = null;
                parseResponseError = false;
                nativeOpsComplete = false;
                // -- End Reset
                // try again server establishment
                if (tries < 3)
                    new Operations().execute();
                else {
                    Log.i("mTag", "Inside onPostExec ERROR");
                    Toast.makeText(Welcome.this, "Network error, try later", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private void sqlOperations(JSONObject job) {
        Log.i("mTag", "start sqlOperations");

        // job is the fbObj containing   gender,link,first_name,last_name
        try {

            // META DATA
            String query = "DELETE FROM meta_data WHERE 1=1";
            dbObj.execSQL(query);

            query = "INSERT INTO meta_data (first_name,last_name) VALUES(\"" + job.getString("first_name") + "\",\"" + job.getString("last_name") + "\")";
            dbObj.execSQL(query);   // returned in proper editted format

            if (job.getString("gender").length() == 4) {
                query = "UPDATE meta_data SET gender=\"Male\" WHERE 1=1";
                dbObj.execSQL(query);
            } else {
                query = "UPDATE meta_data SET gender=\"Female\" WHERE 1=1";
                dbObj.execSQL(query);
            }

            // USER MEDIA
            query = "DELETE FROM user_media WHERE 1=1";
            dbObj.execSQL(query);
            query = "INSERT INTO user_media (profile_image) VALUES(NULL)";
            dbObj.execSQL(query);


            //SOCIAL NETWORKS

            query = "DELETE FROM social_networks_data WHERE 1=1";
            dbObj.execSQL(query);

            query = "INSERT INTO social_networks_data(type,url) VALUES(1,\"" + job.getString("link") + "\")";
            dbObj.execSQL(query);

            query = "INSERT INTO social_networks_data(type,url) VALUES (2,NULL),(3,NULL),(4,NULL)";
            dbObj.execSQL(query);


            // SERVER_SYNC
            query = "DELETE FROM server_sync WHERE 1=1";
            dbObj.execSQL(query);

            query = "INSERT INTO server_sync(exp_sync, text_sync, profile_pic_sync) VALUES (0,0,0)";
            dbObj.execSQL(query);
            // NO NEED TO SET PROFILE_PIC_SYNC=1 as SERVER ops in ProfileSetupOne handles that!


        } catch (SQLException j) {
            Log.i("mTag", j.toString());
            Toast.makeText(Welcome.this, "Try Again", Toast.LENGTH_LONG).show();
            mLoginManagerInstance.logOut();
        } catch (JSONException e) {
            Log.i("mTag", e.toString());
            Toast.makeText(Welcome.this, "Try Again", Toast.LENGTH_LONG).show();
            mLoginManagerInstance.logOut();
        }
        Log.i("mTag", "end of  sqlOperations");
    }

    private String photoOperations() {
        // user media row inserted in sql operations
        String mUrl;

        if ((mProfile != null) && (mProfile.getProfilePictureUri(150, 150).toString() != null))
            mUrl = mProfile.getProfilePictureUri(150, 150).toString();
        else
            mUrl = null;

        if (mUrl != null) {
            return mUrl;
        } else {
            return null;
        }
    }

    private void uploadAndGet() {
        try {
            jUp.put("mac_address", mac_address);
        } catch (JSONException e) {
        }

        JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.init, jUp, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    Log.i("mTag", response.toString());

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            parseServerResponse(response);
                        }
                    }).start();

                } catch (Exception e) {
                    parseResponseError = true;
                    nativeOpsComplete = true;
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("mTag", "HAS ERROR" + error.toString());
                parseResponseError = true;
                nativeOpsComplete = true;
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

    private void parseServerResponse(final JSONObject jj) {

        Log.i("mTag", "inside ParseServerResponse  doInBackground");
        try {
            if (jj.has("error")) {
                if (jj.getInt("error") == 0) {
                    String query = "DELETE FROM user_data WHERE 1=1";
                    dbObj.execSQL(query);
                    long time;
                    time = (System.currentTimeMillis()) / 1000; // in seconds
                    time = (time) / (24 * 60 * 60); // in days
                    query = "INSERT INTO user_data(user_id,password,start) VALUES(" + jj.getInt("user_id") + ",\"" + jj.getString("password") + "\"," + time + ")";
                    dbObj.execSQL(query);
                    parseResponseError = false;
                } else {
                    parseResponseError = true;
                }
            } else {
                parseResponseError = true;
            }
        } catch (JSONException e) {
            parseResponseError = true;
            Log.i("mTag", "ParseServerResponse exception:" + e.toString());
        } finally {
            nativeOpsComplete = true;
            Log.i("mTag", "Native server ops complete set to true");
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fb_CallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private String formatMac(String x) {
        String toRet = "";
        String error = "";

        if (x == null)
            return null;

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


}
