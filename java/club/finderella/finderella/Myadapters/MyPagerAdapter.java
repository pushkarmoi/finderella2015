package club.finderella.finderella.Myadapters;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import club.finderella.finderella.BookmarksFrag;
import club.finderella.finderella.Helpers.FragmentPagerAdapter;
import club.finderella.finderella.IntroductionsFrag;
import club.finderella.finderella.MyIntroFrag;
import club.finderella.finderella.PokesFrag;


public class MyPagerAdapter extends FragmentPagerAdapter {  // imported from private class in Helpers

    private CharSequence Titles[]; // This will Store the Titles of the Tabs which are Going to be passed when ViewPagerAdapter is created
    private int NumbOfTabs; // Store the number of tabs, this will also be passed when the ViewPagerAdapter is created
    private FragmentManager myFm;
    private int mViewPagerId;

    public MyPagerAdapter(FragmentManager fm, int numbOfTabs, CharSequence[] titles, int mViewPagerId) {
        super(fm);
        this.NumbOfTabs = numbOfTabs;
        this.Titles = titles;
        this.myFm = fm;
        this.mViewPagerId = mViewPagerId;
    }

    @Override
    public Fragment getItem(int i) {

        switch (i) {
            case 0:
                return new IntroductionsFrag();
            case 1:
                return new BookmarksFrag();
            case 2:
                return new PokesFrag();
            case 3:
                return new MyIntroFrag();
            default:
                return new IntroductionsFrag();

        }

    }

    public @Nullable Fragment getFragmentForPosition(int position)
    {
        String tag = makeFragmentName(mViewPagerId, getItemId(position));
        Fragment fragment = myFm.findFragmentByTag(tag);
        return fragment;
    }





    @Override
    public CharSequence getPageTitle(int position) {
        return Titles[position];
    }

    @Override
    public int getCount() {
        return NumbOfTabs;
    }


}
