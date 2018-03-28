package club.finderella.finderella.Myadapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import club.finderella.finderella.Coach.StepOne;
import club.finderella.finderella.Coach.StepThree;
import club.finderella.finderella.Coach.StepTwo;


public class CoachAdapter extends FragmentPagerAdapter {
    private int nTabs;

    public CoachAdapter(FragmentManager fm, int tabs) {
        super(fm);
        this.nTabs = tabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new StepOne();
            case 1:
                return new StepTwo();
            case 2:
                return new StepThree();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return this.nTabs;
    }
}
