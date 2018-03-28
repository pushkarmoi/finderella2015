package club.finderella.finderella.POJO;


public class BookmarkWrapper {
    public int bookmark_id, account_id, operation;

    public BookmarkWrapper(int account_id, int op) {
        this.account_id = account_id;
        this.operation = op;

    }

    public BookmarkWrapper(int account_id, int bookmar_id, int op) {
        this.account_id = account_id;
        this.bookmark_id = bookmar_id;
        this.operation = op;
    }
}
