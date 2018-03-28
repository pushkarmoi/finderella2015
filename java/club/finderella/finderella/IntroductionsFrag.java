package club.finderella.finderella;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import club.finderella.finderella.Coach.Coach;
import club.finderella.finderella.Helpers.FragmentLifecycle;
import club.finderella.finderella.POJO.IntroItem;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.Myadapters.IntroAdapter;


public class IntroductionsFrag extends Fragment implements FragmentLifecycle {

    private static ListView mListView;
    private static IntroAdapter mAdapter;
    private static ArrayList<IntroItem> mList;
    private RequestQueue queue;
    private static MaterialRefreshLayout mSwipeRefresh;

    private MyDBHandler db;
    private SQLiteDatabase dbObj;

    private static int user_id;
    private static String password;
    private static long start_day;

    private static ArrayList<Integer> mBookmarkedAccounts = null;


    private static boolean access_loader = true;
    private static boolean access_cleanser = true;

    private static boolean do_auto_refresh = true;

    private static Timer mTimer;

    private RelativeLayout empty_view;
    private static Button mNewButton = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new MyDBHandler(getActivity(), null, null, 1);
        dbObj = db.getWritableDatabase();


        queue = MySingleton.getInstance(getActivity()).getRequestQueue();

        mList = new ArrayList<IntroItem>();
        mAdapter = new IntroAdapter(getActivity(), mList);
        do_auto_refresh = true;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                dbObj.execSQL("UPDATE main_data SET session =0 WHERE 1=1");
                dbObj.execSQL("UPDATE shown_data SET session =0 WHERE 1=1");


