package club.finderella.finderella.Utilities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.cocosw.bottomsheet.BottomSheet;

import org.apmem.tools.layouts.FlowLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import club.finderella.finderella.POJO.BookmarkWrapper;
import club.finderella.finderella.CustomClasses.CircularImg;
import club.finderella.finderella.POJO.IntroItem;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;
import de.greenrobot.event.EventBus;


public class IntroductionViewer extends AppCompatActivity {

    private static IntroItem mItem = null;
    private static String password;
    private static int user_id;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private RequestQueue queue;
    private ArrayList<Integer> mBookmarkedAccounts;


    private TextView mName, mMetaData, mLocation, mStatus;
    private CircularImg mProfileImg;
    private String profile_image_url;
    private TextView bookmarkButton, pokeButton;
    private ImageView block_icon;
    private FlowLayout mFlowLayout;
    private NetworkImageView exp_img;

    private Bundle bReceived;

    private Toolbar mToolBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_intro_viewer);
        // get references

        mToolBar = (Toolbar) findViewById(R.id.mToolBar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("");

        mName = (TextView) findViewById(R.id.mName);
        mMetaData = (TextView) findViewById(R.id.mMetaData);
        mLocation = (TextView) findViewById(R.id.mLocation);
        mStatus = (TextView) findViewById(R.id.mStatus);
        mProfileImg = (CircularImg) findViewById(R.id.mProfileImg);
        bookmarkButton = (TextView) findViewById(R.id.bookmarkButton);
        pokeButton = (TextView) findViewById(R.id.pokeButton);
        mFlowLayout = (FlowLayout) findViewById(R.id.mFlowLayout);
        exp_img = (NetworkImageView) findViewById(R.id.exp_img);
        block_icon = (ImageView) findViewById(R.id.block_icon);

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();
        mBookmarkedAccounts = new ArrayList<Integer>();

        queue = MySingleton.getInstance(this).getRequestQueue();

        bReceived = getIntent().getExtras();
        Log.i("mTag", "Account id received, iNtroductions viwer-> " + bReceived.getInt("account_id"));
    }


    @Override
    protected void onResume() {
        super.onResume();
        mItem = null;

        if (bReceived != null)
            new DownloadIntroItem().execute(bReceived.getInt("account_id"));
        else
            Log.i("mTag", "NO BUNDLE IN INTENT, IntroductionViewer");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void loadView() {

        mName.setText(mItem.name);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mItem.name);
        }

        mMetaData.setText(mItem.metadata);
        mLocation.setText(mItem.location);
        mStatus.setText(mItem.status);

        bookmarkButton.setOnClickListener(bookmarkListener);
        pokeButton.setOnClickListener(pokeListener);

        if (mItem.bookmarked) {
            bookmarkButton.setTextColor(getResources().getColor(R.color.ColorPrimary));
        } else {
            bookmarkButton.setTextColor(getResources().getColor(R.color.GreyText));
        }

        if (mItem.poked) {
            pokeButton.setTextColor(getResources().getColor(R.color.ColorPrimary));
        } else {
            pokeButton.setTextColor(getResources().getColor(R.color.GreyText));
        }

        block_icon.setOnClickListener(block_icon_click);

        mProfileImg.setOnClickListener(dpViewListener);
        profile_image_url = mItem.profile_image;


        ImageLoader mImageLoader = MySingleton.getInstance(this).getImageLoader();
        if (mItem.profile_image != null)
            mProfileImg.setImageUrl(mItem.profile_image, mImageLoader);
        else
            mProfileImg.setImageDrawable(getResources().getDrawable(R.drawable.default_human));

        if (mItem.exp_type == 1) {
            exp_img.setImageUrl(mItem.exp_img, mImageLoader);
            exp_img.setVisibility(View.VISIBLE);
        } else {
            exp_img.setVisibility(View.GONE);
        }

        mFlowLayout.removeAllViews();
        for (int i = 0; i < mItem.collection.length; i++) {
            if (mItem.collection[i] != null) {
                // add the view
                View vx = LayoutInflater.from(this).inflate(R.layout.badge_icon, null); // red icon is invisible by default
                setBackResource(i + 1, vx);
                mFlowLayout.addView(vx, new FlowLayout.LayoutParams(FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT));

                switch (i) {
                    case 0:
                        vx.setOnClickListener(fb_view);
                        break;
                    case 1:
                        vx.setOnClickListener(tw_view);
                        break;
                    case 2:
                        vx.setOnClickListener(ins_view);
                        break;
                    case 3:
                        vx.setOnClickListener(phone_view);
                        break;
                }
            }
        }
    }


    private final View.OnClickListener dpViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent mIntent = new Intent(IntroductionViewer.this, IntroDPViewer.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            mIntent.putExtra("image_url", profile_image_url);
            startActivity(mIntent);
        }
    };
    private final View.OnClickListener bookmarkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mItem.bookmarked) {
                new AddBookmark().execute();
                bookmarkButton.setTextColor(getResources().getColor(R.color.ColorPrimary));
            } else {
                new DeleteBookmark().execute();
                bookmarkButton.setTextColor(getResources().getColor(R.color.GreyText));
            }
        }
    };

    private final View.OnClickListener pokeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mItem.poked) {
                bookmarkButton.setTextColor(getResources().getColor(R.color.ColorPrimary));
                new Poke().execute(mItem.account_id);
            } else {
                Toast.makeText(IntroductionViewer.this, "Pokes are permanent", Toast.LENGTH_LONG).show();
            }
        }
    };

    private final View.OnClickListener fb_view = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent mInt = new Intent(IntroductionViewer.this, Browser.class);
            mInt.putExtra("url", mItem.collection[0]);
            mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(mInt);
        }
    };
    private final View.OnClickListener tw_view = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent mInt = new Intent(IntroductionViewer.this, Browser.class);
            mInt.putExtra("url", mItem.collection[1]);
            mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(mInt);
        }
    };
    private final View.OnClickListener ins_view = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent mInt = new Intent(IntroductionViewer.this, Browser.class);
            mInt.putExtra("url", mItem.collection[2]);
            mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(mInt);
        }
    };
    private final View.OnClickListener phone_view = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new BottomSheet.Builder(IntroductionViewer.this, R.style.MyBottomSheetTheme).title("Call " + mItem.collection[3] + "?").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == R.id.yes_delete) {
                        dialog.dismiss();
                        Intent mInt = new Intent(Intent.ACTION_DIAL);
                        mInt.setData(Uri.parse("tel:" + mItem.collection[3]));
                        startActivity(mInt);
                    } else {
                        dialog.dismiss();

                    }
                }
            }).show();


        }
    };

    private final View.OnClickListener block_icon_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String title;
            if (mItem.access == 0) {
                title = "Unblock?";
            } else {
                title = "Block?";
            }
            new BottomSheet.Builder(IntroductionViewer.this, R.style.MyBottomSheetTheme).title(title).sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    switch (which) {
                        case R.id.yes_delete:
                            if (mItem.access == 0) {
                                mItem.access = 1;
                                // remove from database
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        serverOps(mItem.account_id, 0, "");
                                    }
                                }).start();
                            } else {
                                mItem.access = 0;
                                // add to database
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        serverOps(mItem.account_id, 1, mItem.name);
                                    }
                                }).start();
                            }

                            dialog.dismiss();
                            break;

                        case R.id.no_delete:
                            dialog.dismiss();
                            break;

                    }


                }
            }).show();


        }
    };

    private class AddBookmark extends AsyncTask<Void, Void, Integer> {
        Button bt;

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                String query = "SELECT _id FROM bookmarks WHERE account_id=" + mItem.account_id;
                Cursor c = dbObj.rawQuery(query, null);
                if (c.getCount() == 0) {
                    ContentValues values = new ContentValues();   // inserting into shown table
                    values.put("account_id", mItem.account_id);
                    long y = dbObj.insert("bookmarks", null, values);

                    mItem.bookmarked = true;
                    EventBus.getDefault().post(new BookmarkWrapper(mItem.account_id, (int) y, 1));
                }
                c.close();


                return 1;
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (integer == 1) {
                // success
            } else {
                // failure
                bt.setTextColor(getResources().getColor(R.color.GreyText));
                Toast.makeText(IntroductionViewer.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteBookmark extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String query = "DELETE FROM bookmarks WHERE account_id=" + mItem.account_id;
            dbObj.execSQL(query);

            mItem.bookmarked = false;
            EventBus.getDefault().post(new BookmarkWrapper(mItem.account_id, 0));
            return null;
        }
    }

    private class Poke extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            if (user_id == 0) {
                //set user_id and password
                Cursor c = dbObj.rawQuery("SELECT user_id, password FROM user_data WHERE 1=1", null);
                while (c.moveToNext()) {
                    user_id = c.getInt(c.getColumnIndex("user_id"));
                    password = c.getString(c.getColumnIndex("password"));
                }
                c.close();
            }

            final JSONObject job = new JSONObject();

            try {

                job.put("user_id", user_id);
                job.put("password", password);
                job.put("account_id", params[0]);


                JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.poke, job, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i("mTag", "Success in poke " + response.toString());
                        } catch (Exception e) {
                            Log.i("mTag", "Inside volley, poke :" + e.toString());
                            pokeButton.setTextColor(getResources().getColor(R.color.GreyText));
                            Toast.makeText(IntroductionViewer.this, "Try later...", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pokeButton.setTextColor(getResources().getColor(R.color.GreyText));
                        Toast.makeText(IntroductionViewer.this, "Try later...", Toast.LENGTH_LONG).show();
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
            }


            return null;
        }


    }

    private class DownloadIntroItem extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            Log.i("mTag", "Inside downloadintroitem");
            try {
                JSONObject job = new JSONObject();
                Cursor c = dbObj.rawQuery("SELECT user_id, password FROM user_data", null);
                while (c.moveToNext()) {
                    user_id = c.getInt(c.getColumnIndex("user_id"));
                    password = c.getString(c.getColumnIndex("password"));
                }
                c.close();
                job.put("user_id", user_id);
                job.put("password", password);

                JSONArray jin = new JSONArray();
                jin.put(1);
                jin.put(params[0]);

                JSONArray jout = new JSONArray();
                jout.put(jin);

                job.put("new_posts", jout);

                c = dbObj.rawQuery("SELECT account_id FROM bookmarks", null);
                while (c.moveToNext()) {
                    mBookmarkedAccounts.add(c.getInt(c.getColumnIndex("account_id")));
                }
                uploadAndGet(job);

            } catch (Exception e) {

            }
            return null;
        }
    }

    private void uploadAndGet(final JSONObject jUp) throws JSONException {

        if (jUp != null) {
            Log.i("mTag", "Inside upload and get");
            Log.i("mTag", "json being sent" + jUp.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_introduction, jUp, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    try {

                        Log.i("mTag", "fetch introduction results for IntroViewer, " + response.toString());

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    parseJsonFeed(response);
                                } catch (JSONException e) {
                                    Log.i("mTag", "Exception thrown in parsing response for intro viewer fetch, " + e.toString());
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                if (mItem.access == 1)
                                    loadView();
                                else {
                                    finish();
                                    Log.i("mTag", "No access");
                                }
                            }
                        }.execute();


                    } catch (Exception e) {
                        Log.i("mTag", "volley exception in received response (intro viewer)" + e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
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
    }

    public void parseJsonFeed(JSONObject job) throws JSONException {

        if (job.has("error")) {
            if (job.getInt("error") == 0) {
                JSONArray result = job.getJSONArray("new_data");
                mItem = getIntroItem(result.getJSONObject(0));
            } else {
                // ERRORS
                switch (job.getInt("error")) {
                }
            }
        }
    }

    private IntroItem getIntroItem(JSONObject feedObj) throws JSONException {
        Log.i("tag", "inside getIntroItem");
        String temp;
        IntroItem item = new IntroItem();

        item.access = feedObj.getInt("access");
        item.account_id = feedObj.getInt("account_id");
        item.post_id = feedObj.getInt("post_id");

        if (feedObj.getInt("access") == 0)
            return item;

        item.name = feedObj.getJSONArray("text_info").get(0).toString();    // Name

        if (mBookmarkedAccounts.contains(feedObj.getInt("account_id"))) // getting a long
            item.bookmarked = true;
        else
            item.bookmarked = false;

        temp = "";
        if (!feedObj.getJSONArray("text_info").get(1).toString().equals("0")) // Age
            temp += feedObj.getJSONArray("text_info").get(1).toString() + ", ";
        if (!feedObj.getJSONArray("text_info").get(2).toString().equals("0")) // Gender
            temp += feedObj.getJSONArray("text_info").get(2).toString();
        item.metadata = temp;


        if (!feedObj.getJSONArray("text_info").get(3).toString().equals("0"))
            item.location = feedObj.getJSONArray("text_info").get(3).toString(); //Location
        else
            item.location = "";

        if (!feedObj.getString("status").equals("0"))
            item.status = feedObj.getString("status"); // Status
        else
            item.status = "";

        String t;

        for (int j = 0; j < 4; j++) { // fb,tw,ins,phone links  // collection array has already been initialized in constructor
            t = feedObj.getJSONArray("social_info").get(j).toString();

            if (t.equals("0"))
                item.collection[j] = null;
            else
                item.collection[j] = t;
        }

        if (!feedObj.getString("profile_pic").equals("0"))
            item.profile_image = feedObj.getString("profile_pic");      // Profile photo
        else
            item.profile_image = null;


        switch (feedObj.getInt("exp_type")) {   // expressions
            case 0:
                item.exp_type = 0;
                item.exp_img = null;
                item.exp_video = null;
                break;
            case 1:
                item.exp_type = 1;
                item.exp_img = feedObj.getString("exp_img");
                item.exp_video = null;
                break;
            case 2:
                item.exp_type = 2;
                item.exp_img = null;
                item.exp_video = feedObj.getString("exp_vid");
                break;
            default:
                item.exp_type = 0;
                item.exp_img = null;
                item.exp_video = null;
                break;
        }


        return item;
    }

    public void setBackResource(int type, View v) {
        switch (type) {
            case 1:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.facebook));
                break;
            case 2:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.twitter));
                break;
            case 3:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.instagram));
                break;
            case 4:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getResources().getDrawable(R.drawable.phone));
                break;
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
                                sendIntent.putExtra(Intent.EXTRA_TEXT, "Find everyone around you, on your phone! Download Finderella for android from www.finderella.club");
                                sendIntent.setType("text/plain");
                                sendIntent.setPackage("com.whatsapp");
                                startActivity(sendIntent);

                                break;
                        }
                    }
                }).show();
                break;
            case R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                onBackPressed();
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void serverOps(final int acc, final int operation, final String name) {

        Cursor c = dbObj.rawQuery("SELECT * FROM block_list WHERE account_id=" + acc, null);
        if (operation == 0) {
            // to remove
            if (c.getCount() == 0)
                return;
        } else {
            if (c.getCount() != 0)
                return;
        }
        c.close();

        try {
            // operation 0 to remove acc_id from block , 1 to add
            final JSONObject job = new JSONObject();

            c = dbObj.rawQuery("SELECT * FROM user_data", null);
            while (c.moveToNext()) {
                job.put("user_id", c.getInt(c.getColumnIndex("user_id")));
                job.put("password", c.getString(c.getColumnIndex("password")));

                break;
            }
            c.close();
            job.put("account_id", acc);
            job.put("operation", operation);


            Log.i("mTag", "json being sent, introadap blocking op:" + job.toString());
            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.block_list_sync, job, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.i("mTag", "on Response for removing from block list:" + response.toString());

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {

                                if (operation == 0) {
                                    // to remove
                                    dbObj.execSQL("DELETE FROM block_list WHERE account_id =" + acc);
                                } else {
                                    // to add
                                    ContentValues values = new ContentValues();
                                    values.put("account_id", acc);
                                    values.put("name", name);
                                    dbObj.insert("block_list", null, values);
                                }
                                return null;
                            }


                        }.execute();


                    } catch (Exception e) {
                        Log.i("mTag", "volley exception in received response, IntroAdapter, Block op" + e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mTag", "Volley error in block list remove" + error.toString());
                    Toast.makeText(IntroductionViewer.this, "No network, try later", Toast.LENGTH_LONG).show();
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
        }
    }  // volley request  // Block ops


}
