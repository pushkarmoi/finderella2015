package club.finderella.finderella.Myadapters;


import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.android.volley.toolbox.ImageLoader;


import java.util.ArrayList;


import club.finderella.finderella.CustomClasses.CircularImg;
import club.finderella.finderella.POJO.IntroItem;
import club.finderella.finderella.Helpers.MyDBHandler;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;
import club.finderella.finderella.Utilities.IntroDPViewer;
import club.finderella.finderella.Utilities.IntroductionViewer;

public class BookmarkAdapter extends ArrayAdapter {


    private ImageLoader mImageLoader;
    private MyDBHandler db;
    private SQLiteDatabase dbObj;
    private LayoutInflater mInflater;


    public BookmarkAdapter(Context context, ArrayList<IntroItem> mList) {
        super(context, R.layout.custom_bookmark, mList);

        mImageLoader = MySingleton.getInstance(getContext()).getImageLoader();

        db = new MyDBHandler(getContext(), null, null, 1);
        dbObj = db.getWritableDatabase();
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final IntroItem mItem = (IntroItem) getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_bookmark, parent, false);

            holder = new ViewHolder();

            holder.account_id = mItem.account_id;
            holder.bookmark_id = mItem.post_id;

            holder.topView = convertView.findViewById(R.id.topView);
            holder.undoText = (TextView) convertView.findViewById(R.id.undoText);

            holder.profile_image = (CircularImg) convertView.findViewById(R.id.mProfileImg);
            holder.profile_image_url = mItem.profile_image;

            holder.mName = (TextView) convertView.findViewById(R.id.mName);
            holder.meta_data = (TextView) convertView.findViewById(R.id.mMetaData);
            holder.location = (TextView) convertView.findViewById(R.id.mLocation);

            holder.deleteBookmarkButton = (ImageView) convertView.findViewById(R.id.deleteImage);
            holder.viewButton = (RelativeLayout) convertView.findViewById(R.id.viewButton);

            holder.deleteBookmarkButton.setOnClickListener(holder.delete);
            holder.viewButton.setOnClickListener(holder.go_to_intro);
            holder.undoText.setOnClickListener(holder.undo);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.profile_image.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));

        holder.mName.setText(mItem.name);
        holder.meta_data.setText(mItem.metadata);
        holder.location.setText(mItem.location);


        if (mItem.profile_image != null) {
            holder.profile_image.setImageUrl(mItem.profile_image, mImageLoader);
            holder.profile_image.setOnClickListener(holder.dpViewListener);
        } else
            holder.profile_image.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));


        return convertView;
    }

    private class ViewHolder {

        public ViewHolder() {
        }


        public int account_id, bookmark_id;    // account_id , _id in bookmarks DB

        public View topView;
        public TextView undoText;

        public String profile_image_url;

        public CircularImg profile_image;
        public TextView meta_data, mName, location;   // meta data is age gender location

        public ImageView deleteBookmarkButton;
        public RelativeLayout viewButton;

        private final View.OnClickListener dpViewListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (topView.getVisibility() != View.VISIBLE) {
                    Intent mIntent = new Intent(getContext(), IntroDPViewer.class);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    mIntent.putExtra("image_url", profile_image_url);
                    getContext().startActivity(mIntent);
                }


            }
        };

        public final View.OnClickListener delete = new View.OnClickListener() {
            @Override
            public void onClick(View v) { // Delete pressed

                if (topView.getVisibility() != View.VISIBLE) {

                    new AsyncTask<Void, Void, Void>(){
                        @Override
                        protected Void doInBackground(Void... params) {
                            String query = "DELETE FROM bookmarks WHERE _id =" + bookmark_id;
                            dbObj.execSQL(query);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            topView.setVisibility(View.VISIBLE);
                            undoText.setVisibility(View.VISIBLE);
                        }
                    }.execute();


                }
            }
        };  // no change to mList

        public final View.OnClickListener go_to_intro = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (topView.getVisibility() != View.VISIBLE) {
                    Intent mIntent = new Intent(getContext(), IntroductionViewer.class);
                    mIntent.putExtra("account_id", account_id);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    getContext().startActivity(mIntent);
                }
            }
        };

        public final View.OnClickListener undo = new View.OnClickListener() {
            @Override
            public void onClick(View v) {   // Add bookmark again
                // toggle visibility of topView and undoText
                if (topView.getVisibility() == View.VISIBLE) {

                    new AsyncTask<Void, Void, Void>(){
                        @Override
                        protected Void doInBackground(Void... params) {
                            String query = "INSERT INTO bookmarks(_id, account_id, session) VALUES(" + bookmark_id + "," + account_id + ",1)";
                            dbObj.execSQL(query);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            topView.setVisibility(View.GONE);
                            undoText.setVisibility(View.GONE);

                        }
                    }.execute();



                }
            }
        }; // no change to mList

    }





}
