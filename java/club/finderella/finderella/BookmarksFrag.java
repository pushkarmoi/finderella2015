package club.finderella.finderella;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

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


import club.finderella.finderella.Helpers.FragmentLifecycle;
import club.finderella.finderella.POJO.BookmarkWrapper;
import club.finderella.finderella.POJO.IntroItem;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.Myadapters.BookmarkAdapter;
import de.greenrobot.event.EventBus;


public class BookmarksFrag extends Fragment implements FragmentLifecycle {

    private static ListView mListView;
    private static BookmarkAdapter mAdapter;

    private static ArrayList<IntroItem> mList;

    private static MyDBHandler db;
    private static SQLiteDatabase dbObj;

    private static Parcelable state;

    private RequestQueue queue;
    private RelativeLayout empty_view;

    private MaterialRefreshLayout mSwipeRefresh;

    private static boolean auto_refresh = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new MyDBHandler(getActivity(), null, null, 1);
        dbObj = db.getWritableDatabase();

        auto_refresh = true;

        mList = new ArrayList<IntroItem>();
        mAdapter = new BookmarkAdapter(getActivity(), mList);

        dbObj.execSQL("UPDATE bookmarks SET session =0 WHERE 1=1");

        queue = MySingleton.getInstance(getActivity()).getRequestQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_bookmarks, container, false);

        mListView = (ListView) v.findViewById(R.id.mListView);
        Log.i("workflow", "on createview executed for Bookmarks FRAG");

        if (mListView.getAdapter() == null)
            mListView.setAdapter(mAdapter);

        empty_view = (RelativeLayout) v.findViewById(R.id.empty_view);

        mSwipeRefresh = (MaterialRefreshLayout) v.findViewById(R.id.mSwipeRefresh);
        mSwipeRefresh.setProgressColors(new int[]{Color.BLACK, Color.BLUE});
        mSwipeRefresh.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {

                if (auto_refresh) {
                    auto_refresh = false;
                }
                new LoadBookmarks().execute();
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                Log.i("tag", "USER caled load OLD BOOKMARKS");

                if (mList.size() != 0) {
                    new LoadBookmarks().execute();
                } else
                    mSwipeRefresh.finishRefreshLoadMore();

            }
        });

        /*
        if (auto_refresh) {
            Log.i("workflow", "calling autorefresh Bookmarks FRAG");
            mSwipeRefresh.autoRefresh();        // Load Bookmarks called inside swipeListener
        }
        */

        return v;
    }

    @Override
    public void onPauseFragment() {
        Log.i("workflow", "On pause(custom) executed for BOOKMARKS FRAG");
    }

    @Override
    public void onResumeFragment() {
        Log.i("workflow", "On resume(custom) executed for BOOKMARKS FRAG");
        if (auto_refresh) {
            Log.i("workflow", "calling autorefresh Bookmarks FRAG");
            mSwipeRefresh.autoRefresh();        // Load Bookmarks called inside swipeListener
        }
    }

    private class LoadBookmarks extends AsyncTask<Void, Void, Void> {

        private JSONArray jArray, jTemp;
        private boolean empty = false;

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Log.i("tag", "Loading Bookmarks...");
                String query;
                Cursor c;


                query = "SELECT * FROM bookmarks WHERE session =0 ORDER BY _id DESC LIMIT 10";

                c = dbObj.rawQuery(query, null);
                Log.i("tag", " Bookmarks count =" + c.getCount());
                if (c.getCount() != 0) {
                    empty = false;
                    jArray = new JSONArray();

                    while (c.moveToNext()) {
                        jTemp = new JSONArray();

                        jTemp.put(c.getInt(c.getColumnIndex("_id")));
                        jTemp.put(c.getInt(c.getColumnIndex("account_id")));

                        jArray.put(jTemp);
                    }
                    c.close();

                    ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        uploadAndGet(jArray);
                    }
                } else {
                    empty = true;
                }
            } catch (SQLException j) {
                Log.i("mTag", "SQL exception thrown in LoadBookmaeks" + j.toString());
            } catch (JSONException s) {
                Log.i("mTag", "JSON exception thrown in LoadBookmaeks" + s.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (empty) {
                mSwipeRefresh.finishRefresh();
                mSwipeRefresh.finishRefreshLoadMore();
            }

        }


    } // end LoadBookmarks

    private void uploadAndGet(JSONArray jar) throws JSONException {
        if (jar != null) {

            final JSONObject jup = new JSONObject();
            int user_id;
            String password;

            String query = "SELECT user_id, password FROM user_data";
            Cursor c = dbObj.rawQuery(query, null);
            while (c.moveToNext()) {
                user_id = c.getInt(c.getColumnIndex("user_id"));
                password = c.getString(c.getColumnIndex("password"));

                jup.put("user_id", user_id);
                jup.put("password", password);

                break;
            }       // setting user id and password

            jup.put("new_posts", jar);
            Log.i("mTag", "JSON being sent, for fetching bookmarks:" + jup.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_introduction, jup, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    try {
                        Log.i("mTag", "Bookmarks volley response: " + response.toString());
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    parseJsonFeed(response);
                                } catch (JSONException e) {
                                    Log.i("mTag", "JSON exception in parsing response feed bookmarks...");
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                mAdapter.notifyDataSetChanged();
                                mSwipeRefresh.finishRefresh();
                                mSwipeRefresh.finishRefreshLoadMore();

                                if (mList.size() != 0)
                                    empty_view.setVisibility(View.GONE);

                            }
                        }.execute();

                    } catch (Exception e) {
                        Log.i("mTag", "volley exception in received response" + e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mTag", "HAS ERROR, bookmarks frag " + error.toString());
                    mSwipeRefresh.finishRefresh();
                    mSwipeRefresh.finishRefreshLoadMore();
                }
            }) {
                @Override
                public byte[] getBody() {
                    return jup.toString().getBytes();
                }
            };


            mReq.setRetryPolicy(new DefaultRetryPolicy(20000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(mReq);
        }
    }

    private void parseJsonFeed(JSONObject j) throws JSONException {

        if (j.has("error")) {

            if (j.getInt("error") == 0) {
                // no error
                JSONArray jar = j.getJSONArray("new_data");
                for (int i = 0; i < jar.length(); i++) {
                    try {
                        JSONObject job = jar.getJSONObject(i);
                        IntroItem mItem = getItem(job);
                        if (mItem.access == 1) {
                            mList.add(0, mItem);
                        }
                        // set session =1;
                        dbObj.execSQL("UPDATE bookmarks SET session =1 WHERE _id=" + mItem.post_id);

                    } catch (JSONException js) {
                        Log.i("tag", "JSON exception in parsing response, " + js.toString());
                    } catch (SQLException s) {
                        Log.i("tag", "SQL exception in parsing response, " + s.toString());
                    }


                }

            }
        } else {
            Log.i("tag", "No error field in response ");
        }
    }

    private IntroItem getItem(JSONObject feedObj) throws JSONException {
        String temp;
        IntroItem item = new IntroItem();

        item.access = feedObj.getInt("access");

        item.account_id = feedObj.getInt("account_id");
        item.post_id = feedObj.getInt("post_id");

        if (feedObj.getInt("access") == 0)
            return item;

        item.name = feedObj.getJSONArray("text_info").get(0).toString();

        temp = "";
        if (!feedObj.getJSONArray("text_info").get(1).toString().equals("0")) // Age
            temp += feedObj.getJSONArray("text_info").get(1).toString() + ", ";
        if (!feedObj.getJSONArray("text_info").get(2).toString().equals("0")) // Male
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

        for (int j = 0; j < 4; j++) { // 0,1,2,3    fb,tw,ins,phone    // collection array has already been initialized in constructor
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


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    public void onEvent(BookmarkWrapper bw) {
        Log.i("mTag", "Evnt received in bookmarks frag");
        if (bw.operation == 1) {
            // add
            addBookmark(bw.account_id, bw.bookmark_id);
        } else {
            delBookmark(bw.account_id);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    public void addBookmark(final int acc_id, final int bookmark_id) {


        mSwipeRefresh.autoRefresh();


    }   // receives account_id of newly added bookmark

    public void delBookmark(final int acc_id) {

        new AsyncTask<Void, Void, Void>() {
            boolean removed = false;

            @Override
            protected Void doInBackground(Void... params) {
                for (int i = 0; i < mList.size(); i++) {
                    if (mList.get(i).account_id == acc_id) {
                        // delete
                        Log.i("mTag", "bookmark found in mList, ready to delete");
                        mList.remove(i);
                        removed = true;
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (removed) {
                    mAdapter.notifyDataSetChanged();
                    if (mList.size() == 0) {
                        empty_view.setVisibility(View.VISIBLE);
                    }
                }

            }
        }.execute();


    }
}
