package club.finderella.finderella.POJO;


public class BlockListItem {

    public int block_id;
    public int account_id;
    public String name;

    public BlockListItem() {
        this.block_id = 0;
        this.account_id = 0;
        this.name = "";

    }

    public BlockListItem(int block_id, int account_id, String name) {
        this.block_id = block_id;
        this.account_id = account_id;
        this.name = name;
    }
}
