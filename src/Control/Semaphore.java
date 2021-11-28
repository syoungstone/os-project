package Control;

import Processes.PCB;

import java.util.LinkedList;
import java.util.Queue;

public class Semaphore {
    private int value;
    private final Queue<Integer> queue;

    public Semaphore() {
        value = 1;
        queue = new LinkedList<>();
    }

    public synchronized void wait(int pid) {
        value--;
        if (value < 0) {
            queue.add(pid);
        } else {
            wakeup(pid);
        }
    }

    public synchronized void signal() {
        value++;
        if (value <= 0) {
            int pid = queue.remove();
            wakeup(pid);
        }
    }

    // In case a process is terminated early by its parent
    public synchronized void removeFromQueue(int pid) {
        if (queue.remove(pid)) {
            value++;
        }
    }

    public synchronized int getWaitingCount() {
        return queue.size();
    }

    private void wakeup(int pid) {
        PCB p = OperatingSystem.getInstance().pidLookup(pid);
        if (p != null) {
            p.wakeup();
        }
    }
}
