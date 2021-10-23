package Control;

import java.util.LinkedList;
import java.util.Queue;

// Round Robin scheduler employing a circular queue
public class RRScheduler extends Scheduler {

    private final Queue<Integer> queue;

    RRScheduler() {
        queue = new LinkedList<>();
    }

    public void add(int pid) {
        queue.add(pid);
    }

    public int remove() {
        if (queue.peek() != null) {
            return queue.poll();
        } else {
            return OperatingSystem.KERNEL_ID;
        }
    }

}
