package club.finderella.finderella.POJO;


public class BookmarkItem {

    public int account_id, bookmark_id, exp_type, access;
    public String mName,  mMetaData, mLocation, mStatus;

    public String mProfileImg,exp_img, exp_video;
    public String collection[];


    public BookmarkItem() {
        account_id = bookmark_id = exp_type = 0;
        access = 1;
        mName = mMetaData = mLocation = mStatus = "";
        mProfileImg = exp_img = exp_video = null;
        collection = new String[]{null, null, null, null, null};
    }


}
