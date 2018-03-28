package club.finderella.finderella.Myadapters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;

import club.finderella.finderella.CustomClasses.CircularImg;
import club.finderella.finderella.POJO.PokeItem;
import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;
import club.finderella.finderella.Utilities.IntroductionViewer;

public class PokesAdapter extends ArrayAdapter<PokeItem> {

    LayoutInflater mInflater;
    private ImageLoader mImageLoader;

    public PokesAdapter(Context context, ArrayList<PokeItem> mList) {
        super(context, R.layout.custom_pokes, mList);
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageLoader = MySingleton.getInstance(getContext()).getImageLoader();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final PokeItem mItem = (PokeItem) getItem(position);
        final ViewHolder mHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_pokes, parent, false);

            mHolder = new ViewHolder();
            mHolder.account_id = mItem.account_id;
            mHolder.access = mItem.access;


            mHolder.mName = (TextView) convertView.findViewById(R.id.mName);
            mHolder.content = (TextView) convertView.findViewById(R.id.content);

            mHolder.mProfileImg = (CircularImg) convertView.findViewById(R.id.mProfileImg);

            convertView.setOnClickListener(mHolder.mClickListener);

            convertView.setTag(mHolder);

        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }

        mHolder.mProfileImg.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));

        (mHolder.mName).setText(mItem.mName);


        mHolder.content.setText("poked you");


        if (mItem.mProfileImg != null)
            mHolder.mProfileImg.setImageUrl(mItem.mProfileImg, mImageLoader);
        else
            mHolder.mProfileImg.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_human));


        return convertView;
    }

    private class ViewHolder {
        public ViewHolder() {
        }

        public int account_id, access;
        public TextView mName, content;
        public CircularImg mProfileImg;

        public final View.OnClickListener mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (access == 1) {
                    Intent mIntent = new Intent(getContext(), IntroductionViewer.class);
                    mIntent.putExtra("account_id", account_id);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    getContext().startActivity(mIntent);
                }
            }
        };
    }


}
