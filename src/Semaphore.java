import java.util.LinkedList;
import java.util.Queue;

public class Semaphore {
    private int value;
    private Queue<Process> queue;

    public Semaphore() {
        value = 1;
        queue = new LinkedList<>();
    }

    public void wait(Process p) {
        value--;
        if (value < 0) {
            queue.add(p);
            p.block();
        }
    }

    public void signal() {
        value++;
        if (value <= 0) {
            Process p = queue.remove();
            p.wakeup();
        }
    }
}
