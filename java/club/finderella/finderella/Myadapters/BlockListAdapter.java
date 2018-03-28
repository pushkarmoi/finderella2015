package club.finderella.finderella.Myadapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cocosw.bottomsheet.BottomSheet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import club.finderella.finderella.POJO.BlockListItem;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;


public class BlockListAdapter extends ArrayAdapter<BlockListItem> {

    private ArrayList<BlockListItem> mList;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private LayoutInflater mInflater;
    private RequestQueue queue;

    public BlockListAdapter(Context context, ArrayList<BlockListItem> mList) {
        super(context, R.layout.custom_block, mList);
        this.mList = mList;
        db = new MyDBHandler(getContext(), null, null, 1);
        dbObj = db.getWritableDatabase();
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        queue = MySingleton.getInstance(getContext()).getRequestQueue();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_block, null);
        }

        final BlockListItem mItem = getItem(position);
        ((TextView) convertView.findViewById(R.id.content)).setText(mItem.name);
        (convertView.findViewById(R.id.unblock_icon)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BottomSheet.Builder((Activity) getContext(), R.style.MyBottomSheetTheme).title("Unblock?").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.yes_delete:
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        serverOps(mItem.account_id, 0);
                                    }
                                }).start();
                                dialog.dismiss();
                                break;
                            case R.id.no_delete:
                                dialog.dismiss();
                                break;
                        }
                    }
                }).show();
            }
        });

        return convertView;
    }

    private void serverOps(final int acc, final int operation) {

        try {
            // operation 0 to remove acc_id from block , 1 to add
            final JSONObject job = new JSONObject();

            Cursor c = dbObj.rawQuery("SELECT * FROM user_data", null);
            while (c.moveToNext()) {
                job.put("user_id", c.getInt(c.getColumnIndex("user_id")));
                job.put("password", c.getString(c.getColumnIndex("password")));

                break;
            }
            c.close();
            job.put("account_id", acc);
            job.put("operation", operation);

            // ADD VOLLEY REQUEST
            JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.block_list_sync, job, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.i("mTag", "on Response for removing from block list:" + response.toString());

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                dbObj.execSQL("DELETE FROM block_list WHERE account_id =" + acc);

                                for (int i = 0; i < mList.size(); i++) {
                                    if (mList.get(i).account_id == acc) {
                                        mList.remove(i);
                                        break;
                                    }
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                Log.i("mTag", "updating block list adapter, from adapter code");
                                (BlockListAdapter.this).notifyDataSetChanged();
                            }
                        }.execute();


                    } catch (Exception e) {
                        Log.i("mTag", "volley exception in received response" + e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mTag", "Volley error in block list remove" + error.toString());
                    Toast.makeText(getContext(), "No network, try later", Toast.LENGTH_LONG).show();
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
    }  // volley request (rest not on separate thread)


}
