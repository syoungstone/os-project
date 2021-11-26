package Control;

import Processes.PCB;

import java.util.PriorityQueue;
import java.util.Queue;

// Shortest Job First scheduler using the number of CALCULATE operations in a burst
public class SJFScheduler extends Scheduler {

    private final Queue<PCB> queue;

    SJFScheduler() {
        // The compareTo() method in the PCB class will determine the ordering
        // This method orders PCBs by number of CALCULATE operations in current operation set
        queue = new PriorityQueue<>();
    }

    public synchronized void add(PCB p) {
        queue.add(p);
    }

    public synchronized PCB remove() {
        return queue.poll();
    }

    public synchronized int getReadyCount() {
        return queue.size();
    }

    // No time quantum, so no need to schedule new process after a set number of cycles
    public boolean scheduleNew(int counter) {
        return false;
    }

}
