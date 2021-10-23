// Round Robin scheduler employing a circular queue
public class RRScheduler extends Scheduler {

    private Node head;
    private Node tail;

    public void add(int pid) {
        Node n = new Node(pid);
        if (head == null) {
            head = n;
        } else {
            tail.setNext(n);
        }
        tail = n;
        n.setNext(head);
    }

    public int remove() {
        Node n = head;
        head = n.getNext();
        tail.setNext(head);
        return n.getPid();
    }

    private static class Node {
        private final int pid;
        private Node next;

        Node(int pid) {
            this.pid = pid;
        }

        public int getPid() {
            return pid;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

    }
}
