package club.finderella.finderella;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.POJO.PokeItem;
import club.finderella.finderella.POJO.PokeWrapper;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.Myadapters.PokesAdapter;
import de.greenrobot.event.EventBus;


public class PokesFrag extends Fragment implements FragmentLifecycle {

    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private static ArrayList<PokeItem> mList;
    private static PokesAdapter mAdapter;
    private ListView mListView;
    private RelativeLayout empty_view;


    private RequestQueue queue;

    private MaterialRefreshLayout mSwipeRefresh;

    private static boolean auto_refresh = true;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(PokeWrapper pw) {
        addPoke(pw.account_id);
    }

    @Override
    public void onPauseFragment() {
        Log.i("workflow", "On pause(custom) executed for POKES FRAG");
    }

    @Override
    public void onResumeFragment() {
        Log.i("workflow", "On resume(custom) executed for POKES FRAG");
        if (auto_refresh) {
            Log.i("workflow", "calling autorefresh POKES FRAG");
            mSwipeRefresh.autoRefresh();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        db = new MyDBHandler(getActivity(), null, null, 1);
        dbObj = db.getWritableDatabase();
        dbObj.execSQL("UPDATE pokes SET session =0, read =1 WHERE 1=1");

        auto_refresh = true;

        queue = MySingleton.getInstance(getActivity()).getRequestQueue();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_pokes, container, false);
        Log.i("workflow", "on create view called for PokesFrag");

        mListView = (ListView) v.findViewById(R.id.mListView);
        mList = new ArrayList<PokeItem>();
        mAdapter = new PokesAdapter(getActivity(), mList);
        mListView.setAdapter(mAdapter);

        empty_view = (RelativeLayout) v.findViewById(R.id.empty_view);
        mSwipeRefresh = (MaterialRefreshLayout) v.findViewById(R.id.mSwipeRefresh);
        mSwipeRefresh.setProgressColors(new int[]{Color.BLACK, Color.BLUE});

        mSwipeRefresh.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {
                if (auto_refresh)
                    auto_refresh = false;

                new LoadPokes().execute();
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                if (mList.size() != 0)
                    new LoadPokes().execute();
                else
                    mSwipeRefresh.finishRefreshLoadMore();
            }
        });

        /*
        if (auto_refresh) {
            Log.i("workflow", "calling autorefresh POKES FRAG");
            mSwipeRefresh.autoRefresh();
        }
        */


        return v;
    }

    private class LoadPokes extends AsyncTask<Void, Void, Void> {   // loading the mList
        boolean empty = true;
        @Override
        protected Void doInBackground(Void... params) {
            Cursor c;
            c = dbObj.rawQuery("SELECT * FROM pokes WHERE session =0 ORDER BY _id DESC LIMIT 10", null);
            JSONArray jar = new JSONArray();
            JSONArray jTemp;

            if (c.getCount() != 0) {
                empty = false;
                while (c.moveToNext()) {
                    jTemp = new JSONArray();
                    jTemp.put(c.getInt(c.getColumnIndex("_id")));
                    jTemp.put(c.getInt(c.getColumnIndex("account_id")));
                    jar.put(jTemp);
                }

                try {
                    uploadAndGet(jar);
                } catch (JSONException e) {
                    Log.i("mTag", "json exception in upload and get, pokes frag" + e.toString());
                }
            } else {
                empty = true;
            }
            c.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (empty) {
                mSwipeRefresh.finishRefresh();
                mSwipeRefresh.finishRefreshLoadMore();
            }
        }
    }

    private void uploadAndGet(JSONArray jar) throws JSONException {
        if (jar != null) {

            final JSONObject jup = new JSONObject();
            int user_id;
            String password;

            Cursor c = dbObj.rawQuery("SELECT * FROM user_data", null);
            while (c.moveToNext()) {
                user_id = c.getInt(c.getColumnIndex("user_id"));
                password = c.getString(c.getColumnIndex("password"));

                jup.put("user_id", user_id);
                jup.put("password", password);

                break;
            }

            c.close();

            jup.put("main_data", jar);
            Log.i("mTag", "Poke json being sent to server:" + jup.toString());

            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.fetch_poke_item, jup, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    try {
                        Log.i("mTag", response.toString());
                        new AsyncTask<JSONObject, Void, Void>() {
                            @Override
                            protected Void doInBackground(JSONObject... params) {

                                parseJsonFeed(params[0]);

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
                        }.execute(response);

                    } catch (Exception e) {
                        Log.i("mTag", "volley exception in received response" + e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mTag", "HAS ERROR, pokes frag vollet " + error.toString());
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

    private void parseJsonFeed(JSONObject j) {
        try {

            if (j.has("error")) {

                if (j.getInt("error") == 0) {
                    // no error
                    JSONArray jar = j.getJSONArray("main_data");
                    for (int i = 0; i < jar.length(); i++) {
                        JSONObject job = jar.getJSONObject(i);
                        PokeItem mItem = getItem(job);

                        if (mItem.access == 1)
                            mList.add(0, mItem);

                        // Execute query
                        dbObj.execSQL("UPDATE pokes SET session =1 WHERE _id =" + mItem.poke_id);

                    }


                }
            }
        } catch (JSONException ll) {
            Log.i("mTag", "JSON exception in parseJsonFeed, pokes frag" + ll.toString());
        }
    }

    private PokeItem getItem(JSONObject job) throws JSONException {

        PokeItem it = new PokeItem();

        it.access = job.getInt("access");
        it.account_id = job.getInt("account_id");
        it.poke_id = job.getInt("poke_id");

        if (it.access == 0)
            return it;

        it.mProfileImg = job.getString("profile_pic");
        it.mName = job.getString("name");

        return it;

    }


    private void addPoke(final int x) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mSwipeRefresh.autoRefresh();
            }
        }.execute();


    }
}
