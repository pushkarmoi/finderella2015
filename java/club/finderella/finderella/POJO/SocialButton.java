package club.finderella.finderella.POJO;

public class SocialButton {
    public String url; // will be NULL for not activated
    public int type;

    public SocialButton(String url, int type) {
        this.url = url;
        this.type = type;
    }

    //type ->   facebook,twitter,instagram,phone
    //          1,2,3,4
}
