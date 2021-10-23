package Control;

import java.util.LinkedList;
import java.util.Queue;

public class Semaphore {
    private int value;
    private final Queue<Integer> queue;

    public Semaphore() {
        value = 1;
        queue = new LinkedList<>();
    }

    public void wait(int pid) {
        value--;
        if (value < 0) {
            queue.add(pid);
        } else {
            OperatingSystem.getInstance().wakeup(pid);
        }
    }

    public void signal() {
        value++;
        if (value <= 0) {
            int pid = queue.remove();
            OperatingSystem.getInstance().wakeup(pid);
        }
    }
}
