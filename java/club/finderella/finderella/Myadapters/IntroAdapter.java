package club.finderella.finderella.Myadapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import club.finderella.finderella.POJO.BookmarkWrapper;
import club.finderella.finderella.POJO.MyUrlData;
import club.finderella.finderella.Utilities.Browser;
import club.finderella.finderella.CustomClasses.CircularImg;
import club.finderella.finderella.POJO.IntroItem;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;
import club.finderella.finderella.Utilities.IntroDPViewer;
import de.greenrobot.event.EventBus;


public class IntroAdapter extends ArrayAdapter {
    public ArrayList<IntroItem> mList;
    private ImageLoader mImageLoader;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private IntroItem theItem;
    private LayoutInflater mInflater;
    private RequestQueue queue;

    private static int user_id = 0; // initialized as a flag for being not set
    private static String password;

    public IntroAdapter(Context context, ArrayList<IntroItem> mList) {
        super(context, R.layout.custom_intro, mList);
        this.mList = mList;
        mImageLoader = MySingleton.getInstance(getContext()).getImageLoader();

        db = new MyDBHandler(getContext(), null, null, 1);
        dbObj = db.getWritableDatabase();
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        queue = MySingleton.getInstance(context).getRequestQueue();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        theItem = (IntroItem) getItem(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_intro, parent, false);

            holder = new ViewHolder();

            holder.account_id = theItem.account_id;
            holder.bookmarked = theItem.bookmarked;

            if (theItem.access == 0) {      // access =0 means blocked
                holder.blocked = true;
            } else {
                holder.blocked = false;
            }

            holder.name = (TextView) convertView.findViewById(R.id.mName);
            holder.meta_data = (TextView) convertView.findViewById(R.id.mMetaData);
            holder.location = (TextView) convertView.findViewById(R.id.mLocation);
            holder.status = (TextView) convertView.findViewById(R.id.mStatus);

            holder.profile_image = (CircularImg) convertView.findViewById(R.id.mProfileImg);
            holder.profile_image_url = theItem.profile_image;

            holder.mFlowLayout = (FlowLayout) convertView.findViewById(R.id.mFlowLayout);
            holder.mCollection = theItem.collection;

            holder.exp_type = theItem.exp_type;
            holder.exp_img = (NetworkImageView) convertView.findViewById(R.id.exp_img);

            holder.bookmarkButton = (TextView) convertView.findViewById(R.id.bk_text);
            holder.bookmarkButton.setOnClickListener(holder.bookmarkListener);

            holder.block_icon = (ImageView) convertView.findViewById(R.id.block_icon);
            holder.block_icon.setOnClickListener(holder.block_icon_click);

            holder.pokeButton = (TextView) convertView.findViewById(R.id.pk_text);
            holder.pokeButton.setOnClickListener(holder.pokeListener);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mFlowLayout.removeAllViews();
        holder.profile_image.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));


        holder.name.setText(theItem.name);
        holder.meta_data.setText(theItem.metadata);
        holder.location.setText(theItem.location);
        holder.status.setText(theItem.status);

        if (holder.bookmarked) {
            holder.bookmarkButton.setTextColor(getContext().getResources().getColor(R.color.ColorPrimary));
        } else {
            holder.bookmarkButton.setTextColor(getContext().getResources().getColor(R.color.GreyText));
        }

        if (holder.poked) {
            holder.pokeButton.setTextColor(getContext().getResources().getColor(R.color.ColorPrimary));
        } else {
            holder.pokeButton.setTextColor(getContext().getResources().getColor(R.color.GreyText));
        }

        if (theItem.profile_image != null) {
            holder.profile_image.setImageUrl(theItem.profile_image, mImageLoader);
            holder.profile_image.setOnClickListener(holder.dpViewListener);
        } else
            holder.profile_image.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));


        if (holder.exp_type == 1) {
            holder.exp_img.setImageUrl(theItem.exp_img, mImageLoader);
            holder.exp_img.setVisibility(View.VISIBLE);
        } else {
            holder.exp_img.setVisibility(View.GONE);
        }

        // Social Buttons
        //holder.mFlowLayout.removeAllViews();  Done above
        for (int i = 0; i < (holder.mCollection).length; i++) {
            View vx;
            if (holder.mCollection[i] != null) {
                // add the view

                vx = mInflater.inflate(R.layout.badge_icon, null); // red icon is invisible by default
                setBackResource(i + 1, vx);
                holder.mFlowLayout.addView(vx, new FlowLayout.LayoutParams(FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT));

                switch (i) {
                    case 0:
                        vx.setOnClickListener(holder.fb_view);
                        break;
                    case 1:
                        vx.setOnClickListener(holder.tw_view);
                        break;
                    case 2:
                        vx.setOnClickListener(holder.ins_view);
                        break;
                    case 3:
                        vx.setOnClickListener(holder.phone_view);
                        break;
                }
            }
        }


        return convertView;
    }

    private class ViewHolder {

        public ViewHolder() {
            this.mCollection = new String[]{null, null, null, null};
            this.bookmarked = false;
            this.poked = false;
            this.blocked = false;
        }

        private int account_id, exp_type;
        private boolean bookmarked, poked, blocked;

        private NetworkImageView exp_img;
        private CircularImg profile_image;
        private String profile_image_url;
        //private VideoView exp_vid;

        private TextView meta_data, name, status, location;

        private FlowLayout mFlowLayout;
        private String[] mCollection;

        private TextView bookmarkButton, pokeButton;
        private ImageView block_icon;


        private final View.OnClickListener dpViewListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = new Intent(getContext(), IntroDPViewer.class);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                mIntent.putExtra("image_url", profile_image_url);
                getContext().startActivity(mIntent);
            }
        };

        private final View.OnClickListener bookmarkListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bookmarked) {
                    new AddBookmark().execute(ViewHolder.this);
                    bookmarkButton.setTextColor(getContext().getResources().getColor(R.color.ColorPrimary));

                } else {
                    new DeleteBookmark().execute(ViewHolder.this);
                    bookmarkButton.setTextColor(getContext().getResources().getColor(R.color.GreyText));

                }
            }
        };

        private final View.OnClickListener pokeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!poked) {
                    new Poke().execute(ViewHolder.this);
                    pokeButton.setTextColor(getContext().getResources().getColor(R.color.ColorPrimary));
                } else
                    Toast.makeText(getContext(), "Pokes are permanent", Toast.LENGTH_LONG).show();
            }
        };

        private final View.OnClickListener fb_view = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mInt = new Intent(getContext(), Browser.class);
                mInt.putExtra("url", mCollection[0]);
                mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                getContext().startActivity(mInt);
            }
        };

        private final View.OnClickListener tw_view = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mInt = new Intent(getContext(), Browser.class);
                mInt.putExtra("url", mCollection[1]);
                mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                getContext().startActivity(mInt);
            }
        };

        private final View.OnClickListener ins_view = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mInt = new Intent(getContext(), Browser.class);
                mInt.putExtra("url", mCollection[2]);
                mInt.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                getContext().startActivity(mInt);
            }
        };
        private final View.OnClickListener phone_view = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BottomSheet.Builder(getContext(), R.style.MyBottomSheetTheme).title("Call " + mCollection[3] + "?").sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == R.id.yes_delete) {
                            dialog.dismiss();
                            Intent mInt = new Intent(Intent.ACTION_DIAL);
                            mInt.setData(Uri.parse("tel:" + mCollection[3]));
                            getContext().startActivity(mInt);
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
                String mTitle;
                if (blocked) {
                    mTitle = "Unblock?";
                } else {
                    mTitle = "Block?";
                }

                new BottomSheet.Builder(getContext(), R.style.MyBottomSheetTheme).title(mTitle).sheet(R.menu.yesorno).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case R.id.yes_delete:
                                if (blocked) {
                                    blocked = false;
                                    // send request to server  to  remove
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            serverOps(account_id, 0, "");
                                        }
                                    }).start();
                                } else {
                                    blocked = true;
                                    // send request to server to add
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            serverOps(account_id, 1, name.getText().toString());
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
            // check database
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
    }  // volley request


    private class AddBookmark extends AsyncTask<ViewHolder, Void, Void> {
        TextView bt;
        int acc;
        long bid;
        boolean send = false;

        @Override
        protected Void doInBackground(ViewHolder... params) {
            acc = params[0].account_id;

            String query = "SELECT _id FROM bookmarks WHERE account_id=" + acc;
            Cursor c = dbObj.rawQuery(query, null);
            if (c.getCount() == 0) {
                send = true;
                ContentValues values = new ContentValues();   // inserting into bookmarks table
                values.put("account_id", params[0].account_id);
                bid = dbObj.insert("bookmarks", null, values);
            }
            c.close();
            params[0].bookmarked = true;
            bt = params[0].bookmarkButton;

            // notify other entries in the list
            for (int i = 0; i < mList.size(); i++) {
                IntroItem x = mList.get(i);
                if (x.account_id == params[0].account_id) {
                    x.bookmarked = true;
                }
            } // end FOR

            return null;
        }

        @Override
        protected void onPostExecute(Void integer) {
            // send event bus event
            if (send) {
                EventBus.getDefault().post(new BookmarkWrapper(acc, (int) bid, 1));
                Log.i("mTag", "Event sent to add bookmark!");
            } else {
                bt.setTextColor(getContext().getResources().getColor(R.color.GreyText));
            }


        }
    }

    private class DeleteBookmark extends AsyncTask<ViewHolder, Void, Void> {
        TextView bt;
        int acc;

        @Override
        protected Void doInBackground(ViewHolder... params) {
            acc = params[0].account_id;

            String query = "DELETE FROM bookmarks WHERE account_id=" + acc;
            dbObj.execSQL(query);

            params[0].bookmarked = false;
            bt = params[0].bookmarkButton;

            for (int i = 0; i < mList.size(); i++) {
                IntroItem x = mList.get(i);

                if (x.account_id == params[0].account_id) {
                    if (x.bookmarked)
                        x.bookmarked = false;
                }
            } // end FOR


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // send eventbus
            EventBus.getDefault().post(new BookmarkWrapper(acc, 0));
            Log.i("mTag", "Event sent to delete bookmark");

        }
    }

    private class Poke extends AsyncTask<ViewHolder, Void, Void> {
        TextView bt;

        @Override
        protected Void doInBackground(ViewHolder... params) {
            if (user_id == 0) {
                //set user_id and password
                Cursor c = dbObj.rawQuery("SELECT user_id, password FROM user_data WHERE 1=1", null);
                while (c.moveToNext()) {
                    user_id = c.getInt(c.getColumnIndex("user_id"));
                    password = c.getString(c.getColumnIndex("password"));
                }
                c.close();
            }

            bt = params[0].pokeButton;
            params[0].poked = true;

            final JSONObject job = new JSONObject();

            try {

                job.put("user_id", user_id);
                job.put("password", password);
                job.put("account_id", params[0].account_id);


                JsonObjectRequest mReq = new JsonObjectRequest(Request.Method.POST, MyUrlData.poke, job, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i("mTag", "Success in poke " + response.toString());
                        } catch (Exception e) {
                            Log.i("mTag", "Inside volley, poke :" + e.toString());
                            bt.setTextColor(getContext().getResources().getColor(R.color.GreyText));
                            Toast.makeText(getContext(), "Try later...", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        bt.setTextColor(getContext().getResources().getColor(R.color.GreyText));
                        Toast.makeText(getContext(), "Try later...", Toast.LENGTH_LONG).show();
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

    public void setBackResource(int type, View v) {
        switch (type) {
            case 1:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getContext().getResources().getDrawable(R.drawable.facebook));
                break;
            case 2:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getContext().getResources().getDrawable(R.drawable.twitter));
                break;
            case 3:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getContext().getResources().getDrawable(R.drawable.instagram));
                break;
            case 4:
                ((ImageView) (v.findViewById(R.id.mIcon))).setImageDrawable(getContext().getResources().getDrawable(R.drawable.phone));
                break;
        }
    }


}
