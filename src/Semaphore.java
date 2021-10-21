import java.util.LinkedList;
import java.util.Queue;

public class Semaphore {
    private int value;
    private Queue<PCB> queue;

    public Semaphore() {
        value = 1;
        queue = new LinkedList<>();
    }

    public void wait(PCB p) {
        value--;
        if (value < 0) {
            queue.add(p);
        } else {
            p.getProcess().wakeup();
        }
    }

    public void signal() {
        value++;
        if (value <= 0) {
            PCB p = queue.remove();
            p.getProcess().wakeup();
        }
    }
}
