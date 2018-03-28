package club.finderella.finderella.POJO;

import java.io.Serializable;


public class IntroItem implements Serializable{

    public int access, post_id, account_id, exp_type; // exp_type ->0 for photo , ->1 for video url //->2 for none

    public Boolean bookmarked, poked;

    public String profile_image, exp_img, exp_video;

    public String name, metadata, location, status;

    public String collection[];

    public IntroItem() {
        post_id = account_id = exp_type = 0;
        access = 1;
        bookmarked = poked = false;
        profile_image = exp_img = exp_video = null;
        name = metadata = location = status = "";

        collection = new String[]{null, null, null,null};
    }

}
