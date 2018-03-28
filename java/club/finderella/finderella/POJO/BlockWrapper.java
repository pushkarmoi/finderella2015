package club.finderella.finderella.POJO;


public class BlockWrapper {
    public int account_id;
    public String name;
    public int operation;   // 0 to remove 1 to add

    public BlockWrapper(int account_id, int operation) {
        this.account_id = account_id;
        this.operation = operation;
    }

    public BlockWrapper(int account_id, String name, int operation) {
        this.account_id = account_id;
        this.name = name;
        this.operation = operation;
    }
}
