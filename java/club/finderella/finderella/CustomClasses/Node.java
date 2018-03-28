package club.finderella.finderella.CustomClasses;

// Binary Search Tree Node Definition
public class Node {
    public String data, value;
    public Node left, right;

    public Node() {
        data = null;
        value = null;
        left = null;
        right = null;

    }

    public Node(String data, String value) {
        this.data = data;
        this.value = value;
        this.left = null;
        this.right = null;
    }

    static public boolean Search(Node head, Node x) {

        if ((head == null) || (x == null))
            return false;

        Node current = head;
        int c;

        while (current != null) {
            c = (x.value).compareTo(current.value);
            if (c == 0) {
                if ((current.data).equals(x.data))
                    return true;
                else
                    current = current.right;
            } else {
                if (c < 0)
                    current = current.left;
                else
                    current = current.right;
            }

        }// end of while
        return false;
    }

    static public Node Insert(Node head, Node x) {     // returns input (x) if head is null, else head remains the same

        if ((head == null) || (x == null)) {
            return x;
        }

        Node current = head;
        int c;

        while (1 == 1) {
            c = (x.value).compareTo(current.value);
            if (c >= 0) {
                if (current.right == null) {
                    current.right = x;
                    return head;
                } else
                    current = current.right;

            } else {
                if (current.left == null) {
                    current.left = x;
                    return head;
                } else
                    current = current.left;

            }


        }// end of while


    }


}
