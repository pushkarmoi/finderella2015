package club.finderella.finderella.Utilities;


import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.support.v7.widget.Toolbar;

import com.android.volley.RequestQueue;
import com.cocosw.bottomsheet.BottomSheet;


import java.util.ArrayList;

import club.finderella.finderella.POJO.BlockListItem;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.Myadapters.BlockListAdapter;
import club.finderella.finderella.R;

public class BlockList extends AppCompatActivity {

    private ListView mListView;

    private ArrayList<BlockListItem> mList;
    private BlockListAdapter mAdapter;


    private RelativeLayout empty_view;

    private RequestQueue queue;

    private Toolbar mToolbar;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_block_list);

        db = new MyDBHandler(this, null, null, 1);
        dbObj = db.getWritableDatabase();

        mToolbar = (Toolbar) findViewById(R.id.mToolBar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Block List");
        }else {
            Log.i("mTag", "action bar cant be loaded");
            onBackPressed();
        }

        empty_view = (RelativeLayout) findViewById(R.id.empty_view);
        mListView = (ListView) findViewById(R.id.mListView);

        mList = new ArrayList<BlockListItem>();
        mAdapter = new BlockListAdapter(this, mList);
        mListView.setAdapter(mAdapter);

        queue = MySingleton.getInstance(this).getRequestQueue();

        new LoadBlockList().execute();
    }

    private class LoadBlockList extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Cursor c = dbObj.rawQuery("SELECT * FROM block_list ORDER BY _id DESC", null);
            if (c.getCount() == 0) {
                c.close();
            } else {
                while (c.moveToNext()) {
                    mList.add(0, new BlockListItem(c.getInt(c.getColumnIndex("_id")), c.getInt(c.getColumnIndex("account_id")), c.getString(c.getColumnIndex("name"))));
                }
                c.close();
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            if (mList.size() == 0) {
                empty_view.setVisibility(View.VISIBLE);
            } else {
                empty_view.setVisibility(View.GONE);
            }

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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
