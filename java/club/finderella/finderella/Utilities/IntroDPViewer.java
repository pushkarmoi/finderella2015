package club.finderella.finderella.Utilities;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import club.finderella.finderella.Helpers.MySingleton;
import club.finderella.finderella.R;

public class IntroDPViewer extends Activity {

    private NetworkImageView mImage;
    private ImageView mClose;
    private String mUrl;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_intro_dpviewer);

        mImageLoader = MySingleton.getInstance(this).getImageLoader();
        mImage = (NetworkImageView) findViewById(R.id.mImage);
        mClose = (ImageView) findViewById(R.id.mClose);

        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        mUrl = bundle.getString("image_url");

        mImage.setImageUrl(mUrl, mImageLoader);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