                return null;
            }
        }.execute();        // sets up bookmarked accounts and sets session bits to 0

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {
                    boolean show_new_posts_button = false;

                    @Override
                    protected Void doInBackground(Void... params) {
                        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        if (networkInfo != null && networkInfo.isConnected()) {
                            if (access_cleanser) {
                                Log.i("mTag", "Inside timer task, Doing cleansing operations");
                                cleanseOperations();
                            }
                            if (access_loader) {
                                if (loader(true, false) != 0) {
                                    Log.i("mTag", "Inside timer task, Doing load new posts operations");
                                    show_new_posts_button = true;
                                } else {
                                    show_new_posts_button = false;
                                }
                            }
                        } else {
                            show_new_posts_button = false;
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if (show_new_posts_button) {
                            Log.i("mTag", "Inside timer task,showing New Posts Button!");

                            mNewButton.setVisibility(View.VISIBLE);
                            mNewButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View vx) {
                                    mListView.smoothScrollToPositionFromTop(0, 0);
                                    vx.setVisibility(View.GONE);

                                }
                            });

                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {

                                    synchronized (this) {
                                        try {
                                            wait(10000);
                                        } catch (InterruptedException ii) {
                                        }
                                    }
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void aVoid) {
                                    mNewButton.setVisibility(View.GONE);
                                }
                            }.execute();    // wait for 10 sec and then make new items button disappear
                        }

                    }
                }.execute();


            } // end here
        }, 1000 * 60 * 10, 1000 * 60 * 10); // delay for FIRST run->10 min and then a 10 min period bw subsequent tasks
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_introductions, container, false);

        mListView = (ListView) v.findViewById(R.id.mListView);
        // adapter and list initialized in onCreate
        if (mListView.getAdapter() == null)
            mListView.setAdapter(mAdapter);

        empty_view = (RelativeLayout) v.findViewById(R.id.empty_view);
        mNewButton = (Button) v.findViewById(R.id.mNewButton);

        mSwipeRefresh = (MaterialRefreshLayout) v.findViewById(R.id.mSwipeRefresh);
        mSwipeRefresh.setProgressColors(new int[]{Color.BLACK, Color.BLUE});

        mSwipeRefresh.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        if (access_loader) {
                            if (do_auto_refresh) {  // it is the first time
                                do_auto_refresh = false;
                                Log.i("tag", "inside swipe listener, executing first time load posts");
                                loader(true, true);
                            } else { // user pressed
                                Log.i("tag", "inside swipre listener, called by USER");

                                if (mList.size() != 0)
                                    loader(true, false);    // only load new
                                else
                                    loader(true, true);     // 0 introductions, load all

                            }
                        }
                        return null;
                    }

                }.execute();


            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                // FOR LOADING OLD POSTS    at the end
                if (mList.size() != 0) {
                    Log.i("tag", "loading more old posts...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            loader(false, true);
                        }// end run();
                    }).start();
                } else {
                    Log.i("tag", "mList empty, therefore cant load more");
                    mSwipeRefresh.finishRefreshLoadMore();
                }


            }


        });     // swipe refresh listener


        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String query = "SELECT * FROM user_data";
                Cursor c = dbObj.rawQuery(query, null);
                while (c.moveToNext()) {
                    user_id = c.getInt(c.getColumnIndex("user_id"));
                    password = c.getString(c.getColumnIndex("password"));
                    start_day = c.getLong(c.getColumnIndex("start"));
                }
                c.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (do_auto_refresh) {
                    Log.i("workflow", "calling autorefresh Introductions FRAG");
                    mSwipeRefresh.autoRefresh();    // calls swipe refresh listener
                }

            }
        }.execute();        // sets up credentials and calls auto refresh for first time


        return v;
    }

    @Override
    public void onPauseFragment() {
        Log.i("workflow", "On pause(custom) executed for INTRO FRAG");
    }

    @Override
    public void onResumeFragment() {
        Log.i("workflow", "On resume(custom) executed for INTRO FRAG");
    }   // not used as this is first fragment!


    @Override
    public void onDetach() {
        super.onDetach();
        mTimer.cancel();
    }


    public int loader(boolean loadNew, boolean loadOld) {   // true and false passed acc. to what needs to be loaded

        access_loader = false;
        int posts = 0;

        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // internet connected
            try {
                posts = forceRefresh(loadNew, loadOld);

                if (posts == 0) {
                    Log.i("tag", "inside loader, NO fresh data");
                    if (access_cleanser && loadNew) {
                        cleanseOperations();


                        // WAIT TILL CLEANSE OPERATIONS END
                        synchronized (this) {
                            while (!access_cleanser) {      // access_cleanser will be false while it is being executed
                                try {
                                    wait(2000);
                                } catch (IllegalStateException e) {
                                }
                            }
                        }

                        posts = forceRefresh(loadNew, loadOld);
                    } else {
                        mSwipeRefresh.finishRefresh();
                        mSwipeRefresh.finishRefreshLoadMore();
                        access_loader = true;
                        return 0;
                    }
                } else {
                    // access_loader = true; will be done by volley response
                    return posts;
                }


                // updated after cleanse operations
                if (posts == 0) {
                    access_loader = true;
                    mSwipeRefresh.finishRefresh();
                    mSwipeRefresh.finishRefreshLoadMore();

                    return 0;
                } else {
                    // access_loader = true; will be done by volley response
                    return posts;
                }

            } catch (Exception e) {
                access_loader = true;
                return 0;
            }

        } else {
            // no internet
            Log.i("tag", "inside loader, no internet");
            mSwipeRefresh.finishRefresh();
            mSwipeRefresh.finishRefreshLoadMore();

            access_loader = true;
            return 0;
        }
    }   // returns of total posts added (being added)

    public void cleanseOperations() {
        Log.i("tag", "Doing cleanse ops");
        try {
            access_cleanser = false;
            String query;
            Cursor c;

            final JSONObject jUp = new JSONObject();
            JSONObject jTemp;
            JSONArray jArray = new JSONArray();

            query = "SELECT * FROM  mac_data";
            c = dbObj.rawQuery(query, null);

            Log.i("tag", "no. of mac_data enries =>" + c.getCount());

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
            Log.i("tag", "JSON being sent for fetch account id:" + jUp.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_account_id, jUp, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    try {
                        Log.i("tag", "fetch account id response : " + response.toString());

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (fillCache(response)) {
                                        try {
                                            otherQueries();
                                        } catch (SQLException s) {
                                            Log.i("tag", "SQL Exception thrown inside otherQueries");
                                        }
                                        try {
                                            uploadAccounts();       // no need to synchronize this
                                        } catch (JSONException gg) {
                                            Log.i("tag", "Exception thrown in uploadaccounts" + gg.toString());
                                        }
                                    }

                                } catch (JSONException e) {
                                    Log.i("tag", "exception in filling cache" + e.toString());

                                } finally {
                                    access_cleanser = true;
                                }
                            }
                        }).start();

                    } catch (Exception e) {
                        Log.i("tag", "volley exception in received response (fetch account id)" + e.toString());
                        access_cleanser = true;
                        return;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("tag", "HAS ERROR " + error.toString());
                    access_cleanser = true;
                    return;
                }
            }) {
                @Override
                public byte[] getBody() {
                    return jUp.toString().getBytes();
                }
            };


            mReq.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);

        } catch (Exception e) {
            access_cleanser = true;
        }
    }

    private boolean uploadAccounts() throws JSONException {
        final JSONObject jUp = new JSONObject();
        JSONArray jArray = new JSONArray();
        String query;
        Cursor c;

        final Boolean status[] = new Boolean[]{true};

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
                        status[0] = true;
                    } catch (Exception e) {
                        Log.i("tag", "volley exception in received response (upload accounts)" + e.toString());
                        status[0] = false;
                        throw e;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("tag", "volley response HAS ERROR " + error.toString());
                    status[0] = false;
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

        return status[0];


    }

    private boolean fillCache(JSONObject jDw) throws JSONException {      // returns success status

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

            return true;

        } else {
            Log.i("tag", "bad server response");
            return false;
        }


    }

    private void otherQueries() throws SQLException {

        Log.i("tag", " *** Executing other queries ***");

        String query;
        query = "DELETE FROM mac_data WHERE _id IN (" +
                "SELECT mac_data_id FROM cache_data)";
        Log.i("tag", "Query being exec->" + query);
        dbObj.execSQL(query);


        query = "DELETE FROM cache_data WHERE account_id = 0";
        dbObj.execSQL(query);
        Log.i("tag", "Query being exec->" + query);

        // delete same rows in cache_data // cache data contains extra server added data, that has greater mac_data_id (appended to the end)
        query = "DELETE FROM cache_data " +
                "WHERE mac_data_id NOT IN (" +
                "SELECT MAX(mac_data_id) FROM cache_data GROUP BY account_id)";
        dbObj.execSQL(query);
        Log.i("tag", "Query being exec->" + query);

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

    private int forceRefresh(boolean loadNew, boolean loadOld) throws JSONException {       // true and false acc. to whether to add new/old posts or not

        int new_posts_count = 0;
        int old_posts_count = 0;

        if (loadNew)
            new_posts_count = addNewPosts();
        else
            new_posts_count = 0;

        if (new_posts_count < 10) {

            if (loadOld)
                old_posts_count = addOldPosts(10 - new_posts_count);
            else
                old_posts_count = 0;

        } else {
            old_posts_count = 0;
        }


        return (new_posts_count + old_posts_count);

    } // returns count of TOTAL (old and new) posts that are present in the database and are being fetched

    private int addNewPosts() throws JSONException {
        Log.i("tag", "inside load new posts");
        JSONArray jArray, jTemp;
        jArray = null;
        Cursor c;
        int count = 0;
        String query = "";

        try {
            query = "SELECT * FROM main_data WHERE shown_id =0 AND session =0 ORDER BY _id DESC LIMIT 10";
            c = dbObj.rawQuery(query, null);
            count = c.getCount();
            Log.i("tag", "no. of new posts = " + count);
        } catch (SQLException s) {
            return 0;
        }

        if (count != 0) {
            // there are new posts

            jArray = new JSONArray();

            while (c.moveToNext()) {
                jTemp = new JSONArray();

                jTemp.put(c.getInt(c.getColumnIndex("_id")));
                jTemp.put(c.getInt(c.getColumnIndex("account_id")));

                jArray.put(jTemp);
            }

            c.close();
            uploadAndProcess(jArray, true);
        }

        return count;
    }   // max of 10 posts can be added

    private int addOldPosts(int number) throws JSONException {
        Log.i("tag", "inside load old posts");
        JSONArray jArray, jTemp;
        Cursor c;
        int count = 0;
        String query = "";

        try {
            query = "SELECT * FROM shown_data WHERE session =0 ORDER BY _id DESC LIMIT " + number;
            c = dbObj.rawQuery(query, null);
            count = c.getCount();

            Log.i("tag", "no. of old posts = " + count);
        } catch (SQLException s) {
            return 0;
        }

        if (count != 0) {
            jArray = new JSONArray();
            while (c.moveToNext()) {
                jTemp = new JSONArray();

                jTemp.put(c.getInt(c.getColumnIndex("_id")));
                jTemp.put(c.getInt(c.getColumnIndex("account_id")));

                jArray.put(jTemp);
            }

            c.close();
            uploadAndProcess(jArray, false);
        }

        return count;
    }   // max of (10-new_posts) can be added

    private void uploadAndProcess(JSONArray jx, final boolean are_new_posts) throws JSONException {

        final JSONObject jUp = new JSONObject();

        jUp.put("user_id", user_id);
        jUp.put("password", password);

        if (are_new_posts) {
            jUp.put("new_posts", jx);
        } else {
            jUp.put("old_posts", jx);
        }
        Log.i("tag", "fetching intro's, json being sent:" + jUp.toString());

        JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_introduction, jUp, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    if (are_new_posts)
                        Log.i("tag", "fetch introduction results for NEW POSTS, " + response.toString());
                    else
                        Log.i("tag", "fetch introduction results for OLD POSTS, " + response.toString());


                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                parseJsonFeed(response);
                            } catch (JSONException e) {
                                Log.i("tag", "Exception thrown in parsing response for introductions fetch, " + e.toString());
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            access_loader = true;
                            mAdapter.notifyDataSetChanged(); // saves scroll state
                            mSwipeRefresh.finishRefresh();
                            mSwipeRefresh.finishRefreshLoadMore();

                            if (mList.size() != 0)
                                empty_view.setVisibility(View.GONE);

                        }
                    }.execute();


                } catch (Exception e) {
                    Log.i("tag", "volley exception in received response (fetch intros)" + e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                access_loader = true;
                Log.i("tag", "HAS ERROR in fetch introfrag volley response " + error.toString());
                mSwipeRefresh.finishRefresh();
                mSwipeRefresh.finishRefreshLoadMore();
            }
        }) {
            @Override
            public byte[] getBody() {
                return jUp.toString().getBytes();
            }
        };


        mReq.setRetryPolicy(new DefaultRetryPolicy(20000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(mReq);


    }   // for introductions

    public void parseJsonFeed(JSONObject job) throws JSONException {

        Log.i("tag", "Inside parse response");

        if (job.has("error")) {
            if (job.getInt("error") == 0) {
                // no errors
                // the returned data will be in lowest post_id first for NEW_DATA and vice-versa for OLD_DATA
                // get new posts first
                IntroItem it;
                String query;
                ContentValues values;
                JSONArray jArray;

                // Reloading mBookmarkedAccounts
                mBookmarkedAccounts = new ArrayList<Integer>();
                Cursor c = dbObj.rawQuery("SELECT account_id FROM bookmarks", null);
                while (c.moveToNext()) {
                    mBookmarkedAccounts.add(c.getInt(c.getColumnIndex("account_id")));
                }
                c.close();

                if (job.has("new_data")) {

                    jArray = job.getJSONArray("new_data");

                    for (int i = 0; i < jArray.length(); i++) {
                        it = getIntroItem((JSONObject) jArray.get(i));

                        if (it.access == 1) {
                            mList.add(0, it); // adding to top
                            Log.i("tag", "new post added to mlist, new mList size" + mList.size());
                        }

                        try {
                            values = new ContentValues();   // inserting into shown table
                            values.put("account_id", it.account_id);
                            values.put("session", 1);
                            long x = dbObj.insert("shown_data", null, values);

                            query = "UPDATE main_data SET shown_id = " + x + ", session =1 WHERE _id =" + it.post_id;   // updating shown_id and session in main_data
                            dbObj.execSQL(query);
                        } catch (SQLException s) {
                            Log.i("tag", "SQL EXCEPTION inside parseJSONfeed (new data )" + s.toString());
                        }

                    }

                }

                if (job.has("old_data")) {

                    jArray = job.getJSONArray("old_data");

                    for (int i = 0; i < jArray.length(); i++) {
                        it = getIntroItem((JSONObject) jArray.get(i));

                        if (it.access == 1) {
                            mList.add(it); // adding to the end
                            Log.i("tag", "old post added to mlist, new mList size" + mList.size());
                        }
                        try {
                            query = "UPDATE shown_data SET session =1 WHERE _id =" + it.post_id;   // updating shown_id and session in main_data
                            dbObj.execSQL(query);
                        } catch (SQLException s) {
                            Log.i("tag", "SQL EXCEPTION inside parseJSONfeed (old data)" + s.toString());
                        }
                    }


                }
            }
        } else {
            Log.i("tag", "received response doesnt have field -> 'error'");
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

        if ((mBookmarkedAccounts != null) && (mBookmarkedAccounts.contains(feedObj.getInt("account_id")))) // getting a long
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


}


