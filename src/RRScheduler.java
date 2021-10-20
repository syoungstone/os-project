// Round Robin scheduler employing a circular queue
public class RRScheduler extends Scheduler {

    private Node head;
    private Node tail;

    public void addProcess(Process p) {
        Node n = new Node(p);
        if (head == null) {
            head = n;
        } else {
            tail.setNext(n);
        }
        tail = n;
        n.setNext(head);
    }

    public Process removeProcess() {
        Node n = head;
        head = n.getNext();
        tail.setNext(head);
        return n.getProcess();
    }

    private static class Node {
        private final Process p;
        private Node next;

        Node(Process p) {
            this.p = p;
        }

        public Process getProcess() {
            return p;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

    }
}
