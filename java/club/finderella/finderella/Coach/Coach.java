package club.finderella.finderella.Coach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.viewpagerindicator.CirclePageIndicator;

import club.finderella.finderella.MainActivity;
import club.finderella.finderella.Myadapters.CoachAdapter;
import club.finderella.finderella.ProfileSetup.ProfileSetupOne;
import club.finderella.finderella.R;
import club.finderella.finderella.Services.FbDownloader;

public class Coach extends AppCompatActivity implements Common {
    private ViewPager mPager;
    private CirclePageIndicator mIndicator;
    private CoachAdapter mAdapter;
    private static String fbProfilePicUrl = null;

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_coach);


        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().getString("fbProfilePicUrl") != null) {
            fbProfilePicUrl = getIntent().getExtras().getString("fbProfilePicUrl");
            Intent mIntent = new Intent(this, FbDownloader.class);
            mIntent.putExtra("fbProfilePicUrl", fbProfilePicUrl);
            startService(mIntent);
        } else
            fbProfilePicUrl = null;

        mPref = getSharedPreferences("finderella_preferences", MODE_PRIVATE);   // filename
        mEditor = mPref.edit();


        mPager = (ViewPager) findViewById(R.id.mPager);
        mIndicator = (CirclePageIndicator) findViewById(R.id.mIndicator);

        mAdapter = new CoachAdapter(getSupportFragmentManager(), 3);
        mPager.setAdapter(mAdapter);
        mPager.setPageTransformer(true, new FadePageTransformer());


        mIndicator.setViewPager(mPager, 0);

        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 2)
                    mIndicator.setVisibility(View.GONE);
                else
                    mIndicator.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

    }

    @Override
    public void next() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
    }

    @Override
    public void gotit() {
        if (mPref.getInt("code", 2) == 2) {
            startActivity(new Intent(this, ProfileSetupOne.class));
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }


    }


}
