package club.finderella.finderella.POJO;


import club.finderella.finderella.BookmarksFrag;
import club.finderella.finderella.IntroductionsFrag;
import club.finderella.finderella.MyIntroFrag;
import club.finderella.finderella.Myadapters.MyPagerAdapter;
import club.finderella.finderella.PokesFrag;

public class FragmentRefrences {

    public static IntroductionsFrag introFrag;
    public static BookmarksFrag bookmarksFrag;
    public static PokesFrag pokesFrag;
    public static MyIntroFrag myIntroFrag;

    public FragmentRefrences() {
        introFrag = null;
        bookmarksFrag = null;
        pokesFrag = null;
        myIntroFrag = null;
    }

    public static void setup(MyPagerAdapter myPagerAdapter) {
        introFrag = (IntroductionsFrag) myPagerAdapter.getFragmentForPosition(0);
        bookmarksFrag = (BookmarksFrag) myPagerAdapter.getFragmentForPosition(1);
        pokesFrag = (PokesFrag) myPagerAdapter.getFragmentForPosition(2);
        myIntroFrag = (MyIntroFrag) myPagerAdapter.getFragmentForPosition(3);
    }


}
