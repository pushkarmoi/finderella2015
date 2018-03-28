package club.finderella.finderella.POJO;


public class PokeItem {

    public int account_id, read, access, poke_id;
    public String mName;         // first_name + last_name of pokeR
    public String mProfileImg;

    public PokeItem() {
        account_id = read = poke_id = 0;
        access = 1;
        mName = "";
        mProfileImg = null;
    }
}
