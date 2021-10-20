// Round Robin scheduler employing a circular queue
public class RRScheduler extends Scheduler {

    private Node head;
    private Node tail;

    public void addProcess(PCB p) {
        Node n = new Node(p);
        if (head == null) {
            head = n;
        } else {
            tail.setNext(n);
        }
        tail = n;
        n.setNext(head);
    }

    public PCB removeProcess() {
        Node n = head;
        head = n.getNext();
        tail.setNext(head);
        return n.getProcess();
    }

    private static class Node {
        private final PCB p;
        private Node next;

        Node(PCB p) {
            this.p = p;
        }

        public PCB getProcess() {
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
