import java.util.Scanner;

public class StringScheduler {

    private Node head;
    private Node tail;

    public void addProcess(String p) {
        Node n = new Node(p);
        if (head == null) {
            head = n;
        } else {
            tail.setNext(n);
        }
        tail = n;
        n.setNext(head);
    }

    public String removeProcess() {
        Node n = head;
        head = n.getNext();
        tail.setNext(head);
        return n.getProcess();
    }

    public void print() {
        if (head == null) {
            System.out.println("empty");
        } else {
            Node n = head;
            do {
                System.out.println(n.getProcess());
                n = n.getNext();
            } while (n != head);
        }
    }

    private static class Node {
        private final String p;
        private Node next;

        Node(String p) {
            this.p = p;
        }

        public String getProcess() {
            return p;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

    }

    public static void main(String[] args) {
        boolean exit = false;
        StringScheduler scheduler = new StringScheduler();
        Scanner scan = new Scanner(System.in);
        while(!exit) {
            System.out.println("Current scheduler state:");
            scheduler.print();
            System.out.println("Enter 1 to add process, 2 to remove, 3 to exit");
            String num = scan.next();
            switch (num) {
                case "1": {
                    System.out.println("Enter name of new process");
                    String name = scan.next();
                    scheduler.addProcess(name);
                    break;
                }
                case "2": {
                    String name = scheduler.removeProcess();
                    System.out.println("Removed process: " + name);
                    break;
                }
                case "3":
                    exit = true;
                    break;
                default:
                    System.out.println("I'm sorry, I didn't understand that");
                    break;
            }
        }
        scan.close();
    }
}
