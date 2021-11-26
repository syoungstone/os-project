package Processor;

import Processes.PCB;

import java.util.LinkedList;
import java.util.Queue;

// Round Robin scheduler employing a circular queue
public class RRScheduler extends ShortTermScheduler {

    private static final int TIME_QUANTUM = 10;

    private final Queue<PCB> queue;

    RRScheduler() {
        queue = new LinkedList<>();
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

    // Time to schedule a new process if counter has reached the time quantum
    public boolean scheduleNew(int counter) {
        return counter >= TIME_QUANTUM;
    }

}
